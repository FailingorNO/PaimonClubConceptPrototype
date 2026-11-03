#!/bin/bash
# 派蒙社启动脚本：确保 JDK 与 WildFly 就绪后前台启动服务。
#
# 兼容只读根文件系统（如 Coze/veFaaS 运行时仅 /tmp 可写）：
# WildFly 的 configuration/data/log/tmp/deployments 全部重定向到 /tmp 下的可写目录，
# JBOSS_HOME 本身保持只读使用。
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

PORT="5000"
WILDFLY_VERSION="41.0.0.Final"
WILDFLY_HOME="$PROJECT_DIR/.deploy/wildfly-$WILDFLY_VERSION"

usage() {
  echo "Usage: $0 [-p port]"
}

while getopts "p:h" opt; do
  case "$opt" in
    p) PORT="$OPTARG" ;;
    h) usage; exit 0 ;;
    \?) echo "Invalid option: -$OPTARG" >&2; usage; exit 1 ;;
  esac
done

# shellcheck source=scripts/ensure-jdk.sh
source "$SCRIPT_DIR/ensure-jdk.sh"

# 构建阶段已准备过；若运行环境缺失则重新准备
if [ ! -x "$WILDFLY_HOME/bin/standalone.sh" ]; then
  bash "$SCRIPT_DIR/prepare-wildfly.sh"
fi

# ---------------------------------------------------------------------------
# 可写目录解析：优先使用 WILDFLY_HOME/standalone，只读环境下退到 /tmp
# ---------------------------------------------------------------------------
RUNTIME_ROOT="${PAIMON_RUNTIME_ROOT:-/tmp/paimon-runtime}"
if [ ! -w "$WILDFLY_HOME/standalone" ] || [ ! -w "$WILDFLY_HOME/standalone/configuration" ]; then
  echo "[run] $WILDFLY_HOME 不可写，切换运行目录到 $RUNTIME_ROOT" >&2
  export HOME="$RUNTIME_ROOT/home"
  export TMPDIR="$RUNTIME_ROOT/tmp"
  mkdir -p "$HOME" "$TMPDIR"
  BASE_DIR="$RUNTIME_ROOT/standalone"
else
  BASE_DIR="$WILDFLY_HOME/standalone"
  mkdir -p "$BASE_DIR/tmp"
  export TMPDIR="$BASE_DIR/tmp"
fi

mkdir -p "$BASE_DIR/configuration" "$BASE_DIR/data" "$BASE_DIR/log" "$BASE_DIR/tmp" "$BASE_DIR/deployments"

# 配置只读来源 -> 可写 base 目录（幂等）
# 注意：构建期生成的个别配置文件可能对运行用户不可读，逐个复制并跳过失败项，不能因此中断启动
SRC_CONF="$WILDFLY_HOME/standalone/configuration"
if [ -d "$SRC_CONF" ] && [ "$(cd "$SRC_CONF" && pwd -P)" != "$(cd "$BASE_DIR/configuration" && pwd -P)" ]; then
  for f in "$SRC_CONF"/*; do
    [ -e "$f" ] || continue
    cp -a "$f" "$BASE_DIR/configuration/" 2>/dev/null || echo "[run] 跳过不可读配置: $(basename "$f")" >&2
  done
  chmod -R u+rwX "$BASE_DIR/configuration" 2>/dev/null || true
  # Elytron properties-realm 会引用这些文件，缺失时补空文件避免启动失败
  for f in application-users.properties application-roles.properties mgmt-users.properties mgmt-groups.properties; do
    [ -f "$BASE_DIR/configuration/$f" ] || : > "$BASE_DIR/configuration/$f"
  done
  if [ ! -f "$BASE_DIR/configuration/standalone.xml" ]; then
    echo "[run] 缺少 standalone.xml，无法启动 WildFly" >&2
    exit 1
  fi
fi

# 部署 WAR：优先 target 产物，其次构建阶段已放进 deployments 的包
WAR_FILE="$(ls -1 "$PROJECT_DIR"/target/*.war 2>/dev/null | head -n 1 || true)"
if [ -z "$WAR_FILE" ] || [ ! -f "$WAR_FILE" ]; then
  WAR_FILE="$WILDFLY_HOME/standalone/deployments/PaimonClub.war"
fi
if [ -z "$WAR_FILE" ] || [ ! -f "$WAR_FILE" ]; then
  echo "[run] 未找到 WAR 产物，请先执行 build.sh" >&2
  exit 1
fi
cp -f "$WAR_FILE" "$BASE_DIR/deployments/PaimonClub.war" 2>/dev/null || {
  echo "[run] WAR 复制失败: $WAR_FILE" >&2
  exit 1
}

echo "[run] 启动 WildFly，端口 $PORT，base.dir=$BASE_DIR" >&2

exec "$WILDFLY_HOME/bin/standalone.sh" \
  -b 0.0.0.0 -bmanagement 0.0.0.0 \
  -Djboss.http.port="$PORT" \
  -Djboss.server.base.dir="$BASE_DIR" \
  -Djava.io.tmpdir="$BASE_DIR/tmp" \
  -Duser.home="${HOME:-$BASE_DIR}"
