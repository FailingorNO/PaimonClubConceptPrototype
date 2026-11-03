# 派蒙社 部署手册

本文覆盖两条部署路径：

- **A. 本地 / 自建服务器部署（推荐，已验证）**：WildFly 41 + MySQL 8
- **B. Coze 云端部署（受限）**：`coze-dev deploy` + 容器内自建 JDK/WildFly + H2

---

## A. 本地 / 自建服务器部署

### A.1 前置组件

| 组件 | 版本 | 获取方式 |
| --- | --- | --- |
| JDK | 21 或 25 | 系统安装，`java -version` 可用 |
| Maven | 3.9.x | 系统安装或 `scripts/ensure-maven` 逻辑自动下载 |
| WildFly | 41.0.0.Final | [wildfly.org](https://www.wildfly.org/downloads/) 或 Maven Central `org.wildfly:wildfly-dist:41.0.0.Final:zip` |
| MySQL | 8.0+ | 本机或独立实例 |
| MySQL JDBC | 8.4.0 | Maven Central `com.mysql:mysql-connector-j:8.4.0` |

### A.2 数据库准备

```sql
CREATE DATABASE paimonclub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
-- 可选：独立账号
CREATE USER 'paimon'@'%' IDENTIFIED BY '<password>';
GRANT ALL PRIVILEGES ON paimonclub.* TO 'paimon'@'%';
FLUSH PRIVILEGES;
```

表结构无需手工创建：首次启动由 Hibernate `hbm2ddl.auto=update` 建表，随后 `SeedService` 写入种子数据。

**推荐做法：直接执行一键建库脚本**（含 24 张表 + 种子数据，可离线交付、可重复执行）：

```bat
scripts\init-db.bat root 123456 127.0.0.1 3306
```

等价于：

```bash
mysql -u root -p --default-character-set=utf8mb4 < sql/init-paimonclub.sql
```

脚本内容：`CREATE DATABASE paimonclub`（utf8mb4）→ `DROP TABLE IF EXISTS` + 24 张表 DDL → 9 组种子数据（5 个账号 / 5 个板块 / 6 篇帖子 / 3 条评论 / 4 个商品 / 4 个装扮 / 4 个小游戏 / 2 项配置 / 1 个游戏账号），执行完自带 `table_count = 24` 校验输出。默认管理员 `admin / admin123`。

> 注意：脚本会先 DROP 再建表，**会清空同名库中的既有数据**；只想补结构时改用 `src/main/resources/db/schema-mysql.sql`（纯 DDL，无种子数据）。

### A.3 安装 JDBC 驱动模块

```text
$WILDFLY_HOME/modules/com/mysql/main/
├── module.xml
└── mysql-connector-j-8.4.0.jar
```

`module.xml`：

```xml
<module xmlns="urn:jboss:module:1.9" name="com.mysql">
    <resources>
        <resource-root path="mysql-connector-j-8.4.0.jar"/>
    </resources>
    <dependencies>
        <module name="java.se"/>
        <module name="jakarta.transaction.api" optional="true"/>
        <module name="jakarta.resource.api" optional="true"/>
    </dependencies>
</module>
```

> 注意：WildFly 41 已无 `jakarta.api` 聚合模块，依赖写 `jakarta.api` 会报 `WFLYJCA0115 ... missing: [jakarta.api]`。

### A.4 配置数据源

新建 `ds.cli`：

```text
embed-server --server-config=standalone.xml
/subsystem=datasources/jdbc-driver=mysql:add(driver-name=mysql,driver-module-name=com.mysql,driver-class-name=com.mysql.cj.jdbc.Driver)
data-source add --name=PaimonDS --jndi-name=java:jboss/datasources/PaimonDS --driver-name=mysql --connection-url="jdbc:mysql://localhost:3306/paimonclub?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true" --user-name=root --password=123456
```

执行：

```bash
$WILDFLY_HOME/bin/jboss-cli.bat --file=ds.cli
```

验证：

```bash
$WILDFLY_HOME/bin/jboss-cli.bat -c "/subsystem=datasources/data-source=PaimonDS:test-connection-in-pool"
# 期望：outcome => "success"
```

> `embed-server` 结束时无需 `stop-embed-server`，进程退出即生效并写入 `standalone.xml`。

### A.5 构建与部署

```bash
mvn -DskipTests package
cp target/PaimonClub.war $WILDFLY_HOME/standalone/deployments/
$WILDFLY_HOME/bin/standalone.bat -b 0.0.0.0
```

首次启动日志应出现：

```text
PaimonClub seed finished: users=5 posts=6 products=4 ...
Deployed "PaimonClub.war"
```

### A.6 验证

```bash
curl http://localhost:8080/api/health
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

项目内置完整冒烟脚本（24 项断言）：

```bash
python tools/smoke.py
```

覆盖：登录/注册、帖子分页/分类/详情/点赞/评论、游戏数据、抽卡导入与分析、商品与兑换、订单、小游戏与排行榜、后台统计、签到、热榜、敏感词拦截、分享二维码、关注、通知、积分记录。

### A.7 常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `Access denied for user 'root'` | 密码错误 | 核对 `ds.cli` 中的 `--password` |
| `WFLYJCA0115 ... missing: [jakarta.api]` | 模块依赖写错 | 改为 `java.se` + 可选事务/资源模块 |
| 启动后 404 | war 未部署成功 | 检查 `deployments/` 下是否残留 `.failed` 标记文件 |
| `Data too long for column 'content'` | 旧版本用 `@Lob` 生成 `tinytext` | 已改为 `@Column(length=5000)`，重建库即可 |
| 登录返回 400 `Expected EOF token` | 请求体非严格 JSON（如 PowerShell 拼接） | 用 curl 或标准 JSON 客户端 |
| RESTEasy 未使用 JSON-B | `jjwt-jackson` 带入 Jackson 注解 | `web.xml` 中 `resteasy.preferJacksonOverJsonB=false` |
| 后台进程随终端退出 | Windows 下 `Start-Process` 子进程被回收 | 用 `run_in_background` 前台运行或注册为服务 |

### A.8 生产建议

- JWT 密钥通过环境变量 `PAIMON_JWT_SECRET` 覆盖默认值
- 上传目录挂载到持久卷，或替换 `FileService` 为对象存储
- WildFly 以 `standalone-full.xml` + 独立系统用户运行；前置 Nginx 终止 TLS
- 数据库开启慢查询日志与定时备份；`hbm2ddl.auto` 生产建议改 `validate` 并配合 `docs/DATABASE.md` 生成 DDL
- 进程内缓存（`RedisCacheService`）在多实例场景替换为真实 Redis

---

## B. Coze 云端部署（受限）

### B.1 配置文件

`.coze`：

```toml
[project]
project_name = "paimon-club"
project_type = "web"
requires = ["java-25"]

[deploy]
build = ["bash", "scripts/build.sh"]
run = ["bash", "scripts/run.sh", "-p", "5000"]
```

脚本职责：

| 脚本 | 职责 |
| --- | --- |
| `scripts/ensure-jdk.sh` | 检测系统 JDK；缺失时按 Corretto → Bell-sw → Microsoft 顺序下载 JDK 21 到 `.deploy/jdk` |
| `scripts/build.sh` | 准备 JDK/Maven → `mvn -DskipTests package` → 校验 war → 调用 `prepare-wildfly.sh` |
| `scripts/prepare-wildfly.sh` | 下载解压 WildFly 41 → 通过 CLI 创建 H2 数据源 `PaimonDS` → 复制 war 到 deployments |
| `scripts/run.sh` | 准备 JDK/WildFly → 解析可写运行目录（只读环境退到 `/tmp/paimon-runtime`）→ `standalone.sh -b 0.0.0.0 -Djboss.http.port=$PORT -Djboss.server.base.dir=<可写目录>` |

### B.2 部署命令

```bash
coze-dev deploy --format json --source-root "<项目绝对路径>" --application-type service --yes
coze-dev deploy --format json status --app-id <app_id> --deploy-id <deploy_id>
coze-dev deploy --format json build-log --app-id <app_id> --deploy-id <deploy_id>
coze-dev deploy --format json online --app-id <app_id>
```

### B.3 已知平台限制（实测）

1. **构建镜像不含 JDK**：构建阶段 `ensure-jdk.sh` 必须自举 JDK（实测走 Corretto 21 成功），否则 `mvn` 无法运行。
2. **JDK 官方源不可达**：Adoptium 下载重定向到 `github.com`，构建沙箱访问会 `curl: (28)` 超时；因此脚本按 Corretto → Bell-sw → Microsoft 顺序回退。
3. **运行镜像自带 JDK**：运行阶段平台注入 `JAVA_HOME=/opt/java/openjdk`（openjdk 25.0.2），`ensure-jdk.sh` 会直接复用，不再下载。
4. **运行期根文件系统只读**：源码与 `.deploy/wildfly-*` 被复制到 `/opt/bytefaas`，该目录**不可写**，仅 `/tmp` 可写。WildFly 默认把 `configuration/data/log/tmp/deployments` 放在 `JBOSS_HOME/standalone` 下，会报
   `WFLYSRV0126: Could not create server content directory: /opt/bytefaas/.deploy/wildfly-41.0.0.Final/standalone/data/content`，
   以及 logmanager 的 `FileHandler` 无法写日志文件，进程随即退出（exit status 1）。
   **解决**：`run.sh` 检测到 `WILDFLY_HOME/standalone` 不可写时，把 `HOME`、`TMPDIR` 与 WildFly `base.dir` 全部重定向到 `/tmp/paimon-runtime`，将只读的 `configuration` 复制到可写目录，再把 war 拷入 `deployments`，用 `-Djboss.server.base.dir`、`-Djava.io.tmpdir`、`-Duser.home` 启动。
5. **构建耗时**：首次构建需下载 JDK（约 200MB）、Maven（约 9MB）、WildFly（约 266MB）与全部依赖，实测构建阶段约 17 分钟，重试成本高。
6. **无状态容器**：重启即回到镜像初始状态，运行期写在 `/tmp` 的数据（含 H2 库文件、上传图片）不持久。

结论：**正式交付形态为 A 路径（本地/自建服务器）**；云端路径已具备完整自举与只读文件系统兼容能力，在允许外网下载、构建时长充足的环境中可直接复用。

### B.4 云端数据存储

云端使用 H2 文件库：`jdbc:h2:file:${jboss.server.data.dir}/paimonclub;MODE=MySQL`（用户 `sa`/`sa`），无需外部数据库；容器重建后数据丢失，仅适合演示。

注意：H2 2.4.240 **不支持** `AUTO_SERVER=TRUE` 与 `DB_CLOSE_ON_EXIT=FALSE` 同时出现，会抛
`org.h2.jdbc.JdbcSQLFeatureNotSupportedException: Feature not supported: "AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE" [50100-240]`，
导致 IronJacamar `IJ031084: Unable to create connection`、持久化单元 `gcPU` 启动失败（应用首页可访问但 `/api/*` 全部 404）。云端连接串因此只保留 `MODE=MySQL`。

### B.5 部署历史（App ID `7701001750840623158`，域名 `https://hpftw9n988.coze.site`）

| # | deploy_history_id | 结论 | 原因 |
| --- | --- | --- | --- |
| 1 | 7701002216743649321 | Failed | 运行环境无 JDK，`mvn` 不可用 |
| 2 | 7701003254586507307 | Failed | 探测 JDK 逻辑仍未自举成功 |
| 3 | 7701003712705118262 | Failed | 自举 JDK 但官方源重定向 github 超时 |
| 4 | 7701004615780843574 | Failed | 改用 Corretto 后构建成功，运行期只读文件系统导致 WildFly 启动退出 |
| 5 | 7701214748414607403 | Failed | 运行目录重定向 `/tmp` 生效，但只读 configuration 里 4 个 properties 不可读，`cp` 中断 |
| 6 | 7701217920722124842 | Succeeded | 配置复制改为逐文件容错；站点上线，但 H2 连接串不被支持导致 `/api/*` 404 |
| 7 | 7701220502576054314 | Succeeded | 修正 H2 连接串（去 `AUTO_SERVER`/`DB_CLOSE_ON_EXIT`，加 `MODE=MySQL`）；线上 24 项冒烟全部符合预期（23 项 200，敏感词用例按预期 400 拒绝） |
