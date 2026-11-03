package com.paimonclub.dao;

import com.paimonclub.entity.PostCategory;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 帖子分类 DAO。
 */
@ApplicationScoped
public class PostCategoryDao extends BaseDao<PostCategory> {

    @Override
    protected Class<PostCategory> entityClass() {
        return PostCategory.class;
    }

    public List<PostCategory> listSorted() {
        return list("select c from PostCategory c order by c.sort asc, c.id asc");
    }

    public PostCategory findByName(String name) {
        return single("select c from PostCategory c where c.name = ?1", name);
    }

    /** 批量按 ID 取分类 */
    public java.util.Map<Long, PostCategory> mapByIds(java.util.Collection<Long> ids) {
        java.util.Map<Long, PostCategory> map = new java.util.HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        for (PostCategory category : list("select c from PostCategory c where c.id in ?1", ids)) {
            map.put(category.getId(), category);
        }
        return map;
    }
}
