package com.paimonclub.dao;

import com.paimonclub.entity.AuditRecord;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 审核记录 DAO。
 */
@ApplicationScoped
public class AuditRecordDao extends BaseDao<AuditRecord> {

    @Override
    protected Class<AuditRecord> entityClass() {
        return AuditRecord.class;
    }

    public List<AuditRecord> pageAll(int first, int max) {
        return listPage("select a from AuditRecord a order by a.id desc", first, max);
    }

    public long countAll() {
        return count("select count(a) from AuditRecord a");
    }

    public List<AuditRecord> listByTarget(String targetType, Long targetId) {
        return list("select a from AuditRecord a where a.targetType = ?1 and a.targetId = ?2 order by a.id desc",
                targetType, targetId);
    }
}
