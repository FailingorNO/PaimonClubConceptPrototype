package com.paimonclub.dao;

import com.paimonclub.entity.MiniGame;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 小游戏 DAO。
 */
@ApplicationScoped
public class MiniGameDao extends BaseDao<MiniGame> {

    @Override
    protected Class<MiniGame> entityClass() {
        return MiniGame.class;
    }

    public List<MiniGame> listEnabled() {
        return list("select g from MiniGame g where g.status = 1 order by g.id asc");
    }

    public List<MiniGame> listAll() {
        return list("select g from MiniGame g order by g.id asc");
    }

    public MiniGame findByName(String name) {
        return single("select g from MiniGame g where g.name = ?1", name);
    }
}
