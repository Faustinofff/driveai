# ============================================================================
# DriveAI Frontend — Build multi-etapa
#   Etapa 1: compila la SPA con Node 20 + Vite (inyecta VITE_API_URL)
#   Etapa 2: sirve los estáticos con Nginx (proxy /api -> backend)
# ============================================================================

FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_API_URL=""
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
