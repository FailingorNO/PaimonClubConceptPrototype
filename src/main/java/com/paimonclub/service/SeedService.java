package com.paimonclub.service;

import com.paimonclub.common.Constants;
import com.paimonclub.dao.DecorationDao;
import com.paimonclub.dao.MiniGameDao;
import com.paimonclub.dao.PostCategoryDao;
import com.paimonclub.dao.ProductDao;
import com.paimonclub.dao.SysConfigDao;
import com.paimonclub.dao.UserDao;
import com.paimonclub.entity.Decoration;
import com.paimonclub.entity.MiniGame;
import com.paimonclub.entity.PostCategory;
import com.paimonclub.entity.Product;
import com.paimonclub.entity.User;
import com.paimonclub.util.PasswordUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * 系统基础数据初始化。
 *
 * <p>只保证两件事：
 * <ol>
 *   <li>最高权限管理员账号 {@code admin} 固定且唯一存在；</li>
 *   <li>各功能模块运行所必需的基础字典（帖子板块、商城商品、装扮、小游戏、系统配置）不为空。</li>
 * </ol>
 *
 * <p>这里<b>不写入任何示例内容</b>：没有示例帖子、示例评论、示例用户、示例订单、
 * 示例抽卡记录。所有用户互动数据都必须由真实用户在页面上操作后自然产生。
 */
@ApplicationScoped
public class SeedService {

    private static final Logger LOG = Logger.getLogger(SeedService.class.getName());

    /** 系统最高权限账号，唯一且固定。 */
    private static final String ADMIN_USERNAME = "admin";
    /** 本地开发兜底密码；生产环境必须通过外部配置覆盖。 */
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String ADMIN_NICKNAME = "系统管理员";

    @Inject UserDao userDao;
    @Inject PostCategoryDao categoryDao;
    @Inject ProductDao productDao;
    @Inject DecorationDao decorationDao;
    @Inject MiniGameDao miniGameDao;
    @Inject SysConfigDao configDao;

    @Transactional
    public void seed() {
        ensureAdmin();
        if (!dictEnabled()) {
            LOG.info("已关闭基础字典初始化（PAIMON_SEED_DICT=false），仅保证管理员账号存在");
            return;
        }
        ensureCategories();
        ensureProducts();
        ensureDecorations();
        ensureMiniGames();
        ensureMiniGameKeys();
        ensureConfigs();
        LOG.info("系统基础数据就绪（管理员 " + ADMIN_USERNAME + "，其余内容由用户产生）");
    }

    /**
     * 基础字典（板块/商品/装扮/小游戏/系统配置）写入开关。
     *
     * <p>默认开启：这些是功能运行所必需的字典，缺失会导致发帖、兑换、游戏页面不可用。
     * 需要接入已有数据库、完全由 DBA 维护字典时，用 {@code -Dpaimon.seed.dict=false}
     * 或环境变量 {@code PAIMON_SEED_DICT=false} 关闭。
     */
    private static boolean dictEnabled() {
        String raw = firstNonBlank(
                System.getProperty("paimon.seed.dict"),
                System.getenv("PAIMON_SEED_DICT"));
        if (raw == null) {
            return true;
        }
        String value = raw.trim();
        return !("false".equalsIgnoreCase(value) || "0".equals(value) || "no".equalsIgnoreCase(value));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /** 最高权限账号：不存在才创建，已存在则完全不动。 */
    private void ensureAdmin() {
        if (userDao.existsByUsername(ADMIN_USERNAME)) {
            return;
        }
        User admin = new User();
        admin.setUsername(ADMIN_USERNAME);
        admin.setPasswordHash(PasswordUtil.hash(resolveAdminPassword()));
        admin.setNickname(ADMIN_NICKNAME);
        admin.setAvatarUrl(Constants.DEFAULT_AVATAR);
        admin.setRole(Constants.ROLE_ADMIN);
        admin.setStatus(Constants.USER_NORMAL);
        admin.setPoints(0);
        admin.setBio("");
        admin.setCreatedAt(LocalDateTime.now());
        userDao.persist(admin);
        LOG.info("已创建最高权限账号 " + ADMIN_USERNAME);
    }

    /** 帖子板块：发帖必需的基础字典。 */
    private void ensureCategories() {
        if (categoryDao.countAll() > 0) {
            return;
        }
        category("攻略分享", 1, "深渊、探索、材料路线等硬核攻略", "book");
        category("同人创作", 2, "绘画、小说、cosplay 作品", "palette");
        category("抽卡交流", 3, "晒卡、玄学、保底记录", "star");
        category("交友组队", 4, "找队友、找同好、联机开黑", "users");
        category("闲聊灌水", 5, "旅行者日常闲聊", "coffee");
    }

    /** 积分商城商品：兑换功能必需。 */
    private void ensureProducts() {
        if (productDao.countAll() > 0) {
            return;
        }
        product("派蒙金属徽章", "限定版派蒙金属徽章，直径 4cm，附赠亚克力立牌。", 300, 20, "PHYSICAL");
        product("深渊挑战纪念挂件", "通关 12 层纪念挂件，限量发售。", 500, 10, "PHYSICAL");
        product("月卡体验券（7 天）", "虚拟道具，兑换后 7 天内每日领取原石。", 800, 50, "VIRTUAL");
        product("社区专属头衔「资深旅行者」", "虚拟头衔，佩戴后展示在昵称旁。", 200, 100, "VIRTUAL");
    }

    /** 装扮：装扮功能必需。 */
    private void ensureDecorations() {
        if (decorationDao.countAll() > 0) {
            return;
        }
        decoration("金色派蒙头像框", "AVATAR_FRAME", "static/img/frame-gold.svg", 300);
        decoration("深渊星辉头像框", "AVATAR_FRAME", "static/img/frame-abyss.svg", 500);
        decoration("头衔·欧皇", "TITLE", null, 400);
        decoration("头衔·资深旅行者", "TITLE", null, 200);
    }

    /** 小游戏：游戏模块必需。 */
    private void ensureMiniGames() {
        if (miniGameDao.countAll() > 0) {
            return;
        }
        miniGame("贪吃蛇·元素版", "games.html", 5, "方向键控制派蒙吃元素球，撞墙即结束", "snake");
        miniGame("2048·原石合成", "games.html", 5, "方向键合成相同数字，冲上 2048", "2048");
        miniGame("猜数字·命运的骰子", "games.html", 3, "7 次机会猜中 1-100 的数字", "guess");
        miniGame("抽卡模拟器", "games.html", 2, "模拟十连，看看你的欧气值", "gacha");
    }

    /** 系统配置：审核模式与敏感词开关。 */
    private void ensureConfigs() {
        if (configDao.findByKey(Constants.CFG_AUDIT_MODE) == null) {
            configDao.put(Constants.CFG_AUDIT_MODE, Constants.CFG_AUDIT_MODE_POST,
                    "审核模式：PRE=先审后发，POST=先发后审");
        }
        if (configDao.findByKey(Constants.CFG_SENSITIVE_ENABLED) == null) {
            configDao.put(Constants.CFG_SENSITIVE_ENABLED, "true", "是否开启敏感词过滤");
        }
    }

    /**
     * 管理员初始密码：优先取外部配置，生产环境缺配置时生成一次性随机密码。
     *
     * <p>不要在公网环境使用 admin/admin123：这类弱口令会在部署后几分钟内被撞库脚本命中。
     */
    private String resolveAdminPassword() {
        String configured = firstNonBlank(
                System.getProperty("paimon.admin.password"),
                System.getenv("PAIMON_ADMIN_PASSWORD"));
        if (configured != null) {
            if (configured.trim().length() < 8) {
                throw new IllegalStateException("管理员初始密码长度不能少于 8 位");
            }
            return configured.trim();
        }
        if (com.paimonclub.common.JwtUtil.isProduction()) {
            String generated = randomPassword();
            LOG.warning("生产环境未配置 PAIMON_ADMIN_PASSWORD，已为 " + ADMIN_USERNAME
                    + " 生成一次性随机密码：" + generated + "（请登录后立即修改）");
            return generated;
        }
        LOG.warning("本地开发环境使用默认管理员密码 " + ADMIN_PASSWORD + "，上线前必须通过 PAIMON_ADMIN_PASSWORD 覆盖");
        return ADMIN_PASSWORD;
    }

    /** 生成 16 位随机密码（大小写字母 + 数字），避免部署即弱口令 */
    private String randomPassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private PostCategory category(String name, int sort, String description, String icon) {
        PostCategory category = new PostCategory();
        category.setName(name);
        category.setSort(sort);
        category.setDescription(description);
        category.setIcon(icon);
        categoryDao.persist(category);
        return category;
    }

    private void product(String name, String description, int price, int stock, String type) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPricePoints(price);
        product.setStock(stock);
        product.setType(type);
        product.setStatus(1);
        product.setCreatedAt(LocalDateTime.now());
        productDao.persist(product);
    }

    private void decoration(String name, String type, String resourceUrl, int price) {
        Decoration decoration = new Decoration();
        decoration.setName(name);
        decoration.setType(type);
        decoration.setResourceUrl(resourceUrl);
        decoration.setPrice(price);
        decoration.setStatus(1);
        decorationDao.persist(decoration);
    }

    private void miniGame(String name, String path, int rewardPoints, String description, String gameKey) {
        MiniGame game = new MiniGame();
        game.setName(name);
        game.setPath(path);
        game.setRewardPoints(rewardPoints);
        game.setStatus(1);
        game.setDescription(description);
        game.setIcon("static/img/game.svg");
        game.setGameKey(gameKey);
        miniGameDao.persist(game);
    }

    /**
     * 老库升级：为已存在但缺少玩法标识的小游戏补齐 gameKey。
     * 只在 gameKey 为空时按名称推断一次，之后以数据库为准，不会覆盖管理员的手工配置。
     */
    private void ensureMiniGameKeys() {
        for (MiniGame game : miniGameDao.listAll()) {
            if (game.getGameKey() != null && !game.getGameKey().isEmpty()) {
                continue;
            }
            String key = guessGameKey(game.getName());
            if (key == null) {
                continue;
            }
            game.setGameKey(key);
            miniGameDao.update(game);
            LOG.info("已为小游戏《" + game.getName() + "》补齐玩法标识 " + key);
        }
    }

    private String guessGameKey(String name) {
        if (name == null) {
            return null;
        }
        if (name.contains("蛇")) {
            return "snake";
        }
        if (name.contains("2048")) {
            return "2048";
        }
        if (name.contains("猜")) {
            return "guess";
        }
        if (name.contains("抽卡")) {
            return "gacha";
        }
        return null;
    }
}
