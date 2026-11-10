#!/usr/bin/env bash
# Levanta el mock SOAP (WireMock) y el backend empaquetado para los jobs de API,
# contrato y DAST del pipeline. Requiere PostgreSQL y RabbitMQ como services del job
# y las variables JWT_SECRET, ADMIN_EMAIL y ADMIN_PASSWORD en el entorno.
set -euo pipefail

docker run -d --name soap-mock -p 8089:8080 \
  -v "$PWD/docker/soap-mock/mappings:/home/wiremock/mappings:ro" \
  -v "$PWD/docker/soap-mock/__files:/home/wiremock/__files:ro" \
  wiremock/wiremock:3.9.1 --port 8080

(cd backend && mvn -B -q -DskipTests package)

nohup java -jar backend/target/driveai-backend.jar > backend-ci.log 2>&1 &

for _ in $(seq 1 60); do
  if curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
    echo "Backend listo"
    exit 0
  fi
  sleep 2
done
echo "El backend no arrancó a tiempo"
tail -100 backend-ci.log
exit 1
