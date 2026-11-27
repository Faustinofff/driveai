# ============================================================================
# DriveAI Frontend — Build multi-etapa
#   Etapa 1: compila la SPA con Node 20 + Vite (inyecta VITE_API_URL)
#   Etapa 2: sirve los estáticos con Nginx por HTTP y HTTPS (proxy /api -> backend)
# ============================================================================

FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
# --ignore-scripts: sin scripts de instalación de terceros (Sonar S6505); el build no los necesita.
RUN npm ci --ignore-scripts
COPY . .
ARG VITE_API_URL=""
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# Sonar docker:S6471: imagen oficial de nginx que corre sin root (usuario nginx, UID 101).
# El PID y los temporales ya están en /tmp; por eso escucha en 8080/8443.
FROM nginxinc/nginx-unprivileged:1.27-alpine
# root solo para instalar paquetes y preparar permisos; el contenedor corre como nginx.
USER root
# openssl: genera el certificado autofirmado al iniciar (DEF-19, sin certificados en el repo).
RUN apk add --no-cache openssl \
    && mkdir -p /etc/nginx/certs \
    && chown nginx:nginx /etc/nginx/certs \
    && chmod 700 /etc/nginx/certs
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY driveai-locations.conf /etc/nginx/snippets/driveai-locations.conf
COPY --chmod=755 docker-entrypoint.d/10-driveai-tls.sh /docker-entrypoint.d/10-driveai-tls.sh
COPY --from=build /app/dist /usr/share/nginx/html
USER nginx
EXPOSE 8080 8443
CMD ["nginx", "-g", "daemon off;"]
