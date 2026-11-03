package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.Constants;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.common.PageResult;
import com.paimonclub.dao.CommentDao;
import com.paimonclub.dao.FavoriteDao;
import com.paimonclub.dao.LikeRecordDao;
import com.paimonclub.dao.PostCategoryDao;
import com.paimonclub.dao.PostDao;
import com.paimonclub.dao.PostImageDao;
import com.paimonclub.dao.ShareDao;
import com.paimonclub.dao.SysConfigDao;
import com.paimonclub.dao.UserDao;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.Post;
import com.paimonclub.entity.User;
import com.paimonclub.entity.PostImage;
import com.paimonclub.util.DateUtil;
import com.paimonclub.util.FileStore;
import com.paimonclub.util.SensitiveWordUtil;
import com.paimonclub.util.XssUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 帖子服务：发布、编辑、删除、列表、详情、搜索、审核状态流转。
 */
@ApplicationScoped
public class PostService {

    @Inject
    PostDao postDao;
    @Inject PostImageDao imageDao;
    @Inject PostCategoryDao categoryDao;
    @Inject UserDao userDao;
    @Inject CurrentUser currentUser;
    @Inject VoMapper mapper;
    @Inject PointsService pointsService;
    @Inject NotificationService notificationService;
    @Inject SysConfigDao configDao;
    @Inject FileStore fileStore;
    @Inject LocalCacheService cache;
    @Inject CommentDao commentDao;
    @Inject LikeRecordDao likeRecordDao;
    @Inject FavoriteDao favoriteDao;
    @Inject ShareDao shareDao;
    @Inject ViewCounter viewCounter;

    /** 当前审核模式：PRE 先审后发 / POST 先发后审 */
    public String auditMode() {
        return configDao.value(Constants.CFG_AUDIT_MODE, Constants.CFG_AUDIT_MODE_POST);
    }

    /**
     * 这条帖子是否需要先审后发。
     *
     * <p>原来只有「全站 PRE」和「全站 POST」两档，要么所有人先审（社区没活跃度），
     * 要么所有人直接发（垃圾内容挡不住）。这里做分级：</p>
     * <ul>
     *   <li>全局配置为 PRE：全部先审；</li>
     *   <li>全局配置为 POST：注册不满 7 天，或已发布帖子少于 3 篇的新号，仍然先审；</li>
     *   <li>其余老用户直接发布，靠举报 + 事后下架兜底。</li>
     * </ul>
     */
    public boolean needPreAudit(Long userId) {
        if (Constants.CFG_AUDIT_MODE_PRE.equals(auditMode())) {
            return true;
        }
        User user = userDao.find(userId);
        if (user == null) {
            return true;
        }
        if (user.getCreatedAt() != null
                && user.getCreatedAt().isAfter(LocalDateTime.now().minusDays(Constants.NEW_USER_DAYS))) {
            return true;
        }
        return postDao.countPublishedByUser(userId) < Constants.NEW_USER_POST_THRESHOLD;
    }

    @Transactional
    public Vos.Post create(Reqs.Post req) {
        Long userId = currentUser.requireLogin();
        categoryDao.findRequired(req.categoryId);
        String title = XssUtil.sanitize(req.title.trim());
        String content = XssUtil.cleanContent(req.content.trim());
        checkSensitive(title + content);

        boolean preAudit = needPreAudit(userId);
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        post.setCategoryId(req.categoryId);
        post.setStatus(preAudit ? Constants.POST_PENDING : Constants.POST_PUBLISHED);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setFavoriteCount(0);
        post.setTop(false);
        post.setTags(req.tags == null ? null : XssUtil.sanitize(req.tags.trim()));
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        postDao.persist(post);

        saveImages(post.getId(), req.images);

        if (preAudit) {
            notificationService.push(userId, Constants.NOTIFY_AUDIT,
                    "你的帖子《" + post.getTitle() + "》已提交，等待版主审核", post.getId());
        } else {
            pointsService.award(userId, Constants.POINTS_POST_CREATE, Constants.POINT_TYPE_POST_CREATE,
                    post.getId(), "发布帖子");
        }
        cache.evictPrefix("posts:hot");
        return mapper.toPost(post, userId);
    }

    @Transactional
    public Vos.Post update(Long postId, Reqs.Post req) {
        Long userId = currentUser.requireLogin();
        Post post = postDao.findRequired(postId);
        if (!userId.equals(post.getUserId()) && !currentUser.isStaff()) {
            throw BusinessException.forbidden("只能编辑自己的帖子");
        }
        if (Constants.POST_PENDING.equals(post.getStatus())) {
            throw BusinessException.badRequest("审核中的帖子不能编辑");
        }
        categoryDao.findRequired(req.categoryId);
        String title = XssUtil.sanitize(req.title.trim());
        String content = XssUtil.cleanContent(req.content.trim());
        checkSensitive(title + content);

        post.setTitle(title);
        post.setContent(content);
        post.setCategoryId(req.categoryId);
        post.setTags(req.tags == null ? null : XssUtil.sanitize(req.tags.trim()));
        post.setUpdatedAt(LocalDateTime.now());
        if (needPreAudit(userId)) {
            post.setStatus(Constants.POST_PENDING);
        }
        postDao.update(post);

        if (req.images != null) {
            imageDao.deleteByPost(postId);
            saveImages(postId, req.images);
        }
        cache.evictPrefix("posts:hot");
        return mapper.toPost(post, userId);
    }

    /**
     * 删除帖子：软删除。
     * <p>
     * 只把 status 置为 DELETED，图片、点赞、收藏、评论等关联数据全部保留。
     * 这样帖子可以恢复、举报与审计记录不会指向不存在的行；
     * 真正的物理清理走 {@link #purge(Long)}。
     */
    @Transactional
    public void delete(Long postId) {
        Long userId = currentUser.requireLogin();
        Post post = postDao.findRequired(postId);
        if (!userId.equals(post.getUserId()) && !currentUser.isStaff()) {
            throw BusinessException.forbidden("无权删除该帖子");
        }
        if (Constants.POST_DELETED.equals(post.getStatus())) {
            return;
        }
        post.setStatus(Constants.POST_DELETED);
        post.setUpdatedAt(LocalDateTime.now());
        postDao.update(post);
        cache.evictPrefix("posts:hot");
        cache.evict("post:detail:" + postId);
    }

    /**
     * 管理员彻底删除：物理删除帖子，并清理点赞、收藏、分享、评论、图片等所有多态关联，
     * 避免留下指向不存在帖子的悬空记录。
     */
    @Transactional
    public void purge(Long postId) {
        currentUser.requireAdmin();
        Post post = postDao.findRequired(postId);
        commentDao.softDeleteByPost(postId);
        likeRecordDao.deleteByPost(postId);
        favoriteDao.deleteByPost(postId);
        shareDao.deleteByPost(postId);
        imageDao.deleteByPost(postId);
        postDao.remove(post);
        cache.evictPrefix("posts:hot");
        cache.evict("post:detail:" + postId);
    }

    @Transactional
    public Vos.Post detail(Long postId) {
        Post post = postDao.findRequired(postId);
        Long viewer = currentUser.getUserId();
        boolean owner = viewer != null && viewer.equals(post.getUserId());
        if (!Constants.POST_PUBLISHED.equals(post.getStatus()) && !owner && !currentUser.isStaff()) {
            throw BusinessException.notFound("帖子不存在或未通过审核");
        }
        // 只打内存计数，由 ViewCounter 每 20 秒批量落库，避免热门帖把行锁打满
        viewCounter.hit(postId);
        Post latest = postDao.findRequired(postId);
        Vos.Post vo = mapper.toPost(latest, viewer);
        int base = latest.getViewCount() == null ? 0 : latest.getViewCount();
        vo.viewCount = base + viewCounter.pendingOf(postId);
        return vo;
    }

    public PageResult<Vos.Post> page(Long categoryId, String keyword, String sort, int page, int size) {
        long total = postDao.countPage(categoryId, keyword, Constants.POST_PUBLISHED, null);
        List<Post> posts = postDao.page(categoryId, keyword, Constants.POST_PUBLISHED, null, sort,
                (page - 1) * size, size);
        return PageResult.of(total, page, size, mapper.toPosts(posts, currentUser.getUserId()));
    }

    public PageResult<Vos.Post> myPosts(String status, int page, int size) {
        Long userId = currentUser.requireLogin();
        long total = postDao.countPage(null, null, status, userId);
        List<Post> posts = postDao.page(null, null, status, userId, null, (page - 1) * size, size);
        return PageResult.of(total, page, size, mapper.toPosts(posts, userId));
    }

    public PageResult<Vos.Post> pending(int page, int size) {
        currentUser.requireStaff();
        List<Long> scope = currentUser.moderatedCategoryIds();
        if (!scope.isEmpty()) {
            // 板块版主：只看自己负责的板块，避免越权审到别人的板块
            long scopedTotal = postDao.countPendingInCategories(scope);
            List<Post> scoped = postDao.pagePendingInCategories(scope, (page - 1) * size, size);
            return PageResult.of(scopedTotal, page, size, mapper.toPosts(scoped, currentUser.getUserId()));
        }
        long total = postDao.countPage(null, null, Constants.POST_PENDING, null);
        List<Post> posts = postDao.page(null, null, Constants.POST_PENDING, null, null, (page - 1) * size, size);
        return PageResult.of(total, page, size, mapper.toPosts(posts, currentUser.getUserId()));
    }

    /** 热帖榜（带缓存） */
    @SuppressWarnings("unchecked")
    public List<Vos.Post> hot(int limit) {
        List<Vos.Post> cached = cache.get("posts:hot:" + limit, List.class);
        if (cached != null) {
            return cached;
        }
        List<Vos.Post> list = mapper.toPosts(postDao.hot(limit), currentUser.getUserId());
        cache.put("posts:hot:" + limit, new ArrayList<>(list), 60);
        return list;
    }

    public List<Vos.Post> related(Long categoryId, Long excludePostId, int limit) {
        List<Post> posts = postDao.page(categoryId, null, Constants.POST_PUBLISHED, null, null, 0, limit + 1);
        List<Vos.Post> result = new ArrayList<>();
        for (Post post : posts) {
            if (!post.getId().equals(excludePostId)) {
                result.add(mapper.toPost(post, currentUser.getUserId()));
            }
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private void saveImages(Long postId, List<String> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        int sort = 0;
        for (String image : images) {
            if (image == null || image.isBlank()) {
                continue;
            }
            String url = image.startsWith("data:") ? fileStore.saveDataUrl(image) : image.trim();
            PostImage entity = new PostImage();
            entity.setPostId(postId);
            entity.setUrl(url);
            entity.setSort(sort++);
            imageDao.persist(entity);
        }
    }

    private void checkSensitive(String text) {
        boolean enabled = Boolean.parseBoolean(configDao.value(Constants.CFG_SENSITIVE_ENABLED, "true"));
        if (!enabled) {
            return;
        }
        String hit = SensitiveWordUtil.firstHit(text);
        if (hit != null) {
            throw BusinessException.badRequest("内容包含敏感词，请修改后再提交");
        }
    }

    /** 供其它服务复用：帖子简要描述 */
    public String describe(Long postId) {
        Post post = postDao.find(postId);
        return post == null ? "帖子#" + postId : "《" + post.getTitle() + "》";
    }

    public String formatTime(LocalDateTime time) {
        return DateUtil.format(time);
    }
}
