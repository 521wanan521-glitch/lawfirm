#!/usr/bin/env bash
# 律所数字化办公系统 - 阿里云 ECS 一键部署脚本
# 用法：在 ECS 上以 root 执行
#   git clone https://github.com/521wanan521-glitch/lawfirm.git /opt/lawfirm && cd /opt/lawfirm/deploy && bash deploy.sh
set -euo pipefail

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info(){ echo -e "${GREEN}[*]${NC} $1"; }
warn(){ echo -e "${YELLOW}[!]${NC} $1"; }
fail(){ echo -e "${RED}[x]${NC} $1"; exit 1; }

REPO="https://github.com/521wanan521-glitch/lawfirm.git"
APP_DIR="/opt/lawfirm"

# ---------- 1. 安装 Docker ----------
if command -v docker >/dev/null 2>&1; then
  info "Docker 已安装：$(docker --version)"
else
  info "安装 Docker..."
  curl -fsSL https://get.docker.com | sh || fail "Docker 安装失败，请手动安装后重试"
  systemctl enable --now docker 2>/dev/null || service docker start
fi

if ! docker compose version >/dev/null 2>&1; then
  warn "缺少 docker compose 插件，尝试安装..."
  apt-get update -y && apt-get install -y docker-compose-plugin 2>/dev/null || true
fi

# ---------- 2. 拉取代码 ----------
if [ -d "$APP_DIR/.git" ]; then
  info "代码目录已存在，执行 git pull..."
  git -C "$APP_DIR" pull origin main 2>/dev/null || warn "拉取更新失败（可能是网络问题），继续使用已有代码"
else
  info "拉取代码..."
  git clone "$REPO" "$APP_DIR" || fail "代码拉取失败。可尝试手动上传代码到 $APP_DIR 后重跑本脚本"
fi
cd "$APP_DIR/deploy"

# ---------- 3. 生成配置 ----------
if [ ! -f .env ]; then
  info "生成 .env 配置（随机数据库密码与 JWT 密钥）..."
  DB_PWD=$(openssl rand -base64 18 | tr -dc 'a-zA-Z0-9' | head -c 16)
  JWT_KEY=$(openssl rand -base64 48 | tr -dc 'a-zA-Z0-9' | head -c 40)
  DEEPSEEK_KEY="${DEEPSEEK_API_KEY:-}"
  if [ -z "$DEEPSEEK_KEY" ]; then
    read -r -p "请输入 DeepSeek API Key（AI 助手功能需要，可留空跳过）: " DEEPSEEK_KEY || true
  fi
  cat > .env <<EOF
DB_NAME=lawfirm
DB_USER=lawfirm
DB_PASSWORD=${DB_PWD}
APP_JWT_SECRET=${JWT_KEY}
FRONTEND_PORT=${FRONTEND_PORT:-80}
DEEPSEEK_API_KEY=${DEEPSEEK_KEY}
DEEPSEEK_BASE_URL=${DEEPSEEK_BASE_URL:-https://api.deepseek.com}
DEEPSEEK_MODEL=${DEEPSEEK_MODEL:-deepseek-chat}
EOF
else
  info ".env 已存在，跳过生成"
  grep -q "DEEPSEEK_API_KEY" .env 2>/dev/null || {
    warn "检测到旧 .env 缺少 AI 助手配置，正在补充..."
    cat >> .env <<EOF
DEEPSEEK_API_KEY=
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-chat
EOF
  }
fi

# ---------- 4. 构建并启动 ----------
info "构建并启动服务（首次约 5-15 分钟，取决于带宽）..."
docker compose up -d --build

# ---------- 5. 完成提示 ----------
sleep 3
IP=$(curl -fsSL --max-time 5 ifconfig.me 2>/dev/null || echo "你的服务器公网IP")
PORT="${FRONTEND_PORT:-80}"
echo ""
info "部署完成！"
echo "  访问地址：  http://${IP}:${PORT}"
echo "  默认账号：  admin / admin123（登录后请立即修改所有默认账号密码！）"
echo "  查看状态：  cd ${APP_DIR}/deploy && docker compose ps"
echo "  查看日志：  cd ${APP_DIR}/deploy && docker compose logs -f backend"
echo ""
warn "上线检查清单："
warn "  1. 安全组已放行 ${PORT} 端口"
warn "  2. .env 中的 DEEPSEEK_API_KEY 已填写（AI 助手功能）"
warn "  3. 登录后立即修改 admin/partner/lawyer1 等默认账号密码"
warn "  4. 本阶段为 HTTP；PWA 安装需要 HTTPS（域名+证书），详见 docs/DEPLOY_ALIYUN.md"
warn "  5. 桌面客户端（Electron）请把 config.json 的 url 改为 http://${IP}:${PORT}"
