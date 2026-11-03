#!/bin/bash
# 派蒙社构建脚本：确保 JDK 可用、使用 Maven 打包 WAR，并准备 WildFly 运行环境。
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

MVN_VERSION="3.9.6"
TOOLS_DIR="$PROJECT_DIR/.deploy"
MAVEN_LOCAL="$TOOLS_DIR/apache-maven-$MVN_VERSION"

# 确保 JDK 可用（云端构建环境默认没有 JDK 时自动下载 Temurin 21）
# shellcheck source=scripts/ensure-jdk.sh
source "$SCRIPT_DIR/ensure-jdk.sh"

resolve_mvn() {
  if command -v mvn >/dev/null 2>&1; then
    command -v mvn
    return 0
  fi
  if [ -x "$MAVEN_LOCAL/bin/mvn" ]; then
    echo "$MAVEN_LOCAL/bin/mvn"
    return 0
  fi
  echo "[build] 未找到 Maven，开始下载 $MVN_VERSION ..." >&2
  mkdir -p "$TOOLS_DIR"
  curl -fsSL "https://repo1.maven.org/maven2/org/apache/maven/apache-maven/$MVN_VERSION/apache-maven-$MVN_VERSION-bin.tar.gz" \
    -o "$TOOLS_DIR/maven.tar.gz"
  tar -xzf "$TOOLS_DIR/maven.tar.gz" -C "$TOOLS_DIR"
  rm -f "$TOOLS_DIR/maven.tar.gz"
  echo "$MAVEN_LOCAL/bin/mvn"
}

MVN_BIN="$(resolve_mvn)"
echo "[build] 使用 Maven: $MVN_BIN"
"$MVN_BIN" -B -DskipTests package

WAR_FILE="$(ls -1 "$PROJECT_DIR"/target/*.war 2>/dev/null | head -n 1 || true)"
if [ -z "$WAR_FILE" ]; then
  echo "[build] 未生成 WAR 产物" >&2
  exit 1
fi
echo "[build] 构建完成: $WAR_FILE"

# 预置 WildFly 与数据源，缩短运行阶段启动时间
bash "$SCRIPT_DIR/prepare-wildfly.sh"
