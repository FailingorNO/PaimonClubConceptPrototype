package com.paimonclub.service;

import com.paimonclub.common.Constants;
import com.paimonclub.dao.DecorationDao;
import com.paimonclub.dao.FavoriteDao;
import com.paimonclub.dao.FollowDao;
import com.paimonclub.dao.FriendDao;
import com.paimonclub.dao.GameAccountDao;
import com.paimonclub.dao.LikeRecordDao;
import com.paimonclub.dao.NotificationDao;
import com.paimonclub.dao.PostCategoryDao;
import com.paimonclub.dao.PostDao;
import com.paimonclub.dao.PostImageDao;
import com.paimonclub.dao.UserDao;
import com.paimonclub.dao.UserDecorationDao;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.Comment;
import com.paimonclub.entity.Decoration;
import com.paimonclub.entity.GameAccount;
import com.paimonclub.entity.Post;
import com.paimonclub.entity.PostCategory;
import com.paimonclub.entity.PostImage;
import com.paimonclub.entity.User;
import com.paimonclub.entity.UserDecoration;
import com.paimonclub.util.DateUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 实体 → VO 映射器，统一填充作者、分类、图片、互动状态等派生字段。
 */
@ApplicationScoped
public class VoMapper {

    @Inject
    UserDao userDao;
    @Inject PostDao postDao;
    @Inject PostCategoryDao categoryDao;
    @Inject PostImageDao imageDao;
    @Inject LikeRecordDao likeDao;
    @Inject FavoriteDao favoriteDao;
    @Inject FollowDao followDao;
    @Inject FriendDao friendDao;
    @Inject NotificationDao notificationDao;
    @Inject UserDecorationDao userDecorationDao;
    @Inject DecorationDao decorationDao;
    @Inject GameAccountDao gameAccountDao;

    public Vos.User toUser(User user) {
        return toUser(user, null);
    }

    public Vos.User toUser(User user, Long viewerId) {
        if (user == null) {
            return null;
        }
        Vos.User vo = new Vos.User();
        vo.id = user.getId();
        vo.username = user.getUsername();
        vo.nickname = user.getNickname();
        vo.avatarUrl = user.getAvatarUrl() == null ? Constants.DEFAULT_AVATAR : user.getAvatarUrl();
        vo.role = user.getRole();
        vo.points = user.getPoints();
        vo.bio = user.getBio();
        vo.status = user.getStatus();
        vo.self = viewerId != null && viewerId.equals(user.getId());
        // 隐私字段只对本人可见：GET /api/users/{id} 是公开接口，不能把邮箱、手机号发给任何访问者
        vo.email = vo.self ? user.getEmail() : null;
        vo.phone = vo.self ? user.getPhone() : null;
        vo.createdAt = DateUtil.format(user.getCreatedAt());
        vo.followers = followDao.countFollowers(user.getId());
        vo.followees = followDao.countFollowees(user.getId());
        vo.postCount = postDao.countByUser(user.getId());
        if (viewerId != null && !vo.self) {
            vo.followed = followDao.exists(viewerId, user.getId());
            var friend = friendDao.findBetween(viewerId, user.getId());
            vo.friend = friend != null && Constants.FRIEND_ACCEPTED.equals(friend.getStatus());
        }
        GameAccount account = gameAccountDao.findPrimary(user.getId());
        if (account != null) {
            vo.gameUid = account.getGameUid();
            vo.adventureRank = account.getAdventureRank();
        }
        // 佩戴中的装扮
        List<UserDecoration> wearing = userDecorationDao.listWearing(user.getId());
        Map<Long, Decoration> cache = new HashMap<>();
        for (UserDecoration item : wearing) {
            Decoration decoration = cache.computeIfAbsent(item.getDecorationId(), decorationDao::find);
            if (decoration == null) {
                continue;
            }
            if ("TITLE".equals(decoration.getType())) {
                vo.wearingTitle = decoration.getName();
            } else if ("AVATAR_FRAME".equals(decoration.getType())) {
                vo.wearingFrame = decoration.getResourceUrl();
            }
        }
        return vo;
    }

    public Vos.Post toPost(Post post, Long viewerId) {
        if (post == null) {
            return null;
        }
        Vos.Post vo = new Vos.Post();
        vo.id = post.getId();
        vo.title = post.getTitle();
        vo.content = post.getContent();
        vo.summary = summary(post.getContent(), 120);
        vo.categoryId = post.getCategoryId();
        PostCategory category = categoryDao.find(post.getCategoryId());
        vo.categoryName = category == null ? "未分类" : category.getName();
        vo.userId = post.getUserId();
        User author = userDao.find(post.getUserId());
        vo.authorName = author == null ? "已注销用户" : author.getNickname();
        vo.authorAvatar = author == null || author.getAvatarUrl() == null ? Constants.DEFAULT_AVATAR : author.getAvatarUrl();
        vo.status = post.getStatus();
        vo.likeCount = post.getLikeCount();
        vo.commentCount = post.getCommentCount();
        vo.viewCount = post.getViewCount();
        vo.favoriteCount = post.getFavoriteCount();
        vo.top = post.getTop();
        vo.tags = post.getTags();
        vo.rejectReason = post.getRejectReason();
        vo.createdAt = DateUtil.format(post.getCreatedAt());
        for (PostImage image : imageDao.listByPost(post.getId())) {
            vo.images.add(image.getUrl());
        }
        if (viewerId != null) {
            vo.liked = likeDao.exists(viewerId, Constants.TARGET_POST, post.getId());
            vo.favorited = favoriteDao.exists(viewerId, post.getId());
        }
        return vo;
    }

    /**
     * 列表场景的批量映射。
     *
     * <p>原来每条帖子都要单独查分类、作者、图片、点赞、收藏（20 条帖子就是 100 次 SQL），
     * 这里先按整页收集 ID，再各用一次 IN 查询取回来，SQL 数量从 O(N) 降到常数级。</p>
     */
    public List<Vos.Post> toPosts(List<Post> posts, Long viewerId) {
        List<Vos.Post> result = new ArrayList<>();
        if (posts == null || posts.isEmpty()) {
            return result;
        }
        Set<Long> categoryIds = new HashSet<>();
        Set<Long> authorIds = new HashSet<>();
        List<Long> postIds = new ArrayList<>(posts.size());
        for (Post post : posts) {
            postIds.add(post.getId());
            if (post.getCategoryId() != null) {
                categoryIds.add(post.getCategoryId());
            }
            if (post.getUserId() != null) {
                authorIds.add(post.getUserId());
            }
        }
        Map<Long, PostCategory> categories = categoryDao.mapByIds(categoryIds);
        Map<Long, User> authors = userDao.mapByIds(authorIds);
        Map<Long, List<String>> images = imageDao.mapUrlsByPost(postIds);
        Set<Long> liked = viewerId == null ? Collections.emptySet()
                : likeDao.likedTargetIds(viewerId, Constants.TARGET_POST, postIds);
        Set<Long> favorited = viewerId == null ? Collections.emptySet()
                : favoriteDao.favoritedPostIds(viewerId, postIds);

        for (Post post : posts) {
            Vos.Post vo = basePost(post, categories, authors, images);
            if (viewerId != null) {
                vo.liked = liked.contains(post.getId());
                vo.favorited = favorited.contains(post.getId());
            }
            result.add(vo);
        }
        return result;
    }

    /** 组装帖子基础字段，不再查库 */
    private Vos.Post basePost(Post post, Map<Long, PostCategory> categories,
                              Map<Long, User> authors, Map<Long, List<String>> images) {
        Vos.Post vo = new Vos.Post();
        vo.id = post.getId();
        vo.title = post.getTitle();
        vo.content = post.getContent();
        vo.summary = summary(post.getContent(), 120);
        vo.categoryId = post.getCategoryId();
        PostCategory category = categories.get(post.getCategoryId());
        vo.categoryName = category == null ? "未分类" : category.getName();
        vo.userId = post.getUserId();
        User author = authors.get(post.getUserId());
        vo.authorName = author == null ? "已注销用户" : author.getNickname();
        vo.authorAvatar = author == null || author.getAvatarUrl() == null
                ? Constants.DEFAULT_AVATAR : author.getAvatarUrl();
        vo.status = post.getStatus();
        vo.likeCount = post.getLikeCount();
        vo.commentCount = post.getCommentCount();
        vo.viewCount = post.getViewCount();
        vo.favoriteCount = post.getFavoriteCount();
        vo.top = post.getTop();
        vo.tags = post.getTags();
        vo.rejectReason = post.getRejectReason();
        vo.createdAt = DateUtil.format(post.getCreatedAt());
        List<String> urls = images.get(post.getId());
        if (urls != null) {
            vo.images.addAll(urls);
        }
        return vo;
    }

    public Vos.Comment toComment(Comment comment, Long viewerId) {
        Vos.Comment vo = new Vos.Comment();
        vo.id = comment.getId();
        vo.postId = comment.getPostId();
        vo.userId = comment.getUserId();
        User user = userDao.find(comment.getUserId());
        vo.userName = user == null ? "已注销用户" : user.getNickname();
        vo.userAvatar = user == null || user.getAvatarUrl() == null ? Constants.DEFAULT_AVATAR : user.getAvatarUrl();
        vo.parentId = comment.getParentId();
        vo.rootId = comment.getRootId();
        vo.content = comment.getContent();
        vo.likeCount = comment.getLikeCount();
        vo.createdAt = DateUtil.format(comment.getCreatedAt());
        if (viewerId != null) {
            vo.liked = likeDao.exists(viewerId, Constants.TARGET_COMMENT, comment.getId());
        }
        return vo;
    }

    public String summary(String content, int max) {
        if (content == null) {
            return "";
        }
        String plain = content.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        return plain.length() <= max ? plain : plain.substring(0, max) + "...";
    }

    public long unreadNotifications(Long userId) {
        return userId == null ? 0 : notificationDao.countUnread(userId);
    }
}
