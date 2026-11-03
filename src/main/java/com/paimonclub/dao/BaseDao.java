package com.paimonclub.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;

/**
 * DAO 基类：封装 EntityManager 常用操作，子类只写业务查询。
 */
public abstract class BaseDao<T> {

    @PersistenceContext(unitName = "gcPU")
    protected EntityManager em;

    protected abstract Class<T> entityClass();

    public T find(Long id) {
        return id == null ? null : em.find(entityClass(), id);
    }

    public T findRequired(Long id) {
        T entity = find(id);
        if (entity == null) {
            throw com.paimonclub.common.BusinessException.notFound("数据不存在：" + id);
        }
        return entity;
    }

    public T persist(T entity) {
        em.persist(entity);
        em.flush();
        return entity;
    }

    public T update(T entity) {
        T merged = em.merge(entity);
        em.flush();
        return merged;
    }

    public void remove(T entity) {
        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }

    public void removeById(Long id) {
        T entity = find(id);
        if (entity != null) {
            remove(entity);
        }
    }

    /** 全表行数，用于基础字典判空。 */
    public long countAll() {
        return count("SELECT COUNT(e) FROM " + entityClass().getSimpleName() + " e");
    }

    protected List<T> list(String jpql, Object... params) {
        TypedQuery<T> query = em.createQuery(jpql, entityClass());
        bind(query, params);
        return query.getResultList();
    }

    protected List<T> listPage(String jpql, int first, int max, Object... params) {
        TypedQuery<T> query = em.createQuery(jpql, entityClass());
        bind(query, params);
        return query.setFirstResult(first).setMaxResults(max).getResultList();
    }

    protected T single(String jpql, Object... params) {
        List<T> result = list(jpql, params);
        return result.isEmpty() ? null : result.get(0);
    }

    protected long count(String jpql, Object... params) {
        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        bind(query, params);
        Long value = query.getSingleResult();
        return value == null ? 0 : value;
    }

    /**
     * 执行批量 update/delete 并清空持久化上下文。
     * 原子扣减之后必须重新查库，否则拿到的是被一级缓存污染的旧实体。
     */
    protected int executeAndClear(String jpql, Object... params) {
        int affected = execute(jpql, params);
        em.clear();
        return affected;
    }

    protected int execute(String jpql, Object... params) {
        jakarta.persistence.Query query = em.createQuery(jpql);
        bind(query, params);
        return query.executeUpdate();
    }

    protected <R> List<R> listOfPage(Class<R> type, String jpql, int first, int max, Object... params) {
        TypedQuery<R> query = em.createQuery(jpql, type);
        bind(query, params);
        if (max > 0) {
            query.setFirstResult(first).setMaxResults(max);
        }
        return query.getResultList();
    }

    protected <R> List<R> listOf(Class<R> type, String jpql, Object... params) {
        TypedQuery<R> query = em.createQuery(jpql, type);
        bind(query, params);
        return query.getResultList();
    }

    private void bind(jakarta.persistence.Query query, Object... params) {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
    }
}
