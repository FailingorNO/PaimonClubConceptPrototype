package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.Constants;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.dao.CommentDao;
import com.paimonclub.dao.PostDao;
import com.paimonclub.dao.UserDao;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.Comment;
import com.paimonclub.entity.Post;
import com.paimonclub.entity.User;
import com.paimonclub.util.DateUtil;
import com.paimonclub.util.SensitiveWordUtil;
import com.paimonclub.util.XssUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评论服务：发表评论、楼中楼回复、评论树查询、删除。
 */
@ApplicationScoped
public class CommentService {

    @Inject
    CommentDao commentDao;
    @Inject PostDao postDao;
    @Inject UserDao userDao;
    @Inject CurrentUser currentUser;
    @Inject PointsService pointsService;
    @Inject NotificationService notificationService;
    @Inject VoMapper mapper;

    @Transactional
    public Vos.Comment add(Reqs.Comment req) {
        Long userId = currentUser.requireLogin();
        Post post = postDao.findRequired(req.postId);
        if (!Constants.POST_PUBLISHED.equals(post.getStatus())) {
            throw BusinessException.badRequest("帖子尚未发布，不能评论");
        }
        String content = XssUtil.sanitize(req.content.trim());
        if (SensitiveWordUtil.contains(content)) {
            throw BusinessException.badRequest("评论包含敏感词，请修改后再提交");
        }
        Long rootId = null;
        if (req.parentId != null) {
            Comment parent = commentDao.findRequired(req.parentId);
            rootId = parent.getRootId() == null ? parent.getId() : parent.getRootId();
        }
        Comment comment = new Comment();
        comment.setPostId(post.getId());
        comment.setUserId(userId);
        comment.setParentId(req.parentId);
        comment.setRootId(rootId);
        comment.setContent(content);
        comment.setLikeCount(0);
        comment.setStatus(1);
        comment.setCreatedAt(LocalDateTime.now());
        commentDao.persist(comment);
        postDao.increaseCommentCount(post.getId(), 1);

        pointsService.award(userId, Constants.POINTS_COMMENT, Constants.POINT_TYPE_COMMENT,
                post.getId(), "发表评论");
        if (!userId.equals(post.getUserId())) {
            User author = userDao.find(userId);
            notificationService.push(post.getUserId(), Constants.NOTIFY_COMMENT,
                    (author == null ? "有人" : author.getNickname()) + " 评论了你的帖子：" + content,
                    post.getId());
        }
        if (req.parentId != null) {
            Comment parent = commentDao.find(req.parentId);
            if (parent != null && !parent.getUserId().equals(userId)) {
                User author = userDao.find(userId);
                notificationService.push(parent.getUserId(), Constants.NOTIFY_COMMENT,
                        (author == null ? "有人" : author.getNickname()) + " 回复了你的评论：" + content,
                        post.getId());
            }
        }
        return mapper.toComment(comment, userId);
    }

    /** 查询评论树（两级） */
    public List<Vos.Comment> tree(Long postId) {
        Long viewer = currentUser.getUserId();
        List<Comment> all = commentDao.listByPost(postId);
        Map<Long, Vos.Comment> index = new HashMap<>();
        List<Vos.Comment> roots = new ArrayList<>();
        for (Comment comment : all) {
            Vos.Comment vo = mapper.toComment(comment, viewer);
            index.put(comment.getId(), vo);
        }
        for (Comment comment : all) {
            Vos.Comment vo = index.get(comment.getId());
            if (comment.getParentId() == null) {
                roots.add(vo);
            } else {
                Vos.Comment parent = index.get(comment.getParentId());
                if (parent != null) {
                    parent.children.add(vo);
                } else {
                    roots.add(vo);
                }
            }
        }
        return roots;
    }

    @Transactional
    public void delete(Long commentId) {
        Long userId = currentUser.requireLogin();
        Comment comment = commentDao.findRequired(commentId);
        if (!userId.equals(comment.getUserId()) && !currentUser.isStaff()) {
            throw BusinessException.forbidden("无权删除该评论");
        }
        // 软删除：只把 status 置 0，行保留下来便于审计与恢复；重复调用不会重复扣计数
        int removed = commentDao.softDeleteOne(commentId);
        if (removed == 0) {
            return;
        }
        int cascade = 0;
        if (comment.getParentId() == null) {
            // 根评论被删，整楼的回复一起隐藏
            cascade = commentDao.softDeleteReplies(commentId);
        }
        postDao.increaseCommentCount(comment.getPostId(), -(1 + cascade));
    }

    public long countAll() {
        return commentDao.countAll();
    }

    public String formatTime(LocalDateTime time) {
        return DateUtil.format(time);
    }
}
