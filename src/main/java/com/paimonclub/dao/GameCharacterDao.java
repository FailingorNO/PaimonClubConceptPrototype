package com.paimonclub.dao;

import com.paimonclub.entity.GameCharacter;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 玩家录入角色 DAO。
 */
@ApplicationScoped
public class GameCharacterDao extends BaseDao<GameCharacter> {

    @Override
    protected Class<GameCharacter> entityClass() {
        return GameCharacter.class;
    }

    public List<GameCharacter> listByUser(Long userId) {
        return list("select c from GameCharacter c where c.userId = ?1 order by c.id asc", userId);
    }

    public void removeByUser(Long userId) {
        execute("delete from GameCharacter c where c.userId = ?1", userId);
    }
}
