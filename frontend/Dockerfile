ARG NODE_IMAGE=m.daocloud.io/docker.io/library/node:22-alpine
ARG NGINX_IMAGE=m.daocloud.io/docker.io/library/nginx:1.28-alpine
FROM ${NODE_IMAGE} AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM ${NGINX_IMAGE}
COPY nginx-main.conf /etc/nginx/nginx.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html
EXPOSE 80
