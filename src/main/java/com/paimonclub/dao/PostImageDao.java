package com.paimonclub.dao;

import com.paimonclub.entity.PostImage;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 帖子图片 DAO。
 */
@ApplicationScoped
public class PostImageDao extends BaseDao<PostImage> {

    @Override
    protected Class<PostImage> entityClass() {
        return PostImage.class;
    }

    public List<PostImage> listByPost(Long postId) {
        return list("select i from PostImage i where i.postId = ?1 order by i.sort asc, i.id asc", postId);
    }

    public List<PostImage> listByPosts(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }
        return list("select i from PostImage i where i.postId in ?1 order by i.sort asc, i.id asc", (Object) postIds);
    }

    public int deleteByPost(Long postId) {
        return execute("delete from PostImage i where i.postId = ?1", postId);
    }

    /** 批量取帖子的图片列表：一次查询覆盖整页帖子 */
    public java.util.Map<Long, java.util.List<String>> mapUrlsByPost(java.util.Collection<Long> postIds) {
        java.util.Map<Long, java.util.List<String>> map = new java.util.HashMap<>();
        if (postIds == null || postIds.isEmpty()) {
            return map;
        }
        for (PostImage image : list("select i from PostImage i where i.postId in ?1 order by i.sort asc", postIds)) {
            map.computeIfAbsent(image.getPostId(), k -> new java.util.ArrayList<>()).add(image.getUrl());
        }
        return map;
    }
}
