# 派蒙社 接口文档

- 基础地址：`http://<host>:<port>/api`
- 编码：`application/json;charset=UTF-8`
- 鉴权：除注册、登录、公开列表、分享查看、健康检查外，均需请求头 `Authorization: Bearer <token>`
- 统一响应：

```json
{ "code": 0, "message": "ok", "data": {} }
```

`code=0` 表示成功；`401` 未登录/令牌失效；`403` 无权限；`404` 资源不存在；`400` 参数校验失败；`500` 服务端异常。

分页响应：

```json
{ "code": 0, "data": { "list": [], "total": 0, "page": 1, "size": 10, "pages": 0 } }
```

---

## 1. 健康检查

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/health` | 否 | 返回服务状态与时间戳 |

---

## 2. 账号（/auth、/users）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/auth/register` | 否 | 注册。body：`username`、`password`、`nickname`、`email`(可选) |
| POST | `/auth/login` | 否 | 登录。body：`username`、`password`，返回 `token`、用户信息 |
| GET | `/auth/me` | 是 | 当前登录用户 |
| GET | `/users/me` | 是 | 个人资料（含积分、等级、装扮） |
| PUT | `/users/me` | 是 | 修改资料：`nickname`、`avatar`、`bio`、`signature` |
| PUT | `/users/me/password` | 是 | 修改密码：`oldPassword`、`newPassword` |
| POST | `/users/sign-in` | 是 | 每日签到，返回获得积分与连续天数 |
| GET | `/users/points/records` | 是 | 积分明细（分页） |
| GET | `/users/points/ranking` | 否 | 积分排行榜，参数 `limit` |
| GET | `/users/notifications` | 是 | 通知列表（分页） |
| GET | `/users/notifications/unread` | 是 | 未读通知数 |
| POST | `/users/notifications/read` | 是 | 标记已读，body：`ids` 或空表示全部 |
| GET | `/users/search` | 否 | 搜索用户，参数 `keyword`、`page`、`size` |
| GET | `/users/{id}` | 否 | 用户主页（帖子数、粉丝数、关注数） |

---

## 3. 社区帖子（/posts）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/posts` | 否 | 帖子列表。参数 `page`、`size`、`categoryId`、`keyword`、`sort`(latest/hot) |
| POST | `/posts` | 是 | 发帖。body：`title`、`content`、`categoryId`、`images[]`、`gameUid`(可选) |
| GET | `/posts/hot` | 否 | 热榜，参数 `limit` |
| GET | `/posts/mine` | 是 | 我的帖子（分页） |
| GET | `/posts/categories` | 否 | 分类列表 |
| GET | `/posts/{id}` | 否 | 帖子详情（自增浏览、返回作者/分类/是否点赞收藏） |
| PUT | `/posts/{id}` | 是 | 编辑（仅作者/管理员） |
| DELETE | `/posts/{id}` | 是 | 删除（仅作者/管理员） |
| GET | `/posts/{id}/related` | 否 | 相关推荐，参数 `limit` |
| GET | `/posts/{id}/comments` | 否 | 帖子评论（分页） |
| POST | `/posts/{id}/comments` | 是 | 发表评论。body：`content`、`parentId`(可选) |
| POST | `/posts/{id}/like` | 是 | 点赞/取消点赞（切换），返回当前状态与总数 |
| POST | `/posts/{id}/favorite` | 是 | 收藏/取消收藏（切换） |
| POST | `/posts/{id}/share` | 是 | 生成分享短链与二维码，返回 `code`、`url` |
| GET | `/posts/share/{code}` | 否 | 按分享码取帖子 |
| GET | `/posts/share/{code}/qrcode` | 否 | 分享二维码 PNG |
| POST | `/posts/{id}/report` | 是 | 举报帖子。body：`reason`、`detail` |

---

## 4. 评论（/comments）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/comments` | 是 | 发表评论。body：`postId`、`content`、`parentId`(可选) |
| GET | `/comments/post/{postId}` | 否 | 按帖子取评论（分页） |
| DELETE | `/comments/{id}` | 是 | 删除评论（作者/管理员） |
| POST | `/comments/{id}/like` | 是 | 评论点赞切换 |
| POST | `/comments/{id}/report` | 是 | 举报评论。body：`reason` |

---

## 5. 游戏数据（/game）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/game/account` | 是 | 绑定 UID。body：`gameUid`、`server`、`nickname` |
| GET | `/game/account` | 是 | 我的绑定信息 |
| DELETE | `/game/account` | 是 | 解绑 |
| GET | `/game/data` | 是 | 账号数据。参数 `gameUid`，返回冒险等级、世界等级、角色列表、绑定时间 |

---

## 6. 抽卡分析（/gacha）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/gacha/import` | 是 | 导入抽卡记录。body：`gameUid`、`records[]`(或 `simulate` 抽数) |
| GET | `/gacha/analysis` | 是 | 分析结果。参数 `gameUid`、`pool`(可选)，返回总数、五星率、四星率、maxPity、平均出金、欧非曲线、卡池对比 |
| GET | `/gacha/records` | 是 | 记录分页，参数 `gameUid`、`pool`、`page`、`size` |
| DELETE | `/gacha/records` | 是 | 清空我的记录（参数 `gameUid`） |

---

## 7. 积分商城（/mall）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/mall/products` | 否 | 商品列表（分页，含下架） |
| GET | `/mall/products/on-sale` | 否 | 在售商品 |
| POST | `/mall/exchange` | 是 | 兑换。body：`productId`、`quantity`，扣积分、减库存、生成订单 |
| GET | `/mall/orders/mine` | 是 | 我的订单 |
| GET | `/mall/orders` | 是 | 全部订单（管理员） |
| POST | `/mall/products` | 是 | 新增/更新商品（管理员）。body：`id`(更新时)、`name`、`type`(PHYSICAL/VIRTUAL)、`points`、`stock`、`description` |
| DELETE | `/mall/products/{id}` | 是 | 下架商品（管理员） |
| GET | `/mall/decorations` | 否 | 装扮列表（头像框/头衔） |
| GET | `/mall/decorations/mine` | 是 | 我的装扮 |
| POST | `/mall/decorations/{id}/buy` | 是 | 购买装扮 |
| POST | `/mall/decorations/{id}/wear` | 是 | 佩戴 |
| DELETE | `/mall/decorations/{id}/wear` | 是 | 卸下 |

---

## 8. 社交（/social）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/social/follow/{userId}` | 是 | 关注/取关切换 |
| GET | `/social/followers/{userId}` | 否 | 粉丝列表 |
| GET | `/social/followees/{userId}` | 否 | 关注列表 |
| POST | `/social/friends/action` | 是 | 好友操作。body：`userId`、`action`(REQUEST/ACCEPT/REJECT/DELETE/BLOCK/UNBLOCK) |
| GET | `/social/friends` | 是 | 好友列表 |
| GET | `/social/friends/requests` | 是 | 待处理好友申请 |
| GET | `/social/friends/blocked` | 是 | 黑名单 |
| GET | `/social/messages/inbox` | 是 | 会话列表（含未读数） |
| GET | `/social/messages/{userId}` | 是 | 与某用户的私信（分页，读取后置已读） |
| POST | `/social/messages` | 是 | 发送私信。body：`toUserId`、`content` |

---

## 9. 小游戏（/games）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/games` | 否 | 游戏列表（含入口 path、最高分） |
| POST | `/games/score` | 是 | 提交分数。body：`gameId`、`score`，按规则发放积分 |
| GET | `/games/{id}/leaderboard` | 否 | 排行榜，参数 `limit` |

---

## 10. 后台管理（/admin，需管理员/版主）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/admin/stats` | 管理员 | 看板：用户数、帖子数、待审帖子、待处理举报、今日新增 |
| GET | `/admin/users` | 管理员 | 用户列表（分页、关键词、状态） |
| PUT | `/admin/users/{id}` | 管理员 | 修改用户：`status`(NORMAL/BANNED)、`role`(USER/MODERATOR/ADMIN)、`points` |
| GET | `/admin/audits/pending` | 版主 | 待审帖子/评论 |
| POST | `/admin/audits` | 版主 | 审核。body：`targetType`、`targetId`、`pass`、`reason`、`takeDown` |
| GET | `/admin/audits/records` | 版主 | 审核记录（分页） |
| POST | `/admin/posts/{id}/top` | 版主 | 置顶/取消置顶。body：`top` |
| POST | `/admin/posts/{id}/takedown` | 版主 | 下架帖子。body：`reason` |
| GET | `/admin/reports` | 版主 | 举报列表（按状态） |
| POST | `/admin/reports/{id}/handle` | 版主 | 处理举报。body：`result`、`remark` |
| GET | `/admin/configs` | 管理员 | 系统配置列表 |
| PUT | `/admin/configs` | 管理员 | 更新配置。body：`key`、`value` |
| POST | `/admin/sensitive-words` | 管理员 | 追加敏感词。body：`words[]` |

---

## 11. 文件（/files）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/files/upload` | 是 | 上传图片。body：`fileName`、`data`(Base64)，返回访问 URL |
| GET | `/files/{name}` | 否 | 读取已上传文件 |

---

## 12. 典型调用示例

```bash
# 登录
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 带令牌发帖
curl -s -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"title":"标题","content":"正文","categoryId":1}'

# 抽卡分析
curl -s "http://localhost:8080/api/gacha/analysis?gameUid=800000001" \
  -H "Authorization: Bearer <token>"
```
