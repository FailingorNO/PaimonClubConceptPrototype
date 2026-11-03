package com.paimonclub.dao;

import com.paimonclub.entity.Share;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * 分享 DAO。
 */
@ApplicationScoped
public class ShareDao extends BaseDao<Share> {

    @Override
    protected Class<Share> entityClass() {
        return Share.class;
    }

    public Share findByCode(String shareCode) {
        return single("select s from Share s where s.shareCode = ?1", shareCode);
    }

    public long countByPost(Long postId) {
        return count("select count(s) from Share s where s.postId = ?1", postId);
    }

    /** 同一用户对同一帖子的分享记录（复用短链） */
    public Share findByPostAndUser(Long postId, Long userId) {
        if (userId == null) {
            return single("select s from Share s where s.postId = ?1 and s.userId is null order by s.id desc", postId);
        }
        return single("select s from Share s where s.postId = ?1 and s.userId = ?2 order by s.id desc", postId, userId);
    }

    /** 帖子被彻底删除时清理分享记录 */
    public int deleteByPost(Long postId) {
        return executeAndClear("delete from Share s where s.postId = ?1", postId);
    }
}
