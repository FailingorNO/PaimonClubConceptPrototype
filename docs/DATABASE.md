# 派蒙社 数据库设计

- DBMS：MySQL 8.0（生产）/ H2 2.x（云端容器，MySQL 兼容模式关闭，仅用标准 SQL 类型）
- 建表方式：**一键脚本 `sql/init-paimonclub.sql`**（建库 + 24 张表 + 种子数据，可重复执行）；也可依赖 JPA `hibernate.hbm2ddl.auto=update` 自动建表，再由 `SeedService` 在库为空时写入种子数据
- 导入命令：`scripts\init-db.bat root 123456` 或 `mysql -u root -p --default-character-set=utf8mb4 < sql/init-paimonclub.sql`（执行后自动输出 `table_count = 24`）
- 字符集：`utf8mb4` / `utf8mb4_general_ci`
- 主键：全部为 `BIGINT AUTO_INCREMENT`（`@GeneratedValue(strategy = IDENTITY)`）
- 命名：表名为 `t_` 前缀蛇形，**列名默认沿用 JPA 属性名（驼峰）**，未启用物理命名策略改写；本文档按语义写作蛇形，实际列名见 `src/main/resources/db/schema-mysql.sql`（由 `mysqldump` 从真实库导出，24 张表）
- 时间：统一 `LocalDateTime`，业务时区 `Asia/Shanghai`
- 逻辑删除/状态：用 `status` 字段表达（帖子 `PUBLISHED/PENDING/REJECTED/TAKEN_DOWN`，评论 `1` 正常 `0` 删除，商品/装扮/小游戏 `1` 上架 `0` 下架）

共 24 张表。

## 1. 用户与权限

### t_user 用户

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 主键 |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 登录名 |
| password_hash | VARCHAR(100) | NOT NULL | BCrypt 摘要 |
| nickname | VARCHAR(50) | | 昵称 |
| avatar_url | VARCHAR(255) | | 头像 |
| email | VARCHAR(120) | | 邮箱 |
| phone | VARCHAR(30) | | 手机 |
| role | VARCHAR(20) | NOT NULL | `USER` / `MODERATOR` / `ADMIN` |
| status | INT | NOT NULL | 1 正常 / 0 封禁 |
| points | INT | NOT NULL | 积分余额 |
| bio | VARCHAR(500) | | 个人简介 |
| last_sign_in_date | DATE | | 最后签到日 |
| created_at | DATETIME | | 注册时间 |

索引：`uk_user_username(username)`。

### t_user_decoration 用户装扮

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| user_id | BIGINT | 用户 |
| decoration_id | BIGINT | 装扮 |
| is_wearing | BOOLEAN | 是否佩戴 |
| expire_at | DATETIME | 过期时间（空为永久） |
| created_at | DATETIME | 购买时间 |

索引：`idx_ud_user(user_id)`，唯一约束 `(user_id, decoration_id)`。

## 2. 社区内容

### t_post_category 帖子分类

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| name | VARCHAR(50) | 分类名 |
| sort | INT | 排序值 |
| icon | VARCHAR(255) | 图标 |
| description | VARCHAR(255) | 描述 |

### t_post 帖子

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| user_id | BIGINT | 作者 |
| title | VARCHAR(200) | 标题 |
| content | VARCHAR(5000) | 正文（富文本/纯文本） |
| category_id | BIGINT | 分类 |
| status | VARCHAR(20) | `PENDING` / `PUBLISHED` / `REJECTED` / `TAKEN_DOWN` |
| like_count / comment_count / view_count / favorite_count | INT | 冗余计数 |
| top | BOOLEAN | 置顶 |
| tags | VARCHAR(255) | 逗号分隔标签 |
| reject_reason | VARCHAR(255) | 驳回/下架原因 |
| created_at / updated_at | DATETIME | 时间 |

索引：`idx_post_user(user_id)`、`idx_post_cat(category_id)`、`idx_post_status_created(status, created_at)`。

> 计数字段采用冗余存储 + 事务内自增，避免列表页 N+1 聚合查询。

### t_post_image 帖子图片

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| post_id | BIGINT | 帖子 |
| url | VARCHAR(500) | 图片地址 |
| sort | INT | 顺序 |

索引：`idx_pi_post(post_id)`。

### t_comment 评论（两级楼中楼）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| post_id | BIGINT | 帖子 |
| user_id | BIGINT | 作者 |
| parent_id | BIGINT | 直接父评论（顶层为 0） |
| root_id | BIGINT | 所属顶层评论（便于一次取整个楼层） |
| content | VARCHAR(1000) | 内容 |
| like_count | INT | 点赞数 |
| status | INT | 1 正常 / 0 删除 |
| created_at | DATETIME | 时间 |

索引：`idx_cm_post(post_id)`、`idx_cm_root(root_id)`。

### t_like 点赞（多态）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| user_id | BIGINT | 点赞人 |
| target_type | VARCHAR(20) | `POST` / `COMMENT` |
| target_id | BIGINT | 目标 ID |
| created_at | DATETIME | 时间 |

唯一约束：`uk_like(user_id, target_type, target_id)`，保证幂等点赞。

### t_favorite 收藏

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| user_id | BIGINT | 用户 |
| post_id | BIGINT | 帖子 |
| created_at | DATETIME | 时间 |

唯一约束：`uk_fav(user_id, post_id)`。

### t_share 分享

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| post_id | BIGINT | 帖子 |
| user_id | BIGINT | 分享人 |
| share_code | VARCHAR(32) | 短链码（唯一） |
| created_at | DATETIME | 时间 |

索引：`uk_share_code(share_code)`。

## 3. 游戏与抽卡

### t_game_account 绑定账号

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| user_id | BIGINT | 用户（唯一） |
| game_uid | VARCHAR(30) | 游戏 UID |
| server | VARCHAR(30) | 服务器 |
| game_name | VARCHAR(50) | 游戏名 |
| nickname | VARCHAR(50) | 游戏内昵称 |
| adventure_rank | INT | 冒险等级 |
| world_level | INT | 世界等级 |
| bind_time | DATETIME | 绑定时间 |

唯一约束：`uk_ga_user(user_id)`。

### t_gacha_record 抽卡记录

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| user_id | BIGINT | 用户 |
| game_uid | VARCHAR(30) | 游戏 UID |
| pool_name | VARCHAR(50) | 卡池 |
| item_name | VARCHAR(50) | 物品名 |
| item_type | VARCHAR(20) | `CHARACTER` / `WEAPON` |
| rarity | INT | 3 / 4 / 5 |
| pull_time | DATETIME | 抽卡时间 |

索引：`idx_gr_user_uid(user_id, game_uid)`、`idx_gr_pool(pool_name)`。

> 分析接口按 `pull_time` 升序扫描计算保底（pity）、五星率、平均出金间隔与欧非曲线，数据量在万级内为毫秒级。

### t_mini_game 小游戏

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| name | VARCHAR(50) | 名称 |
| path | VARCHAR(255) | 前端入口 |
| reward_points | INT | 单次奖励积分 |
| status | INT | 1 启用 |
| description / icon | VARCHAR | 说明与图标 |

### t_game_score 小游戏成绩

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| user_id | BIGINT | 用户 |
| game_id | BIGINT | 游戏 |
| score | INT | 分数 |
| created_at | DATETIME | 时间 |

索引：`idx_gs_game_score(game_id, score)`（排行榜倒序取前 N）。

## 4. 积分与商城

### t_points_record 积分流水

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| user_id | BIGINT | 用户 |
| amount | INT | 变动值（正增负减） |
| type | VARCHAR(30) | `SIGN_IN` / `POST` / `COMMENT` / `LIKE` / `GAME` / `EXCHANGE` / `ADMIN` |
| ref_id | BIGINT | 关联业务 ID |
| description | VARCHAR(255) | 说明 |
| created_at | DATETIME | 时间 |

索引：`idx_pr_user(user_id, created_at)`。

### t_product 商品

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| name | VARCHAR(100) | 名称 |
| description | VARCHAR(500) | 描述 |
| image | VARCHAR(255) | 图 |
| price_points | INT | 所需积分 |
| stock | INT | 库存 |
| type | VARCHAR(20) | `PHYSICAL` / `VIRTUAL` |
| status | INT | 1 上架 / 0 下架 |
| created_at | DATETIME | 时间 |

### t_order 兑换订单

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| user_id | BIGINT | 用户 |
| product_id | BIGINT | 商品 |
| product_name | VARCHAR(100) | 下单时名称快照 |
| points_cost | INT | 消耗积分 |
| status | VARCHAR(20) | `CREATED` / `FINISHED` / `CANCELLED` |
| created_at | DATETIME | 时间 |

索引：`idx_od_user(user_id)`。

### t_decoration 装扮

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| name | VARCHAR(50) | 名称 |
| type | VARCHAR(20) | `FRAME`（头像框）/ `TITLE`（头衔） |
| resource_url | VARCHAR(255) | 资源地址 |
| price | INT | 价格（积分） |
| status | INT | 1 上架 |

## 5. 社交

### t_follow 关注

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| follower_id | BIGINT | 关注者 |
| followee_id | BIGINT | 被关注者 |
| created_at | DATETIME | 时间 |

唯一约束：`uk_follow(follower_id, followee_id)`。

### t_friend 好友

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| user_id | BIGINT | 发起方 |
| friend_id | BIGINT | 对方 |
| status | VARCHAR(20) | `PENDING` / `ACCEPTED` / `BLOCKED` |
| created_at / updated_at | DATETIME | 时间 |

索引：`idx_fd_user(user_id)`、`idx_fd_friend(friend_id)`。

### t_message 私信

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| from_user | BIGINT | 发送方 |
| to_user | BIGINT | 接收方 |
| content | VARCHAR(1000) | 内容 |
| is_read | BOOLEAN | 是否已读 |
| created_at | DATETIME | 时间 |

索引：`idx_msg_from_to(from_user, to_user)`、`idx_msg_to_read(to_user, is_read)`。

### t_notification 通知

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| user_id | BIGINT | 接收人 |
| type | VARCHAR(30) | `LIKE` / `COMMENT` / `FOLLOW` / `SYSTEM` / `AUDIT` |
| content | VARCHAR(255) | 内容 |
| ref_id | BIGINT | 关联业务 ID |
| is_read | BOOLEAN | 是否已读 |
| created_at | DATETIME | 时间 |

索引：`idx_nt_user_read(user_id, is_read)`。

## 6. 审核与治理

### t_report 举报

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| reporter_id | BIGINT | 举报人 |
| target_type | VARCHAR(20) | `POST` / `COMMENT` / `USER` |
| target_id | BIGINT | 目标 ID |
| reason | VARCHAR(255) | 原因 |
| status | VARCHAR(20) | `PENDING` / `HANDLED` / `REJECTED` |
| handler_id | BIGINT | 处理人 |
| handle_result | VARCHAR(255) | 处理结论 |
| created_at | DATETIME | 时间 |

索引：`idx_rp_status(status)`。

### t_audit_record 审核记录

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| target_type | VARCHAR(20) | `POST` / `COMMENT` |
| target_id | BIGINT | 目标 ID |
| auditor_id | BIGINT | 审核人 |
| result | VARCHAR(20) | `PASS` / `REJECT` |
| reason | VARCHAR(255) | 原因 |
| created_at | DATETIME | 时间 |

索引：`idx_ar_target(target_type, target_id)`。

### t_config 系统配置

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| cfg_key | VARCHAR(64) | 配置键（唯一） |
| cfg_value | VARCHAR(500) | 配置值 |
| remark | VARCHAR(255) | 说明 |

默认配置：`audit.mode=PRE`、`sensitive.enabled=true`、`signin.points=10`、`post.points=5`、`game.reward.limit=50`。

## 7. 实体关系概览

```text
t_user 1 ── n t_post ── n t_post_image
  │            │
  │            ├── n t_comment (parent_id/root_id 自关联)
  │            ├── n t_like (多态：POST/COMMENT)
  │            ├── n t_favorite
  │            └── n t_share
  ├── 1 ── 1 t_game_account ── n t_gacha_record
  ├── n t_points_record ── n t_order ── 1 t_product
  ├── n t_user_decoration ── 1 t_decoration
  ├── n t_follow / t_friend / t_message / t_notification
  └── n t_report / t_audit_record（含 target 多态引用）
```

外键策略：**不使用数据库外键**，由 Service 层保证一致性（便于分库与灰度迁移）；关键唯一性通过唯一索引兜底。
