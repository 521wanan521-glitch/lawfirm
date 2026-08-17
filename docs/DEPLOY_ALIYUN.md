# 阿里云 ECS 部署指南

本指南将系统部署到你的阿里云 ECS 服务器，使用 Docker Compose 一键启动
（PostgreSQL + 后端 + 前端 Nginx），并配置 HTTPS。

## 一、服务器建议配置

| 人数 | 推荐配置 | 带宽 | 说明 |
| --- | --- | --- | --- |
| 20-50 人 | 2 vCPU / 4GB 内存 | 3-5 Mbps | 经济型起步 |
| 50-100 人 | 4 vCPU / 8GB 内存 | 5-10 Mbps | 推荐 |

系统盘 40GB 起（建议 60GB，需存放上传的文档材料）。

## 二、购买 ECS 与域名

1. 在阿里云控制台购买 ECS，操作系统选择 **Ubuntu 22.04 LTS**（或 CentOS Stream 9 / Alibaba Cloud Linux 3）。
2. 安全组放行端口：**80（HTTP）、443（HTTPS）**。数据库与后端不对外暴露，无需放行 5432/8080。
3. 域名已备案后，在云解析 DNS 中添加 A 记录指向 ECS 公网 IP。
4. （可选）在数字证书管理服务申请免费 SSL 证书，或使用阿里云免费证书。

## 三、服务器初始化

```bash
# 1. 更新系统
sudo apt update && sudo apt upgrade -y

# 2. 安装 Docker 与 Compose 插件
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
# 重新登录使权限生效（或执行 newgrp docker）
docker --version
docker compose version
```

## 四、上传代码并配置环境

```bash
# 1. 在服务器创建项目目录
sudo mkdir -p /opt/lawfirm && sudo chown $USER:$USER /opt/lawfirm
cd /opt/lawfirm

# 2. 上传代码（任选其一）
# 方式 A：Git 仓库
git clone <你的仓库地址> .
# 方式 B：本地 scp 上传（在本地电脑执行）
scp -r lawfirm/* user@你的服务器IP:/opt/lawfirm/

# 3. 配置环境变量
cd deploy
cp .env.example .env
vim .env
```

编辑 `.env`：

```bash
DB_NAME=lawfirm
DB_USER=lawfirm
DB_PASSWORD=替换为强密码，例如 Kf9#mX2@LpQ7
APP_JWT_SECRET=替换为至少32位随机字符串，可用命令生成：openssl rand -base64 32
```

## 五、构建并启动

```bash
cd /opt/lawfirm/deploy
docker compose up -d --build
```

首次构建需下载 Maven 依赖与 npm 包，约 5-15 分钟。查看启动状态：

```bash
docker compose ps            # 三个容器均为 running
docker compose logs -f backend
```

验证：浏览器访问 `http://服务器IP`，看到登录页即部署成功。
使用 `admin / admin123` 登录（登录后立即在「成员管理」中修改密码）。

## 六、配置 HTTPS（域名 + 证书）

推荐使用 Caddy 或 Nginx 代理，下面以 Nginx 为例（假设已申请证书）。

**方案 A：让 Nginx 直接代理到前端容器**

在宿主机安装 nginx 并配置 `/etc/nginx/sites-available/lawfirm`：

```nginx
server {
    listen 80;
    server_name 你的域名.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name 你的域名.com;

    ssl_certificate     /etc/nginx/ssl/你的域名.pem;
    ssl_certificate_key /etc/nginx/ssl/你的域名.key;

    client_max_body_size 200m;

    location /api/ {
        proxy_pass http://127.0.0.1:80;   # 前端容器 Nginx（80 端口已映射）
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        proxy_pass http://127.0.0.1:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/lawfirm /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

**方案 B（推荐）：Caddy 自动申请证书**

```bash
sudo apt install caddy
sudo tee /etc/caddy/Caddyfile <<'EOF'
你的域名.com {
    reverse_proxy 127.0.0.1:80
    encode zstd gzip
}
EOF
sudo systemctl restart caddy
```

Caddy 会自动申请并续期 Let's Encrypt 证书，无需手动管理。

## 七、日常运维

```bash
# 查看状态
docker compose ps

# 查看日志
docker compose logs -f backend
docker compose logs -f frontend

# 升级（拉取新代码后重建）
docker compose up -d --build

# 备份数据库
docker compose exec db pg_dump -U lawfirm lawfirm > backup_$(date +%F).sql

# 恢复数据库
cat backup_2024-01-01.sql | docker compose exec -T db psql -U lawfirm lawfirm

# 备份上传文件
tar czf uploads_backup.tar.gz <upload-dir>  # 默认挂载在名为 uploads 的 Docker volume 中
docker compose run --rm -v uploads:/data alpine tar czf - /data > uploads_backup.tar.gz
```

**备份建议**：数据库 + 上传目录 每天定时备份（crontab），并同步到 OSS 或异地。

## 八、安全加固清单

- [ ] 修改所有默认账号密码（admin/partner/lawyer1 等）
- [ ] `.env` 中的 `DB_PASSWORD`、`APP_JWT_SECRET` 使用强随机值
- [ ] 安全组仅放行 80/443（和 22 用于 SSH 管理）
- [ ] SSH 改为密钥登录并禁用密码登录（`/etc/ssh/sshd_config` 中 `PasswordAuthentication no`）
- [ ] 定期执行 `apt update && apt upgrade` 更新系统补丁
- [ ] 数据库与后端容器不映射宿主机端口（compose 文件中已默认不映射）
- [ ] 如需 HTTPS，使用上方方案 A/B 并保持证书自动续期

## 九、常见问题

**Q：启动后访问 80 端口无响应？**
A：检查安全组是否放行 80 端口；检查 `docker compose ps` 中 frontend 容器是否正常。

**Q：上传大文件失败？**
A：Nginx 已配置 `client_max_body_size 200m`。若经宿主 Nginx/Caddy 代理，也需同样配置。

**Q：忘记管理员密码？**
A：连接数据库重置：
```bash
docker compose exec db psql -U lawfirm lawfirm
UPDATE sys_user SET password = '$2a$10$e0MYzXyjpJS7Pd0RVvHwHe1HlCkM0S1qOqBmYdQ9eW2xKjKfLZ8nO' WHERE username = 'admin';
```
（hash 为 BCrypt 加密的 `admin123`，登录后立即修改）

**Q：如何将文件存储改为挂载目录而不是 Docker volume？**
A：在 `docker-compose.yml` 中把 `uploads:/data/uploads` 改为 `./uploads:/data/uploads`。
