package com.paimonclub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.io.Serializable;

/**
 * 小游戏（t_mini_game）
 */
@Entity
@Table(name = "t_mini_game")
public class MiniGame implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** 主键 */
    private Long id;

    @Column(nullable = false, length = 50)
    /** 名称 */
    private String name;

    @Column(nullable = false, length = 200)
    /** 页面路径 */
    private String path;

    @Column(nullable = false)
    /** 达标奖励积分 */
    private Integer rewardPoints;

    @Column(nullable = false)
    /** 1启用 0停用 */
    private Integer status;

    @Column(length = 255)
    /** 描述 */
    private String description;

    @Column(length = 100)
    /** 图标 */
    private String icon;

    @Column(length = 30)
    /** 玩法标识：snake / 2048 / guess / gacha，前端据此分派具体玩法 */
    private String gameKey;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(Integer rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getGameKey() {
        return gameKey;
    }

    public void setGameKey(String gameKey) {
        this.gameKey = gameKey;
    }

}
