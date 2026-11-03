package com.paimonclub.common;

/**
 * 全局常量：角色、状态、积分规则、配置键。
 */
public final class Constants {

    private Constants() {
    }

    /* ===== 响应码 ===== */
    public static final int CODE_OK = 0;
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_SERVER_ERROR = 500;

    /* ===== 角色 ===== */
    public static final String ROLE_USER = "USER";
    public static final String ROLE_MODERATOR = "MODERATOR";
    public static final String ROLE_ADMIN = "ADMIN";

    /* ===== 用户状态 ===== */
    public static final int USER_NORMAL = 1;
    public static final int USER_BANNED = 0;

    /* ===== 帖子状态 ===== */
    public static final String POST_PENDING = "PENDING";
    public static final String POST_PUBLISHED = "PUBLISHED";
    /** 已删除（软删，行仍在，可恢复） */
    public static final String POST_DELETED = "DELETED";

    /* ===== 分级审核 ===== */
    /** 注册不满这么多天的账号，发帖一律先审后发 */
    public static final int NEW_USER_DAYS = 7;
    /** 已发布帖子少于这个数量的账号，同样先审后发 */
    public static final int NEW_USER_POST_THRESHOLD = 3;
    public static final String POST_REJECTED = "REJECTED";

    /* ===== 互动目标类型 ===== */
    public static final String TARGET_POST = "POST";
    public static final String TARGET_COMMENT = "COMMENT";
    public static final String TARGET_USER = "USER";

    /* ===== 好友状态 ===== */
    public static final String FRIEND_PENDING = "PENDING";
    public static final String FRIEND_ACCEPTED = "ACCEPTED";
    public static final String FRIEND_BLOCKED = "BLOCKED";

    /* ===== 举报状态 ===== */
    public static final String REPORT_PENDING = "PENDING";
    public static final String REPORT_HANDLED = "HANDLED";
    public static final String REPORT_REJECTED = "REJECTED";

    /* ===== 通知类型 ===== */
    public static final String NOTIFY_LIKE = "LIKE";
    public static final String NOTIFY_COMMENT = "COMMENT";
    public static final String NOTIFY_FOLLOW = "FOLLOW";
    public static final String NOTIFY_AUDIT = "AUDIT";
    public static final String NOTIFY_SYSTEM = "SYSTEM";
    public static final String NOTIFY_POINTS = "POINTS";

    /* ===== 积分规则（非功能性需求：积分规则可配置，此处为默认值） ===== */
    public static final int POINTS_POST_CREATE = 5;
    public static final int POINTS_LIKE_GIVE = 1;
    public static final int POINTS_LIKE_RECEIVE = 2;
    public static final int POINTS_COMMENT = 2;
    public static final int POINTS_BE_FAVORITED = 3;
    public static final int POINTS_SIGN_IN = 10;
    /** 新用户注册赠予，保证一进社区就能体验兑换装扮 */
    public static final int POINTS_WELCOME = 300;
    public static final int POINTS_GAME_REWARD = 5;

    public static final String POINT_TYPE_POST_CREATE = "POST_CREATE";
    public static final String POINT_TYPE_LIKE_GIVE = "LIKE_GIVE";
    public static final String POINT_TYPE_LIKE_RECEIVE = "LIKE_RECEIVE";
    public static final String POINT_TYPE_COMMENT = "COMMENT";
    public static final String POINT_TYPE_BE_FAVORITED = "BE_FAVORITED";
    public static final String POINT_TYPE_SIGN_IN = "SIGN_IN";
    public static final String POINT_TYPE_EXCHANGE = "EXCHANGE";
    public static final String POINT_TYPE_GAME_REWARD = "GAME_REWARD";
    public static final String POINT_TYPE_ADMIN = "ADMIN_ADJUST";
    public static final String POINT_TYPE_EXCHANGE_REFUND = "EXCHANGE_REFUND";

    /* ===== 订单状态 ===== */
    /** 待发货（实物商品下单后） */
    public static final String ORDER_CREATED = "CREATED";
    /** 已发货 */
    public static final String ORDER_SHIPPED = "SHIPPED";
    /** 已完成（虚拟商品下单即完成 / 实物商品已签收） */
    public static final String ORDER_COMPLETED = "COMPLETED";
    /** 已取消（待发货阶段取消，积分已退还） */
    public static final String ORDER_CANCELED = "CANCELED";

    /* ===== 商品类型 ===== */
    public static final String PRODUCT_VIRTUAL = "VIRTUAL";
    public static final String PRODUCT_PHYSICAL = "PHYSICAL";

    /* ===== 系统配置键 ===== */
    /** 审核模式：PRE=先审后发，POST=先发后审 */
    public static final String CFG_AUDIT_MODE = "audit.mode";
    public static final String CFG_AUDIT_MODE_PRE = "PRE";
    public static final String CFG_AUDIT_MODE_POST = "POST";
    /** 敏感词过滤开关 */
    public static final String CFG_SENSITIVE_ENABLED = "sensitive.enabled";
    /** 每页默认大小上限 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 默认头像：相对路径，适配任意部署上下文 */
    public static final String DEFAULT_AVATAR = "static/img/avatar-default.svg";
}
