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
import java.time.LocalDate;

/**
 * 用户表（t_user）
 */
@Entity
@Table(name = "t_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** 主键 */
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    /** 登录名 */
    private String username;

    @Column(nullable = false, length = 100)
    /** BCrypt 密码散列 */
    private String passwordHash;

    @Column(nullable = false, length = 50)
    /** 昵称 */
    private String nickname;

    @Column(length = 500)
    /** 头像地址 */
    private String avatarUrl;

    @Column(length = 100)
    /** 邮箱 */
    private String email;

    @Column(length = 30)
    /** 手机号 */
    private String phone;

    @Column(nullable = false, length = 20)
    /** 角色 USER/MODERATOR/ADMIN */
    private String role;

    @Column(nullable = false)
    /** 状态 1正常 0封禁 */
    private Integer status;

    @Column(nullable = false)
    /** 积分余额 */
    private Integer points;

    @Column(length = 255)
    /** 个性签名 */
    private String bio;

    /** 版主负责的板块 ID，逗号分隔；为空表示全站版主 */
    @Column(length = 255)
    private String moderatedCategories;

    @Column(name = "last_sign_in_date")
    /** 最近签到日期 */
    private LocalDate lastSignInDate;

    @Column(nullable = false)
    /** 注册时间 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getModeratedCategories() {
        return moderatedCategories;
    }

    public void setModeratedCategories(String moderatedCategories) {
        this.moderatedCategories = moderatedCategories;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public LocalDate getLastSignInDate() {
        return lastSignInDate;
    }

    public void setLastSignInDate(LocalDate lastSignInDate) {
        this.lastSignInDate = lastSignInDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
