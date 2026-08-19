#!/usr/bin/env bash
# 律所数字化办公系统 - 新租户（律所）快速开通脚本
#
# 用法: bash create-tenant.sh <租户名> <对外端口>
# 示例: bash create-tenant.sh lawfirm-b 8081
#   - 对外访问端口(nginx): 8081
#   - 后端端口:           9081  (= 对外 + 1000)
#   - H2 AUTO_SERVER 端口:10081  (= 对外 + 2000)
#
# 每个租户 = 独立后端进程 + 独立 H2 数据目录 + 独立 nginx 端口，
# 复用同一份 jar 和前端 dist（代码共享、数据隔离）。
set -euo pipefail

APP_DIR=/opt/lawfirm-lite
TENANT_BASE=/opt/lawfirm-tenants
SHARED_JAR=$APP_DIR/lawfirm-backend-1.0.0.jar
SHARED_DIST=$APP_DIR/dist
H2_JAR=$APP_DIR/h2/h2.jar

NAME="${1:-}"
EXT_PORT="${2:-}"

if [ -z "$NAME" ] || [ -z "$EXT_PORT" ]; then
  echo "用法: bash $0 <租户名> <对外端口>"
  echo "示例: bash $0 lawfirm-b 8081"
  exit 1
fi

if ! echo "$NAME" | grep -qE '^[a-z0-9-]+$'; then
  echo "错误: 租户名只能是小写字母/数字/连字符"
  exit 1
fi
if ! echo "$EXT_PORT" | grep -qE '^[0-9]+$'; then
  echo "错误: 端口必须是数字"
  exit 1
fi

BACKEND_PORT=$((EXT_PORT + 1000))
H2_PORT=$((EXT_PORT + 2000))
TENANT_DIR=$TENANT_BASE/$NAME
SERVICE="lawfirm-$NAME"

echo "=============================================="
echo " 开通新租户: $NAME"
echo "   对外端口(nginx): $EXT_PORT"
echo "   后端端口:        $BACKEND_PORT"
echo "   H2 端口:         $H2_PORT"
echo "   数据目录:        $TENANT_DIR"
echo "=============================================="

# 1. 前置检查
if [ -d "$TENANT_DIR" ]; then
  echo "错误: 租户 $NAME 已存在 ($TENANT_DIR)"
  exit 1
fi
if ss -ltn 2>/dev/null | grep -qE ":$EXT_PORT[[:space:]]"; then
  echo "错误: 对外端口 $EXT_PORT 已被占用"
  exit 1
fi
if ss -ltn 2>/dev/null | grep -qE ":$BACKEND_PORT[[:space:]]"; then
  echo "错误: 后端端口 $BACKEND_PORT 已被占用"
  exit 1
fi

# 2. 创建目录 + env.sh
mkdir -p "$TENANT_DIR/data"
JWT_KEY=$(openssl rand -base64 48 | tr -dc 'a-zA-Z0-9' | head -c 40)
cat > "$TENANT_DIR/env.sh" <<EOF
APP_JWT_SECRET=${JWT_KEY}
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=${BACKEND_PORT}
H2_AUTO_SERVER_PORT=${H2_PORT}
EOF
chmod 600 "$TENANT_DIR/env.sh"
echo "✓ 已创建数据目录与配置"

# 3. systemd 服务
cat > "/etc/systemd/system/$SERVICE.service" <<EOF
[Unit]
Description=LawFirm tenant $NAME
After=network.target

[Service]
WorkingDirectory=$TENANT_DIR
EnvironmentFile=$TENANT_DIR/env.sh
ExecStart=/usr/bin/java -Xmx512m -XX:MaxMetaspaceSize=256m -jar $SHARED_JAR
Restart=always
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable "$SERVICE" >/dev/null 2>&1
systemctl start "$SERVICE"
echo "✓ 已创建并启动服务 $SERVICE"

# 4. 等待后端就绪（默认账号 seed 完成）
echo -n "  等待后端就绪"
READY=0
for _ in $(seq 1 40); do
  sleep 2
  code=$(curl -s -m 3 -o /dev/null -w "%{http_code}" "http://127.0.0.1:${BACKEND_PORT}/api/v3/api-docs" 2>/dev/null || true)
  if [ "$code" = "200" ]; then
    READY=1
    break
  fi
  echo -n "."
done
echo ""
if [ "$READY" != "1" ]; then
  echo "⚠ 后端未在 80 秒内就绪，请检查: systemctl status $SERVICE"
  exit 1
fi
echo "✓ 后端已就绪"

# 5. 清理演示数据（保留默认账号 + 审批模板）
echo -n "  清理演示数据..."
java -cp "$H2_JAR" org.h2.tools.Shell \
  -url "jdbc:h2:file:$TENANT_DIR/data/lawfirm-dev;AUTO_SERVER=TRUE" \
  -user sa -password "" \
  -sql "DELETE FROM CAL_PARTICIPANT; DELETE FROM CAL_EVENT; DELETE FROM CASE_CO_LAWYER; DELETE FROM CASE_PROGRESS; DELETE FROM CASE_CASE; DELETE FROM KNOW_ARTICLE; DELETE FROM CRM_INTERACTION; DELETE FROM CRM_CONTACT; DELETE FROM CRM_CLIENT;" \
  >/dev/null 2>&1
echo "✓"

# 6. nginx 配置
NGINX_CONF="/etc/nginx/conf.d/tenant-$NAME.conf"
cat > "$NGINX_CONF" <<EOF
server {
    listen $EXT_PORT;
    server_name _;
    client_max_body_size 200m;

    root $SHARED_DIST;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:$BACKEND_PORT;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_read_timeout 300s;
    }

    location /api/assistant/chat {
        proxy_pass http://127.0.0.1:$BACKEND_PORT;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_http_version 1.1;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 600s;
        proxy_send_timeout 600s;
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }
}
EOF
if nginx -t >/dev/null 2>&1; then
  systemctl reload nginx
  echo "✓ 已写入 nginx 配置并 reload"
else
  echo "⚠ nginx 配置校验失败，请检查 $NGINX_CONF"
fi

# 7. 防火墙放行
ufw allow "$EXT_PORT/tcp" >/dev/null 2>&1 && echo "✓ 已放行防火墙端口 $EXT_PORT"

# 8. 验证
sleep 1
FINAL_CODE=$(curl -s -m 8 -o /dev/null -w "%{http_code}" "http://127.0.0.1:${EXT_PORT}/" 2>/dev/null || echo "000")

echo ""
echo "=============================================="
echo " ✅ 租户 $NAME 开通完成"
echo "   访问地址:  http://47.107.62.86:$EXT_PORT"
echo "   默认账号:  admin / admin123  (请立即修改密码)"
echo "   后端端口:  $BACKEND_PORT   H2端口: $H2_PORT"
echo "   数据目录:  $TENANT_DIR"
echo "   首页校验:  HTTP $FINAL_CODE"
echo ""
echo " 常用命令:"
echo "   状态:  systemctl status $SERVICE"
echo "   重启:  systemctl restart $SERVICE"
echo "   查库:  java -cp $H2_JAR org.h2.tools.Shell -url \"jdbc:h2:file:$TENANT_DIR/data/lawfirm-dev;AUTO_SERVER=TRUE\" -user sa -password \"\""
echo "=============================================="
