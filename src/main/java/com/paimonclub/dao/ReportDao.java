package com.paimonclub.dao;

import com.paimonclub.entity.Report;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 举报 DAO。
 */
@ApplicationScoped
public class ReportDao extends BaseDao<Report> {

    @Override
    protected Class<Report> entityClass() {
        return Report.class;
    }

    public List<Report> page(String status, int first, int max) {
        if (status == null || status.isBlank()) {
            return listPage("select r from Report r order by r.id desc", first, max);
        }
        return listPage("select r from Report r where r.status = ?1 order by r.id desc", first, max, status);
    }

    public long countPage(String status) {
        if (status == null || status.isBlank()) {
            return count("select count(r) from Report r");
        }
        return count("select count(r) from Report r where r.status = ?1", status);
    }

    public long countByStatus(String status) {
        return count("select count(r) from Report r where r.status = ?1", status);
    }
}
