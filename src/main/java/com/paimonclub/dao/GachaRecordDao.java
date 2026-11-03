package com.paimonclub.dao;

import com.paimonclub.entity.GachaRecord;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 抽卡记录 DAO。
 */
@ApplicationScoped
public class GachaRecordDao extends BaseDao<GachaRecord> {

    @Override
    protected Class<GachaRecord> entityClass() {
        return GachaRecord.class;
    }

    public List<GachaRecord> listByUser(Long userId, String poolName, int first, int max) {
        if (poolName == null || poolName.isBlank()) {
            return listPage("select g from GachaRecord g where g.userId = ?1 order by g.pullTime desc", first, max, userId);
        }
        return listPage("select g from GachaRecord g where g.userId = ?1 and g.poolName = ?2 order by g.pullTime desc",
                first, max, userId, poolName);
    }

    public long countByUser(Long userId) {
        return count("select count(g) from GachaRecord g where g.userId = ?1", userId);
    }

    public long countByUserAndPool(Long userId, String poolName) {
        if (poolName == null || poolName.isBlank()) {
            return countByUser(userId);
        }
        return count("select count(g) from GachaRecord g where g.userId = ?1 and g.poolName = ?2", userId, poolName);
    }

    public long countAll() {
        return count("select count(g) from GachaRecord g");
    }

    /** 全量按时间升序，用于保底/欧非曲线计算 */
    public List<GachaRecord> listAllAsc(Long userId, String poolName) {
        if (poolName == null || poolName.isBlank()) {
            return list("select g from GachaRecord g where g.userId = ?1 order by g.pullTime asc, g.id asc", userId);
        }
        return list("select g from GachaRecord g where g.userId = ?1 and g.poolName = ?2 order by g.pullTime asc, g.id asc",
                userId, poolName);
    }

    public long countByRarity(Long userId, int rarity) {
        return count("select count(g) from GachaRecord g where g.userId = ?1 and g.rarity = ?2", userId, rarity);
    }

    public List<String> listPools(Long userId) {
        return listOf(String.class, "select distinct g.poolName from GachaRecord g where g.userId = ?1", userId);
    }

    public int deleteByUser(Long userId) {
        return execute("delete from GachaRecord g where g.userId = ?1", userId);
    }
}
