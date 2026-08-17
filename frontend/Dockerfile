# 前端镜像：多阶段构建（构建静态资源 + Nginx 托管）
FROM node:20-alpine AS build
WORKDIR /app

# 限制构建 Node 内存，适配小内存（≤2G）服务器
ENV NODE_OPTIONS="--max-old-space-size=768"

COPY package.json package-lock.json* ./
# 使用淘宝 npm 镜像加速（国内服务器构建）
RUN npm install --registry=https://registry.npmmirror.com --no-audit --no-fund

COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
