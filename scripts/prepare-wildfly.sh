#!/bin/bash
# 准备 WildFly 运行环境：下载解压、配置 H2 数据源、部署 WAR。幂等，可重复执行。
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

WILDFLY_VERSION="41.0.0.Final"
WILDFLY_URL="https://repo1.maven.org/maven2/org/wildfly/wildfly-dist/$WILDFLY_VERSION/wildfly-dist-$WILDFLY_VERSION.zip"
TOOLS_DIR="$PROJECT_DIR/.deploy"
WILDFLY_HOME="$TOOLS_DIR/wildfly-$WILDFLY_VERSION"

# 1. 下载并解压 WildFly（仅首次）
if [ ! -x "$WILDFLY_HOME/bin/standalone.sh" ]; then
  mkdir -p "$TOOLS_DIR"
  if [ ! -f "$TOOLS_DIR/wildfly.zip" ]; then
    echo "[prepare] 下载 WildFly $WILDFLY_VERSION ..." >&2
    curl -fsSL "$WILDFLY_URL" -o "$TOOLS_DIR/wildfly.zip"
  fi
  echo "[prepare] 解压 WildFly ..." >&2
  ( cd "$TOOLS_DIR" && unzip -q -o wildfly.zip )
  rm -f "$TOOLS_DIR/wildfly.zip"
fi

# 2. 配置 H2 数据源（JNDI 与 persistence.xml 保持一致）
CLI_FILE="$TOOLS_DIR/paimon-ds.cli"
cat > "$CLI_FILE" <<'CLI_EOF'
embed-server --server-config=standalone.xml
if (outcome != success) of /subsystem=datasources/data-source=PaimonDS:read-resource
    data-source add --name=PaimonDS --jndi-name=java:jboss/datasources/PaimonDS --driver-name=h2 --connection-url="jdbc:h2:file:${jboss.server.data.dir}/paimonclub;MODE=MySQL" --user-name=sa --password=sa --min-pool-size=2 --max-pool-size=20 --validate-on-match=false --background-validation=true --share-prepared-statements=false
end-if
/subsystem=datasources/data-source=PaimonDS:write-attribute(name=connection-url, value="jdbc:h2:file:${jboss.server.data.dir}/paimonclub;MODE=MySQL")
CLI_EOF
"$WILDFLY_HOME/bin/jboss-cli.sh" --file="$CLI_FILE" >/dev/null 2>&1 \
  && echo "[prepare] 数据源 PaimonDS 就绪" >&2 \
  || echo "[prepare] 数据源配置未生效（可能已存在），继续" >&2

# 3. 部署 WAR
WAR_FILE="$(ls -1 "$PROJECT_DIR"/target/*.war 2>/dev/null | head -n 1 || true)"
if [ -n "$WAR_FILE" ]; then
  cp -f "$WAR_FILE" "$WILDFLY_HOME/standalone/deployments/PaimonClub.war"
  echo "[prepare] 已部署 $(basename "$WAR_FILE")" >&2
fi

echo "[prepare] 完成" >&2
