package com.paimonclub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 抽卡记录（t_gacha_record）
 */
@Entity
@Table(name = "t_gacha_record")
public class GachaRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** 主键 */
    private Long id;

    @Column(nullable = false)
    /** 用户 */
    private Long userId;

    @Column(length = 30)
    /** 游戏 UID */
    private String gameUid;

    @Column(nullable = false, length = 50)
    /** 卡池 */
    private String poolName;

    @Column(nullable = false, length = 100)
    /** 物品名 */
    private String itemName;

    @Column(length = 20)
    /** 角色/武器 */
    private String itemType;

    @Column(nullable = false)
    /** 星级 3/4/5 */
    private Integer rarity;

    @Column(nullable = false)
    /** 抽取时间 */
    private LocalDateTime pullTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getGameUid() {
        return gameUid;
    }

    public void setGameUid(String gameUid) {
        this.gameUid = gameUid;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Integer getRarity() {
        return rarity;
    }

    public void setRarity(Integer rarity) {
        this.rarity = rarity;
    }

    public LocalDateTime getPullTime() {
        return pullTime;
    }

    public void setPullTime(LocalDateTime pullTime) {
        this.pullTime = pullTime;
    }

}
