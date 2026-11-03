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
 * 帖子表（t_post）
 */
@Entity
@Table(name = "t_post")
public class Post implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** 主键 */
    private Long id;

    @Column(nullable = false)
    /** 作者 */
    private Long userId;

    @Column(nullable = false, length = 200)
    /** 标题 */
    private String title;

    @Column(nullable = false, length = 5000)
    /** 正文 */
    private String content;

    @Column(nullable = false)
    /** 分类 */
    private Long categoryId;

    @Column(nullable = false, length = 20)
    /** PENDING/PUBLISHED/REJECTED */
    private String status;

    @Column(nullable = false)
    /** 点赞数 */
    private Integer likeCount;

    @Column(nullable = false)
    /** 评论数 */
    private Integer commentCount;

    @Column(nullable = false)
    /** 浏览数 */
    private Integer viewCount;

    @Column(nullable = false)
    /** 收藏数 */
    private Integer favoriteCount;

    @Column(name = "is_top", nullable = false)
    /** 是否置顶 */
    private Boolean top;

    @Column(length = 255)
    /** 标签，逗号分隔 */
    private String tags;

    @Column(length = 255)
    /** 驳回原因 */
    private String rejectReason;

    @Column(nullable = false)
    /** 发布时间 */
    private LocalDateTime createdAt;

    @Column(nullable = false)
    /** 更新时间 */
    private LocalDateTime updatedAt;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public Boolean getTop() {
        return top;
    }

    public void setTop(Boolean top) {
        this.top = top;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
