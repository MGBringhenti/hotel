FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ENV VITE_CLIENTES_URL=/api/clientes
ENV VITE_QUARTOS_URL=/api/quartos
ENV VITE_RESERVAS_URL=/api/reservas
RUN npm run build

FROM nginx:1.28-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
