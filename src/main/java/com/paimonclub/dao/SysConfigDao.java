package com.paimonclub.dao;

import com.paimonclub.entity.SysConfig;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 系统配置 DAO。
 */
@ApplicationScoped
public class SysConfigDao extends BaseDao<SysConfig> {

    @Override
    protected Class<SysConfig> entityClass() {
        return SysConfig.class;
    }

    public SysConfig findByKey(String key) {
        return single("select c from SysConfig c where c.cfgKey = ?1", key);
    }

    public String value(String key, String defaultValue) {
        SysConfig config = findByKey(key);
        return config == null || config.getCfgValue() == null ? defaultValue : config.getCfgValue();
    }

    public List<SysConfig> listAll() {
        return list("select c from SysConfig c order by c.id asc");
    }

    public void put(String key, String value, String remark) {
        SysConfig config = findByKey(key);
        if (config == null) {
            config = new SysConfig();
            config.setCfgKey(key);
            config.setCfgValue(value);
            config.setRemark(remark);
            persist(config);
        } else {
            config.setCfgValue(value);
            update(config);
        }
    }
}
