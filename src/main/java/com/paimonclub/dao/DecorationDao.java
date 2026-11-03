package com.paimonclub.dao;

import com.paimonclub.entity.Decoration;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 装扮 DAO。
 */
@ApplicationScoped
public class DecorationDao extends BaseDao<Decoration> {

    @Override
    protected Class<Decoration> entityClass() {
        return Decoration.class;
    }

    public List<Decoration> listByStatus(Integer status) {
        return list("select d from Decoration d where d.status = ?1 order by d.price asc", status);
    }

    public List<Decoration> listByType(String type) {
        return list("select d from Decoration d where d.type = ?1 and d.status = 1 order by d.price asc", type);
    }
}
