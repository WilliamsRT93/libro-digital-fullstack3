# Build multi-etapa: compilacion del frontend con Node 22 LTS y servido con Nginx 1.27
FROM node:22-alpine AS build
WORKDIR /app

# Copiamos solo los manifests primero para aprovechar cache de Docker
COPY package.json .npmrc ./

# Limpieza de cache previo + instalacion con peer-deps permisivos.
# Si existiera package-lock.json local con vite 8 (inestable), lo ignoramos
# por el .dockerignore para evitar arrastrar resoluciones rotas.
RUN npm cache clean --force \
 && npm install --legacy-peer-deps --no-audit --no-fund

# Ahora copiamos el codigo fuente y construimos
COPY . .
RUN npm run build

# Imagen final liviana basada en Nginx
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1
