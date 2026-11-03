package com.paimonclub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 全部入参 DTO（含 Bean Validation 校验注解）。
 * 使用公有字段 + JSON-B 反序列化，保持简洁。
 */
public final class Reqs {

    private Reqs() {
    }

    /** 注册 */
    public static class Register {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 20, message = "用户名长度需在 3-20 之间")
        public String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度需在 6-32 之间")
        public String password;

        @Size(max = 50, message = "昵称过长")
        public String nickname;

        @Size(max = 100, message = "邮箱过长")
        public String email;
    }

    /** 登录 */
    public static class Login {
        @NotBlank(message = "用户名不能为空")
        public String username;

        @NotBlank(message = "密码不能为空")
        public String password;
    }

    /** 资料修改 */
    public static class Profile {
        @Size(max = 50, message = "昵称过长")
        public String nickname;
        @Size(max = 500, message = "头像地址过长")
        public String avatarUrl;
        @Size(max = 255, message = "签名过长")
        public String bio;
        @Size(max = 100, message = "邮箱过长")
        public String email;
        @Size(max = 30, message = "手机号过长")
        public String phone;
    }

    /** 修改密码 */
    public static class Password {
        @NotBlank(message = "原密码不能为空")
        public String oldPassword;

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 32, message = "新密码长度需在 6-32 之间")
        public String newPassword;
    }

    /** 发帖 / 编辑帖子 */
    public static class Post {
        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题不能超过 200 字")
        public String title;

        @NotBlank(message = "正文不能为空")
        public String content;

        @NotNull(message = "请选择分类")
        public Long categoryId;

        @Size(max = 255, message = "标签过长")
        public String tags;

        /** Base64 data-url 或已上传文件地址 */
        public List<String> images;
    }

    /** 评论 */
    public static class Comment {
        @NotNull(message = "帖子 ID 不能为空")
        public Long postId;

        /** 父评论 ID，0 表示直接评论 */
        public Long parentId;

        @NotBlank(message = "评论内容不能为空")
        @Size(max = 1000, message = "评论不能超过 1000 字")
        public String content;
    }

    /** 绑定游戏账号（昵称、冒险等阶、世界等级全部由用户自己填写） */
    public static class BindGame {
        @NotBlank(message = "游戏 UID 不能为空")
        @Size(max = 30, message = "游戏 UID 过长")
        public String gameUid;

        @Size(max = 30, message = "区服过长")
        public String server;

        @Size(max = 50, message = "游戏名过长")
        public String gameName;

        @Size(max = 50, message = "游戏昵称过长")
        public String nickname;

        @Min(value = 1, message = "冒险等阶至少为 1")
        @Max(value = 60, message = "冒险等阶最大 60")
        public Integer adventureRank;

        @Min(value = 1, message = "世界等级至少为 1")
        @Max(value = 8, message = "世界等级最大 8")
        public Integer worldLevel;
    }

    /** 玩家自己录入的角色 */
    public static class GameCharacter {
        @NotBlank(message = "角色名不能为空")
        @Size(max = 30, message = "角色名过长")
        public String name;

        @Size(max = 20, message = "元素过长")
        public String element;

        @Size(max = 30, message = "武器过长")
        public String weapon;

        @Min(value = 1, message = "等级至少为 1")
        @Max(value = 90, message = "等级最大 90")
        public Integer level;

        @Min(value = 0, message = "命座数不能为负")
        @Max(value = 6, message = "命座最多 6")
        public Integer constellation;
    }

    /** 抽卡记录导入 */
    public static class GachaImport {
        @Size(max = 30, message = "游戏 UID 过长")
        public String gameUid;

        @Size(max = 50, message = "卡池名过长")
        public String poolName;

        /** 手动导入的记录；为空且 mock=true 时由服务端生成模拟数据 */
        public List<Item> records;

        public Boolean mock;

        public static class Item {
            public String itemName;
            public String itemType;
            public Integer rarity;
            public String pullTime;
        }
    }

    /** 商城兑换 */
    public static class Exchange {
        @NotNull(message = "商品 ID 不能为空")
        public Long productId;

        /** 实物商品必填：收货人 */
        @Size(max = 50, message = "收货人过长")
        public String receiverName;

        /** 实物商品必填：联系电话 */
        @Size(max = 30, message = "联系电话过长")
        public String receiverPhone;

        /** 实物商品必填：收货地址 */
        @Size(max = 255, message = "收货地址过长")
        public String receiverAddress;

        @Size(max = 255, message = "备注过长")
        public String remark;
    }

    /** 管理员发货 */
    public static class Ship {
        @NotBlank(message = "物流单号不能为空")
        @Size(max = 50, message = "物流单号过长")
        public String logisticsNo;
    }

    /** 佩戴装扮 */
    public static class DecorationWear {
        @NotNull(message = "装扮 ID 不能为空")
        public Long decorationId;
    }

    /** 举报 */
    public static class Report {
        @NotBlank(message = "举报对象类型不能为空")
        public String targetType;

        @NotNull(message = "举报对象 ID 不能为空")
        public Long targetId;

        @NotBlank(message = "请填写举报原因")
        @Size(max = 255, message = "举报原因过长")
        public String reason;
    }

    /** 私信 */
    public static class Message {
        @NotNull(message = "接收人不能为空")
        public Long toUser;

        @NotBlank(message = "内容不能为空")
        @Size(max = 1000, message = "内容不能超过 1000 字")
        public String content;
    }

    /** 好友操作 */
    public static class FriendAction {
        @NotNull(message = "目标用户不能为空")
        public Long friendId;

        /** APPLY / ACCEPT / REJECT / DELETE / BLOCK / UNBLOCK */
        @NotBlank(message = "操作类型不能为空")
        public String action;
    }

    /** 小游戏分数上传 */
    public static class Score {
        @NotNull(message = "游戏 ID 不能为空")
        public Long gameId;

        @NotNull(message = "分数不能为空")
        public Integer score;

        /** 单局耗时（毫秒），可选；服务端据此识别「0 秒通关」这类异常提交 */
        public Long durationMs;
    }

    /** 审核 */
    public static class Audit {
        /** 举报处理时可直接下架内容 */
        public Boolean takeDown;

        @NotBlank(message = "目标类型不能为空")
        public String targetType;

        @NotNull(message = "目标 ID 不能为空")
        public Long targetId;

        /** APPROVED / REJECTED */
        @NotBlank(message = "审核结果不能为空")
        public String result;

        @Size(max = 255, message = "审核意见过长")
        public String reason;
    }

    /** 管理端处理举报 */
    public static class ReportHandle {
        /** HANDLED / REJECTED */
        @NotBlank(message = "处理结果不能为空")
        public String status;

        @Size(max = 255, message = "处理说明过长")
        public String result;
    }

    /** 管理端用户管理 */
    public static class AdminUser {
        /** USER / MODERATOR / ADMIN */
        public String role;

        /** 1 正常 / 0 封禁 */
        public Integer status;

        /** 积分调整（正负均可） */
        public Integer pointsDelta;

        @Size(max = 255, message = "说明过长")
        public String reason;
        /** 版主负责的板块 ID，逗号分隔；留空表示全站版主 */
        @Size(max = 255, message = "板块配置过长")
        public String moderatedCategories;
    }

    /** 商品维护 */
    public static class Product {
        /** 新增时为空，修改时必填 */
        public Long id;

        @NotBlank(message = "商品名不能为空")
        @Size(max = 100, message = "商品名过长")
        public String name;

        @Size(max = 500, message = "描述过长")
        public String description;

        @Size(max = 500, message = "图片地址过长")
        public String image;

        @NotNull(message = "价格不能为空")
        public Integer pricePoints;

        @NotNull(message = "库存不能为空")
        public Integer stock;

        @NotBlank(message = "商品类型不能为空")
        public String type;

        public Integer status;
    }

    /** 系统配置 */
    public static class Config {
        @NotBlank(message = "配置键不能为空")
        public String key;

        @NotBlank(message = "配置值不能为空")
        public String value;
    }

    /** 图片上传 */
    public static class Upload {
        @NotBlank(message = "图片内容不能为空")
        public String dataUrl;
    }
}
