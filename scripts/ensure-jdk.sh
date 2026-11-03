#!/bin/bash
# 确保存在可用 JDK：优先使用系统 JDK，否则下载 Linux x64 JDK 21 到 .deploy/jdk。
# 用法：source scripts/ensure-jdk.sh
set -Eeuo pipefail

_ENSURE_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
_ENSURE_PROJECT_DIR="$(cd "$_ENSURE_SCRIPT_DIR/.." && pwd)"
_ENSURE_TOOLS_DIR="$_ENSURE_PROJECT_DIR/.deploy"
_ENSURE_JDK_HOME="$_ENSURE_TOOLS_DIR/jdk"

# 候选下载源（依次尝试，避免单一站点不可达）
_JDK_URLS=(
  "https://corretto.aws/downloads/latest/amazon-corretto-21-x64-linux-jdk.tar.gz"
  "https://download.bell-sw.com/java/21.0.5+11/bellsoft-jdk21.0.5+11-linux-amd64.tar.gz"
  "https://aka.ms/download-jdk/microsoft-jdk-21-linux-x64.tar.gz"
)

if [ -z "${JAVA_HOME:-}" ] && command -v java >/dev/null 2>&1; then
  _JAVA_REAL="$(readlink -f "$(command -v java)" 2>/dev/null || command -v java)"
  JAVA_HOME="$(cd "$(dirname "$_JAVA_REAL")/.." && pwd)"
  export JAVA_HOME
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/java" ]; then
  if [ ! -x "$_ENSURE_JDK_HOME/bin/java" ]; then
    mkdir -p "$_ENSURE_TOOLS_DIR" "$_ENSURE_JDK_HOME"
    _JDK_OK=0
    for _url in "${_JDK_URLS[@]}"; do
      echo "[jdk] 尝试下载 JDK 21: $_url" >&2
      if curl -fsSL --connect-timeout 30 --max-time 600 "$_url" -o "$_ENSURE_TOOLS_DIR/jdk.tar.gz"; then
        if tar -xzf "$_ENSURE_TOOLS_DIR/jdk.tar.gz" -C "$_ENSURE_JDK_HOME" --strip-components=1; then
          _JDK_OK=1
          break
        fi
      fi
      rm -f "$_ENSURE_TOOLS_DIR/jdk.tar.gz"
    done
    rm -f "$_ENSURE_TOOLS_DIR/jdk.tar.gz"
    if [ "$_JDK_OK" != "1" ]; then
      echo "[jdk] 所有 JDK 下载源均不可用" >&2
      exit 1
    fi
  fi
  JAVA_HOME="$_ENSURE_JDK_HOME"
  export JAVA_HOME
fi

export PATH="$JAVA_HOME/bin:$PATH"
echo "[jdk] JAVA_HOME=$JAVA_HOME" >&2
"$JAVA_HOME/bin/java" -version 2>&1 | head -n 1 >&2 || true
