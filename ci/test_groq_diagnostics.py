import io
import json
import os
import unittest
import urllib.error
from unittest.mock import patch

from groq_diagnostics import DEFAULT_MODEL, check_provider, safe_http_error


class GroqDiagnosticsTest(unittest.TestCase):
    def error(self, code, detail):
        return urllib.error.HTTPError("https://test.invalid", code, "test", {},
                                      io.BytesIO(json.dumps({"detail": detail}).encode()))

    def test_unknown_backend_response_does_not_disclose_private_data(self):
        result = safe_http_error(self.error(503, "private-token-and-response"), backend=True)
        self.assertEqual(result, "HTTP 503: servicio no disponible")
        self.assertNotIn("private-token", result)

    def test_backend_configuration_error_is_distinguished_from_authentication(self):
        result = safe_http_error(self.error(503,
            "La configuración del modelo de IA no es válida. Revisa GROQ_MODEL y los parámetros de la solicitud."), backend=True)
        self.assertEqual(result, "HTTP 503: modelo o parametros de Groq")

    @patch.dict(os.environ, {"GROQ_API_KEY": ""})
    @patch("urllib.request.urlopen")
    def test_missing_key_stops_before_network(self, urlopen):
        with self.assertRaisesRegex(RuntimeError, "Falta el secret"):
            check_provider()
        urlopen.assert_not_called()

    @patch.dict(os.environ, {"GROQ_API_KEY": "test-private-key", "GROQ_MODEL": DEFAULT_MODEL})
    @patch("urllib.request.urlopen")
    def test_available_model_authenticates_at_fixed_groq_endpoint(self, urlopen):
        urlopen.return_value.__enter__.return_value = io.BytesIO(json.dumps({"data": [{"id": DEFAULT_MODEL}]}).encode())
        self.assertEqual(check_provider(), {"model": DEFAULT_MODEL, "available": True})
        request = urlopen.call_args.args[0]
        self.assertEqual(request.full_url, "https://api.groq.com/openai/v1/models")
        self.assertEqual(request.get_header("Authorization"), "Bearer test-private-key")
        self.assertEqual(request.get_header("User-agent"), "DriveAI-Groq/1.0")

    @patch.dict(os.environ, {"GROQ_API_KEY": "test-private-key", "GROQ_MODEL": DEFAULT_MODEL})
    @patch("urllib.request.urlopen")
    def test_unavailable_model_fails_without_silent_fallback(self, urlopen):
        urlopen.return_value.__enter__.return_value = io.BytesIO(b'{"data": []}')
        with self.assertRaisesRegex(RuntimeError, "no esta disponible"):
            check_provider()

    @patch.dict(os.environ, {"GROQ_API_KEY": "test-private-key", "GROQ_MODEL": DEFAULT_MODEL})
    @patch("urllib.request.urlopen")
    def test_provider_auth_failure_does_not_echo_body_or_credentials(self, urlopen):
        urlopen.side_effect = self.error(401, "test-private-key")
        with self.assertRaises(RuntimeError) as context:
            check_provider()
        self.assertEqual(str(context.exception), "Groq: HTTP 401: autenticacion")


if __name__ == "__main__":
    unittest.main()
