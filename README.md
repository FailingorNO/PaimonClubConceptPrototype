# 派蒙社（Paimon Club）

基于 **Jakarta EE 11 + WildFly 41 + MySQL 8/H2 + Maven** 实现的原神玩家社区平台，涵盖社区内容、游戏数据、抽卡分析、积分商城、社交关系、后台审核与小游戏七大模块。

- 后端：Jakarta EE 11（JAX-RS / CDI / EJB / JPA 3.2），JWT 鉴权，BCrypt 密码
- 前端：原生 HTML + CSS + JavaScript（无外部 CDN 依赖），单文件页面直出
- 数据库：MySQL 8（本地）/ H2（云端容器），JPA 自动建表 + 首次启动种子数据

---

## 1. 快速开始

### 1.1 环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 21+ | 项目以 `release 21` 编译，已在 JDK 25 上验证 |
| Maven | 3.9+ | 构建 WAR |
| WildFly | 41.0.0.Final | Jakarta EE 11 全平台容器 |
| MySQL | 8.0+ | 本地开发数据库（可选，云端用 H2） |

### 1.2 构建

```bash
mvn -DskipTests package
# 产物：target/PaimonClub.war
```

### 1.3 本地运行（MySQL）

1. 建库（含 24 张表与种子数据，一条命令）：

```bat
scripts\init-db.bat root 123456
```

等价于手工执行 `mysql -u root -p --default-character-set=utf8mb4 < sql/init-paimonclub.sql`；
该脚本会 `CREATE DATABASE paimonclub`（utf8mb4）、重建 24 张表、写入 5 个账号/5 个板块/6 篇帖子/4 个商品/4 个装扮/4 个小游戏/2 项配置，结束时输出 `table_count = 24`。
默认管理员 `admin / admin123`。只想建结构不写种子数据时用 `src/main/resources/db/schema-mysql.sql`。

2. 安装 MySQL JDBC 驱动模块（WildFly 默认不带）：

```bash
# modules/com/mysql/main/ 下放 mysql-connector-j-8.4.0.jar 与 module.xml
```

`module.xml` 内容：

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

3. 配置数据源（JNDI：`java:jboss/datasources/PaimonDS`）：

```bash
jboss-cli.bat --file=ds.cli
```

```text
embed-server --server-config=standalone.xml
/subsystem=datasources/jdbc-driver=mysql:add(driver-name=mysql,driver-module-name=com.mysql,driver-class-name=com.mysql.cj.jdbc.Driver)
data-source add --name=PaimonDS --jndi-name=java:jboss/datasources/PaimonDS --driver-name=mysql \
  --connection-url="jdbc:mysql://localhost:3306/paimonclub?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true" \
  --user-name=root --password=<你的密码>
```

4. 部署并启动：

```bash
cp target/PaimonClub.war $WILDFLY_HOME/standalone/deployments/
$WILDFLY_HOME/bin/standalone.bat -b 0.0.0.0
```

5. 访问 `http://localhost:8080/`，健康检查 `http://localhost:8080/api/health`。

### 1.4 云端运行（H2，无需外部数据库）

```bash
bash scripts/build.sh   # 自动准备 JDK（如缺失）、Maven、WildFly 并打包
bash scripts/run.sh -p 5000
```

`scripts/prepare-wildfly.sh` 会以幂等方式创建 H2 数据源（`java:jboss/datasources/PaimonDS`），业务代码与 `persistence.xml` 无需改动。

---

## 2. 目录结构

```text
Paimon Club/
├── pom.xml
├── .coze                     # Coze 部署描述（service + java 运行时）
├── scripts/                  # 部署脚本（构建、启动、JDK/WildFly 准备）
├── src/main/java/com/paimonclub/
│   ├── common/               # Result、PageResult、异常、JWT、当前用户
│   ├── config/               # JAX-RS 应用、鉴权过滤器、CORS、异常映射、启动初始化
│   ├── dao/                  # BaseDao + 24 个实体 DAO
│   ├── dto/                  # 请求体（Reqs）与出参（Vos）
│   ├── entity/               # 24 个 JPA 实体
│   ├── rest/                 # 12 个 JAX-RS 资源类
│   ├── scheduler/            # EJB 定时任务
│   ├── service/              # 业务服务（19 个）
│   └── util/                 # 密码、XSS、敏感词、二维码、文件存储、日期
├── src/main/resources/
│   ├── META-INF/{beans.xml,persistence.xml}
│   └── db/sensitive_words.txt
└── src/main/webapp/
    ├── index.html 等 13 个页面
    ├── WEB-INF/{web.xml,jboss-web.xml}
    └── static/{css,js,img}
```

---

## 3. 功能模块

| 模块 | 主要能力 | 关键接口 |
| --- | --- | --- |
| 账号 | 注册、登录、JWT、资料、改密、签到、积分排行 | `/api/auth/*`、`/api/users/*` |
| 社区 | 发帖、编辑、删除、分类、置顶、热榜、敏感词过滤 | `/api/posts/*` |
| 评论 | 两级楼中楼、删除、点赞、举报 | `/api/comments/*` |
| 互动 | 点赞、收藏、分享（短链 + 二维码） | `/api/posts/{id}/like|favorite|share` |
| 游戏数据 | 绑定 UID、角色列表、账号数据 | `/api/game/*` |
| 抽卡分析 | 记录导入（支持模拟 180 抽）、五星率、保底分布、欧非曲线、卡池对比 | `/api/gacha/*` |
| 积分商城 | 商品列表、积分兑换、订单、库存 | `/api/mall/*` |
| 装扮 | 头像框/头衔购买与佩戴 | `/api/mall/decorations/*` |
| 社交 | 关注、好友、私信、消息未读数 | `/api/social/*` |
| 审核 | 待审列表、通过/驳回、下架、审核记录 | `/api/admin/audits/*` |
| 举报 | 提交举报、处理举报 | `/api/reports`、`/api/admin/reports` |
| 小游戏 | 游戏列表、提交分数、排行榜、积分奖励 | `/api/games/*` |
| 后台 | 数据看板、用户管理、系统配置、敏感词维护、商品维护 | `/api/admin/*` |

## 4. 演示账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| admin | admin123 | 管理员 |
| moderator | mod123 | 版主 |
| traveler | 123456 | 普通用户 |
| paimon | 123456 | 普通用户 |
| venti | 123456 | 普通用户 |

首次启动由 `SeedService` 自动写入（检测到已有用户则跳过），包含 5 个分类、6 篇帖子、4 个商品、4 个装扮、4 个小游戏。

## 5. 配置项

| 配置 | 位置 | 说明 |
| --- | --- | --- |
| `paimon.jwt.secret` / 环境变量 `PAIMON_JWT_SECRET` | 系统属性/环境变量 | JWT 签名密钥，生产环境务必覆盖 |
| `paimon.upload.dir` | 系统属性 | 上传目录，默认 `${jboss.server.data.dir}/paimon-uploads` |
| 审核模式 | 表 `t_sys_config` 键 `audit.mode` | `PRE` 先审后发 / `POST` 先发后审 |
| 敏感词开关 | 表 `t_sys_config` 键 `sensitive.enabled` | 是否启用敏感词过滤 |

## 6. 文档

- [接口文档](docs/API.md)
- [数据库设计](docs/DATABASE.md)
- [部署手册](docs/DEPLOY.md)

## 7. 已知限制

- 云端部署目标平台（Coze 部署服务）默认只提供 Node/Python 运行时，Java 运行时需在构建脚本中自行安装 JDK；若构建沙箱无法访问 JDK 下载源，云端部署会失败，此时请以本地 WildFly 部署为准。
- 缓存使用进程内 TTL 实现（`RedisCacheService`），多实例部署需替换为真实 Redis。
