package com.paimonclub.dao;

import com.paimonclub.entity.GameScore;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 小游戏分数 DAO。
 */
@ApplicationScoped
public class GameScoreDao extends BaseDao<GameScore> {

    @Override
    protected Class<GameScore> entityClass() {
        return GameScore.class;
    }

    /** 排行榜：[userId, 最高分] */
    public List<Object[]> topByGame(Long gameId, int limit) {
        return listOfPage(Object[].class,
                "select s.userId, max(s.score) from GameScore s where s.gameId = ?1 group by s.userId order by max(s.score) desc",
                0, limit, gameId);
    }

    public Integer bestOf(Long userId, Long gameId) {
        List<Object> values = listOf(Object.class,
                "select max(s.score) from GameScore s where s.userId = ?1 and s.gameId = ?2", userId, gameId);
        if (values.isEmpty() || values.get(0) == null) {
            return null;
        }
        return ((Number) values.get(0)).intValue();
    }

    public List<GameScore> pageByGame(Long gameId, int first, int max) {
        return listPage("select s from GameScore s where s.gameId = ?1 order by s.id desc", first, max, gameId);
    }

    public long countAll() {
        return count("select count(s) from GameScore s");
    }
}
