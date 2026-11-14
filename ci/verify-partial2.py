"""Smoke de entrega 2. Crea datos de demostracion; ejecutar en una base de demo."""
import argparse
import datetime
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://localhost:5173")
    parser.add_argument("--frontend", action="store_true",
                        help="Comprueba que el origen sirve el HTML de la SPA")
    parser.add_argument("--messaging", action="store_true",
                        help="Verifica outbox y consumo en PostgreSQL via Docker Compose")
    parser.add_argument("--db-user", default="cardb_user")
    parser.add_argument("--db-name", default="cardb")
    parser.add_argument("--report", default="work/partial2-smoke.json")
    args = parser.parse_args()
    email, password = os.getenv("ADMIN_EMAIL"), os.getenv("ADMIN_PASSWORD")
    if not email or not password:
        parser.error("Exportar ADMIN_EMAIL y ADMIN_PASSWORD del entorno de demo")
    report = {"started_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
              "base_url": args.base_url, "checks": [], "status": "running"}

    def request(label, method, path, expected, body=None, token=None, key=None):
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = "Bearer " + token
        if key:
            headers["Idempotency-Key"] = key
        data = None if body is None else json.dumps(body).encode()
        req = urllib.request.Request(args.base_url.rstrip("/") + path, data,
                                     headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=20) as response:
                status, raw = response.status, response.read()
        except urllib.error.HTTPError as error:
            status, raw = error.code, error.read()
        report["checks"].append({"check": label, "expected": expected, "actual": status})
        require(status == expected, f"{label}: esperado {expected}, recibido {status}")
        return json.loads(raw) if raw else None

    try:
        # Usa el origen del frontend para probar tambien Nginx -> API.
        if args.frontend:
            with urllib.request.urlopen(args.base_url.rstrip("/") + "/", timeout=20) as response:
                page = response.read().lower()
                require(response.status == 200 and b"<html" in page and b'id="root"' in page,
                        "El origen no sirve el HTML de la SPA")
                report["checks"].append({"check": "HTML frontend desplegado",
                                          "expected": 200, "actual": response.status})
        request("catalogo publico", "GET", "/api/cars", 200)
        request("carrito anonimo", "GET", "/api/cart", 401)
        admin = request("login admin", "POST", "/api/auth/login", 200,
                        {"email": email, "password": password})["token"]
        run_id = uuid.uuid4().hex
        customer_email = "partial2-" + run_id + "@driveai.test"
        customer_password = uuid.uuid4().hex + "Aa1!"
        customer = request("registro cliente", "POST", "/api/auth/register", 201,
                           {"email": customer_email, "password": customer_password,
                            "fullName": "Entrega 2 Demo"})
        require(customer["user"]["role"] == "CUSTOMER", "Registro elevo privilegios")
        token = request("login cliente", "POST", "/api/auth/login", 200,
                        {"email": customer_email, "password": customer_password})["token"]
        payload = {"brand": "Entrega2", "model": run_id[:12], "year": 2024,
                   "price": 10000, "mileage": 100, "fuelType": "GASOLINA",
                   "fuelConsumption": 5, "transmission": "AUTOMATICA",
                   "bodyType": "SEDAN", "imageUrl": "https://img.test/demo.jpg",
                   "description": "Datos aislados de demostracion", "stock": 3}
        request("RBAC cliente sin permiso", "POST", "/api/cars", 403, payload, token)
        car = request("alta admin", "POST", "/api/cars", 201, payload, admin)
        car_id = int(car["id"])
        cart = request("agregar carrito", "POST", "/api/cart/add", 200,
                       {"carId": car_id, "quantity": 1}, token)
        require(len(cart["items"]) == 1, "Carrito no contiene el auto")
        checkout = {"items": [{"carId": car_id, "quantity": 1}]}
        order = request("checkout", "POST", "/api/orders/checkout", 200,
                        checkout, token, run_id)
        require(order["status"] == "CONFIRMED", "Orden no confirmada")
        order_id = int(order["id"])
        report["order_id"] = order_id
        report["car_id"] = car_id
        replay = request("reintento idempotente", "POST", "/api/orders/checkout", 200,
                         checkout, token, run_id)
        require(replay["id"] == order_id, "Reintento creo otra orden")
        after = request("stock tras compra", "GET", f"/api/cars/{car_id}", 200)
        require(after["stock"] == 2, "Stock incorrecto tras compra")
        empty = request("carrito vaciado", "GET", "/api/cart", 200, token=token)
        require(empty["items"] == [], "Checkout no vacio carrito")
        other = request("registro segundo usuario", "POST", "/api/auth/register", 201,
                        {"email": "other-" + run_id + "@driveai.test",
                         "password": customer_password, "fullName": "Other Demo"})["token"]
        request("orden ajena", "GET", f"/api/orders/{order_id}", 404, token=other)
        cancelled = request("cancelacion valida", "POST", f"/api/orders/{order_id}/cancel",
                            200, token=token)
        require(cancelled["status"] == "CANCELLED", "Orden no cancelada")
        request("State cancelacion repetida", "POST", f"/api/orders/{order_id}/cancel",
                409, token=token)
        request("State no puede reabrirse", "PATCH", f"/api/orders/{order_id}/status",
                409, {"status": "CONFIRMED"}, admin)
        restored = request("stock restaurado una vez", "GET", f"/api/cars/{car_id}", 200)
        require(restored["stock"] == 3, "Se devolvio stock mas de una vez")
        if args.messaging:
            # IDs convertidos a int; no se interpola entrada arbitraria en SQL.
            query = (
                "select count(*) from outbox_events o join processed_messages p "
                "on p.message_id = (o.payload::jsonb ->> 'eventId') "
                f"where o.aggregate_id = {order_id} and o.status = 'SENT' "
                "and p.consumer = 'order-events-consumer'"
            )
            deadline = time.monotonic() + 40
            count = 0
            while time.monotonic() < deadline:
                result = subprocess.run(
                    ["docker", "compose", "exec", "-T", "postgres", "psql",
                     "-U", args.db_user, "-d", args.db_name, "-tAc", query],
                    cwd=Path(__file__).resolve().parent.parent,
                    check=True, capture_output=True, text=True, timeout=10)
                count = int(result.stdout.strip())
                if count == 2:
                    break
                time.sleep(1)
            require(count == 2, "No se confirmaron y consumieron ambos eventos de esta orden")
            report["checks"].append({"check": "RabbitMQ: creacion y cancelacion consumidas",
                                      "expected": 2, "actual": count})
        else:
            report["messaging"] = "NO VERIFICADA: ejecutar con --messaging"
        report["status"] = "passed"
    except Exception as error:
        report["status"] = "failed"
        report["error"] = str(error)
    finally:
        target = Path(args.report)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
        print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0 if report["status"] == "passed" else 1


if __name__ == "__main__":
    sys.exit(main())
