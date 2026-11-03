package com.paimonclub.dao;

import com.paimonclub.entity.UserDecoration;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 用户装扮 DAO。
 */
@ApplicationScoped
public class UserDecorationDao extends BaseDao<UserDecoration> {

    @Override
    protected Class<UserDecoration> entityClass() {
        return UserDecoration.class;
    }

    public List<UserDecoration> listByUser(Long userId) {
        return list("select d from UserDecoration d where d.userId = ?1 order by d.id desc", userId);
    }

    public List<UserDecoration> listWearing(Long userId) {
        return list("select d from UserDecoration d where d.userId = ?1 and d.isWearing = true", userId);
    }

    public boolean exists(Long userId, Long decorationId) {
        return count("select count(d) from UserDecoration d where d.userId = ?1 and d.decorationId = ?2",
                userId, decorationId) > 0;
    }

    public int unwearAll(Long userId) {
        return execute("update UserDecoration d set d.isWearing = false where d.userId = ?1", userId);
    }
}
