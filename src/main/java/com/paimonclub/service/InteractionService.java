package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.Constants;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.dao.CommentDao;
import com.paimonclub.dao.FavoriteDao;
import com.paimonclub.dao.LikeRecordDao;
import com.paimonclub.dao.PostDao;
import com.paimonclub.dao.ShareDao;
import com.paimonclub.dao.UserDao;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.Comment;
import com.paimonclub.entity.Favorite;
import com.paimonclub.entity.LikeRecord;
import com.paimonclub.entity.Post;
import com.paimonclub.entity.Share;
import com.paimonclub.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 互动服务：点赞、收藏、分享（含短链与二维码）。
 */
@ApplicationScoped
public class InteractionService {

    @Inject LikeRecordDao likeDao;
    @Inject FavoriteDao favoriteDao;
    @Inject ShareDao shareDao;
    @Inject PostDao postDao;
    @Inject CommentDao commentDao;
    @Inject UserDao userDao;
    @Inject CurrentUser currentUser;
    @Inject PointsService pointsService;
    @Inject NotificationService notificationService;
    @Inject VoMapper mapper;

    /** 点赞/取消点赞，返回最新状态：true=已点赞 */
    @Transactional
    public boolean toggleLike(String targetType, Long targetId) {
        Long userId = currentUser.requireLogin();
        String type = targetType == null ? Constants.TARGET_POST : targetType.toUpperCase();
        boolean exists = likeDao.exists(userId, type, targetId);
        if (exists) {
            likeDao.deleteOne(userId, type, targetId);
            changeLikeCount(type, targetId, -1);
            return false;
        }
        LikeRecord like = new LikeRecord();
        like.setUserId(userId);
        like.setTargetType(type);
        like.setTargetId(targetId);
        like.setCreatedAt(LocalDateTime.now());
        likeDao.persist(like);
        changeLikeCount(type, targetId, 1);

        User actor = userDao.find(userId);
        String actorName = actor == null ? "有人" : actor.getNickname();
        pointsService.award(userId, Constants.POINTS_LIKE_GIVE, Constants.POINT_TYPE_LIKE_GIVE, targetId, "点赞");
        if (Constants.TARGET_POST.equals(type)) {
            Post post = postDao.findRequired(targetId);
            if (!userId.equals(post.getUserId())) {
                pointsService.award(post.getUserId(), Constants.POINTS_LIKE_RECEIVE,
                        Constants.POINT_TYPE_LIKE_RECEIVE, post.getId(), "帖子被点赞");
                notificationService.push(post.getUserId(), Constants.NOTIFY_LIKE,
                        actorName + " 赞了你的帖子《" + post.getTitle() + "》", post.getId());
            }
        } else if (Constants.TARGET_COMMENT.equals(type)) {
            Comment comment = commentDao.findRequired(targetId);
            if (!userId.equals(comment.getUserId())) {
                notificationService.push(comment.getUserId(), Constants.NOTIFY_LIKE,
                        actorName + " 赞了你的评论", comment.getPostId());
            }
        }
        return true;
    }

    private void changeLikeCount(String type, Long targetId, int delta) {
        if (Constants.TARGET_POST.equals(type)) {
            Post post = postDao.findRequired(targetId);
            post.setLikeCount(Math.max(0, (post.getLikeCount() == null ? 0 : post.getLikeCount()) + delta));
            postDao.update(post);
        } else if (Constants.TARGET_COMMENT.equals(type)) {
            Comment comment = commentDao.findRequired(targetId);
            comment.setLikeCount(Math.max(0, (comment.getLikeCount() == null ? 0 : comment.getLikeCount()) + delta));
            commentDao.update(comment);
        }
    }

    /** 收藏/取消收藏 */
    @Transactional
    public boolean toggleFavorite(Long postId) {
        Long userId = currentUser.requireLogin();
        Post post = postDao.findRequired(postId);
        boolean exists = favoriteDao.exists(userId, postId);
        if (exists) {
            favoriteDao.deleteOne(userId, postId);
            post.setFavoriteCount(Math.max(0, (post.getFavoriteCount() == null ? 0 : post.getFavoriteCount()) - 1));
            postDao.update(post);
            return false;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setPostId(postId);
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteDao.persist(favorite);
        post.setFavoriteCount((post.getFavoriteCount() == null ? 0 : post.getFavoriteCount()) + 1);
        postDao.update(post);

        if (!userId.equals(post.getUserId())) {
            User actor = userDao.find(userId);
            pointsService.award(post.getUserId(), Constants.POINTS_BE_FAVORITED,
                    Constants.POINT_TYPE_BE_FAVORITED, postId, "帖子被收藏");
            notificationService.push(post.getUserId(), Constants.NOTIFY_LIKE,
                    (actor == null ? "有人" : actor.getNickname()) + " 收藏了你的帖子《" + post.getTitle() + "》", postId);
        }
        return true;
    }

    /** 生成分享短链 */
    @Transactional
    public Vos.Share createShare(Long postId, String baseUrl) {
        Long userId = currentUser.getUserId();
        Post post = postDao.findRequired(postId);
        Share share = shareDao.findByPostAndUser(postId, userId);
        if (share == null) {
            share = new Share();
            share.setPostId(postId);
            share.setUserId(userId);
            share.setShareCode(randomCode());
            share.setCreatedAt(LocalDateTime.now());
            shareDao.persist(share);
        }
        Vos.Share vo = new Vos.Share();
        vo.postId = postId;
        vo.shareCode = share.getShareCode();
        String base = baseUrl == null || baseUrl.isBlank() ? "" : baseUrl.replaceAll("/+$", "");
        vo.url = base + "/post-detail.html?share=" + share.getShareCode();
        vo.qrCodeUrl = base + "/api/posts/share/" + share.getShareCode() + "/qrcode";
        return vo;
    }

    /** 通过分享码打开帖子（计入浏览） */
    @Transactional
    public Vos.Post byShareCode(String shareCode) {
        Share share = shareDao.findByCode(shareCode);
        if (share == null) {
            throw BusinessException.notFound("分享链接已失效");
        }
        Post post = postDao.findRequired(share.getPostId());
        if (!Constants.POST_PUBLISHED.equals(post.getStatus())) {
            throw BusinessException.notFound("帖子已不可见");
        }
        postDao.incrementView(post.getId());
        return mapper.toPost(postDao.findRequired(post.getId()), currentUser.getUserId());
    }

    private String randomCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
