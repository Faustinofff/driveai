#!/bin/sh
# DEF-19: genera un certificado autofirmado para HTTPS si no se montó uno propio.
# Se crea al iniciar el contenedor, nunca se versiona (G6) y la clave privada
# no se imprime en el log.
set -eu

CERT_DIR=/etc/nginx/certs
CRT="$CERT_DIR/tls.crt"
KEY="$CERT_DIR/tls.key"

if [ -s "$CRT" ] && [ -s "$KEY" ]; then
    echo "driveai-tls: usando el certificado existente en $CERT_DIR"
    exit 0
fi

mkdir -p "$CERT_DIR"
umask 077
openssl req -x509 -newkey rsa:2048 -sha256 -nodes -days 30 \
    -keyout "$KEY" -out "$CRT" \
    -subj "/CN=localhost/O=DriveAI demo" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1" \
    >/dev/null 2>&1
chmod 600 "$KEY"
chmod 644 "$CRT"
echo "driveai-tls: certificado autofirmado generado (30 dias, CN=localhost)"
