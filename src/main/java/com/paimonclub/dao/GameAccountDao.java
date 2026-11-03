package com.paimonclub.dao;

import com.paimonclub.entity.GameAccount;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 游戏账号绑定 DAO。
 */
@ApplicationScoped
public class GameAccountDao extends BaseDao<GameAccount> {

    @Override
    protected Class<GameAccount> entityClass() {
        return GameAccount.class;
    }

    public List<GameAccount> listByUser(Long userId) {
        return list("select g from GameAccount g where g.userId = ?1 order by g.id asc", userId);
    }

    public GameAccount findPrimary(Long userId) {
        List<GameAccount> list = listByUser(userId);
        return list.isEmpty() ? null : list.get(0);
    }

    public GameAccount findByUid(String gameUid) {
        return single("select g from GameAccount g where g.gameUid = ?1", gameUid);
    }
}
