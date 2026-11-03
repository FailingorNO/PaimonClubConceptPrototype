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
 * 游戏账号绑定（t_game_account）
 */
@Entity
@Table(name = "t_game_account")
public class GameAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** 主键 */
    private Long id;

    @Column(nullable = false)
    /** 所属用户 */
    private Long userId;

    @Column(nullable = false, length = 30)
    /** 游戏 UID */
    private String gameUid;

    @Column(length = 30)
    /** 服务器/区服 */
    private String server;

    @Column(length = 50)
    /** 游戏名 */
    private String gameName;

    @Column(length = 50)
    /** 游戏内昵称 */
    private String nickname;

    @Column(name = "adventure_rank")
    /** 冒险等阶 */
    private Integer adventureRank;

    @Column(name = "world_level")
    /** 世界等级 */
    private Integer worldLevel;

    @Column(nullable = false)
    /** 绑定时间 */
    private LocalDateTime bindTime;

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

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getAdventureRank() {
        return adventureRank;
    }

    public void setAdventureRank(Integer adventureRank) {
        this.adventureRank = adventureRank;
    }

    public Integer getWorldLevel() {
        return worldLevel;
    }

    public void setWorldLevel(Integer worldLevel) {
        this.worldLevel = worldLevel;
    }

    public LocalDateTime getBindTime() {
        return bindTime;
    }

    public void setBindTime(LocalDateTime bindTime) {
        this.bindTime = bindTime;
    }

}
