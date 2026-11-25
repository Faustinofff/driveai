"""Diagnosticos acotados: nunca incluir cuerpos del proveedor, tokens ni headers."""
import json
import os
import urllib.error
import urllib.request

DEFAULT_MODEL = "openai/gpt-oss-120b"


def safe_http_error(error, backend=False):
    category = {401: "autenticacion", 403: "autenticacion o permisos",
                429: "cuota o limite de solicitudes", 400: "modelo o parametros",
                404: "modelo o ruta no disponible", 422: "parametros invalidos"}.get(error.code, "servicio no disponible")
    if backend:
        # Solo convertir mensajes conocidos en categorias fijas; no copiar el cuerpo.
        try:
            detail = json.loads(error.read(16384)).get("detail", "")
        except (ValueError, AttributeError):
            detail = ""
        known = {
            "No se pudo autenticar con el servicio de IA. Avisale al administrador para que revise la clave configurada.": "autenticacion con Groq",
            "La configuración del modelo de IA no es válida. Avisale al administrador para que revise el modelo configurado.": "modelo o parametros de Groq",
            "El servicio de IA está saturado en este momento. Inténtalo de nuevo en unos segundos.": "cuota o limite de Groq",
            "El servicio de IA no respondió a tiempo. Inténtalo de nuevo en un momento.": "timeout de Groq",
            "El asistente devolvió una respuesta inválida. Inténtalo de nuevo.": "JSON invalido del modelo",
            "El modelo de IA no pudo generar el formato requerido. Inténtalo de nuevo.": "generacion estructurada rechazada por Groq",
        }
        if isinstance(detail, str):
            category = known.get(detail, category)
    return f"HTTP {error.code}: {category}"


def check_provider():
    key = os.getenv("GROQ_API_KEY", "").strip()
    model = os.getenv("GROQ_MODEL", DEFAULT_MODEL).strip()
    if not key:
        raise RuntimeError("Falta el secret GROQ_API_KEY")
    if not model or model == key or model.startswith("gsk_"):
        raise RuntimeError("GROQ_MODEL no contiene un identificador de modelo valido")
    req = urllib.request.Request("https://api.groq.com/openai/v1/models",
                                 headers={"Authorization": "Bearer " + key,
                                          "User-Agent": "DriveAI-Groq/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            data = json.load(response)
    except urllib.error.HTTPError as error:
        raise RuntimeError("Groq: " + safe_http_error(error)) from None
    except (urllib.error.URLError, TimeoutError):
        raise RuntimeError("No se pudo conectar con Groq para verificar el modelo") from None
    models = {item.get("id") for item in data.get("data", []) if isinstance(item, dict)}
    if model not in models:
        raise RuntimeError("GROQ_MODEL no esta disponible para esta cuenta; revisar el modelo en Groq")
    return {"model": model, "available": True}
