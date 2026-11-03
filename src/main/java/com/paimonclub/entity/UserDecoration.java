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
 * 用户装扮持有（t_user_decoration）
 */
@Entity
@Table(name = "t_user_decoration")
public class UserDecoration implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** 主键 */
    private Long id;

    @Column(nullable = false)
    /** 用户 */
    private Long userId;

    @Column(nullable = false)
    /** 装扮 */
    private Long decorationId;

    @Column(nullable = false)
    /** 是否佩戴 */
    private Boolean isWearing;

    @Column
    /** 过期时间 */
    private LocalDateTime expireAt;

    @Column(nullable = false)
    /** 获得时间 */
    private LocalDateTime createdAt;

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

    public Long getDecorationId() {
        return decorationId;
    }

    public void setDecorationId(Long decorationId) {
        this.decorationId = decorationId;
    }

    public Boolean getIsWearing() {
        return isWearing;
    }

    public void setIsWearing(Boolean isWearing) {
        this.isWearing = isWearing;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
