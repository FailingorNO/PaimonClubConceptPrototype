package com.paimonclub.dto.response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全部出参 VO（JSON-B 直接序列化公有字段）。
 */
public final class Vos {

    private Vos() {
    }

    /** 用户信息 */
    public static class User {
        public Long id;
        public String username;
        public String nickname;
        public String avatarUrl;
        public String role;
        public Integer points;
        public String bio;
        public Integer status;
        /** 负责的板块 ID 逗号串，仅后台用户列表填充 */
        public String moderatedCategories;
        public String email;
        public String phone;
        public String createdAt;
        public long followers;
        public long followees;
        public long postCount;
        public Boolean followed;
        public Boolean friend;
        public Boolean self;
        public String gameUid;
        public Integer adventureRank;
        public String wearingTitle;
        public String wearingFrame;
        public Integer unreadNotifications;
    }

    /** 登录结果 */
    public static class LoginResult {
        public String token;
        public User user;
    }

    /** 帖子 */
    public static class Post {
        public Long id;
        public String title;
        public String content;
        public String summary;
        public Long categoryId;
        public String categoryName;
        public Long userId;
        public String authorName;
        public String authorAvatar;
        public String status;
        public Integer likeCount;
        public Integer commentCount;
        public Integer viewCount;
        public Integer favoriteCount;
        public Boolean top;
        public String tags;
        public List<String> images = new ArrayList<>();
        public Boolean liked;
        public Boolean favorited;
        public String rejectReason;
        public String createdAt;
    }

    /** 评论（含楼中楼） */
    public static class Comment {
        public Long id;
        public Long postId;
        public Long userId;
        public String userName;
        public String userAvatar;
        public Long parentId;
        public Long rootId;
        public String content;
        public Integer likeCount;
        public Boolean liked;
        public String createdAt;
        public List<Comment> children = new ArrayList<>();
    }

    /** 角色数据 */
    public static class Character {
        public String name;
        public String element;
        public Integer level;
        public Integer constellation;
        public String weapon;
    }

    /** 游戏账号数据 */
    public static class GameData {
        public String gameUid;
        public String server;
        public String nickname;
        public Integer adventureRank;
        public Integer worldLevel;
        public Integer achievementCount;
        public String bindTime;
        public List<Character> characters = new ArrayList<>();
        public Map<String, Integer> elementStats = new LinkedHashMap<>();
    }

    /** 保底区间统计 */
    public static class PityBucket {
        public int from;
        public int to;
        public int count;
    }

    /** 欧非曲线点 */
    public static class LuckPoint {
        public int index;
        public double rate;
    }

    /** 单卡池统计 */
    public static class PoolStat {
        public String poolName;
        public int total;
        public int fiveStar;
        public int fourStar;
        public double fiveStarRate;
    }

    /** 抽卡分析结果 */
    public static class GachaAnalysis {
        public int totalPulls;
        public int fiveStar;
        public int fourStar;
        public int threeStar;
        public double fiveStarRate;
        public double fourStarRate;
        public double avgPullsPerFiveStar;
        public int maxPity;
        public int currentPity;
        public List<PityBucket> pityDistribution = new ArrayList<>();
        public List<LuckPoint> luckCurve = new ArrayList<>();
        public List<PoolStat> poolStats = new ArrayList<>();
        public List<String> pools = new ArrayList<>();
    }

    /** 通知 */
    public static class Notification {
        /** 合并条数，>1 时前端可以显示「等 N 条」 */
        public Integer mergeCount;
        public Long id;
        public String type;
        public String content;
        public Long refId;
        public Boolean isRead;
        public String createdAt;
    }

    /** 商品 */
    public static class Product {
        public Long id;
        public String name;
        public String description;
        public String image;
        public Integer pricePoints;
        public Integer stock;
        public String type;
        public Integer status;
        public String createdAt;
    }

    /** 兑换订单 */
    public static class Order {
        public Long id;
        public Long productId;
        public String productName;
        public String productType;
        public Integer pointsCost;
        public String status;
        public String receiverName;
        public String receiverPhone;
        public String receiverAddress;
        public String logisticsNo;
        public String remark;
        public String shippedAt;
        public String finishedAt;
        public String createdAt;
    }

    /** 积分流水 */
    public static class PointsRecord {
        public Long id;
        public Integer amount;
        /** 变动后余额，便于用户与管理员核对 */
        public Integer balanceAfter;
        public String type;
        public String description;
        public String createdAt;
    }

    /** 小游戏 */
    public static class MiniGame {
        public Long id;
        public String name;
        public String path;
        public Integer rewardPoints;
        public String description;
        public String icon;
        /** 玩法标识：snake / 2048 / guess / gacha */
        public String gameKey;
        public Integer bestScore;
    }

    /** 排行榜条目 */
    public static class Score {
        public int rank;
        public Long userId;
        public String userName;
        public String avatarUrl;
        public Integer score;
    }

    /** 按天统计 */
    public static class DayCount {
        public String date;
        public long count;

        public DayCount() {
        }

        public DayCount(String date, long count) {
            this.date = date;
            this.count = count;
        }
    }

    /** 系统统计 */
    public static class Stats {
        public long userCount;
        public long postCount;
        public long commentCount;
        public long todayUsers;
        public long todayPosts;
        public long pendingPosts;
        public long pendingReports;
        public long orderCount;
        public long totalPointsGranted;
        public long totalPointsConsumed;
        public List<DayCount> postTrend = new ArrayList<>();
        public List<User> topUsers = new ArrayList<>();
    }

    /** 审核记录 */
    public static class Audit {
        public Long id;
        public String targetType;
        public Long targetId;
        public String result;
        public String reason;
        public String auditorName;
        public String createdAt;
        public String targetTitle;
    }

    /** 举报 */
    public static class Report {
        public Long id;
        public Long reporterId;
        public String reporterName;
        public String targetType;
        public Long targetId;
        public String reason;
        public String status;
        public String handleResult;
        public String createdAt;
        public String targetSummary;
    }

    /** 分享结果 */
    public static class Share {
        public String shareCode;
        public String url;
        public String qrCodeUrl;
        public Long postId;
    }

    /** 私信 */
    public static class Message {
        public Long id;
        public Long fromUser;
        public String fromName;
        public String fromAvatar;
        public Long toUser;
        public String content;
        public Boolean isRead;
        public String createdAt;
    }

    /** 装扮 */
    public static class Decoration {
        public Long id;
        public String name;
        public String type;
        public String resourceUrl;
        public Integer price;
        public Boolean owned;
        public Boolean wearing;
    }

    /** 好友/粉丝条目 */
    public static class RelationItem {
        public User user;
        public String status;
        public Long relationId;
        public Boolean incoming;
        public String createdAt;
    }

    /** 签到结果 */
    public static class SignIn {
        public Boolean signed;
        public Integer gain;
        public Integer totalPoints;
        public String message;
    }
}
