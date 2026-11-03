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
 * 通知（t_notification）
 */
@Entity
@Table(name = "t_notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** 主键 */
    private Long id;

    @Column(nullable = false)
    /** 接收者 */
    private Long userId;

    @Column(nullable = false, length = 30)
    /** LIKE/COMMENT/FOLLOW/AUDIT/SYSTEM */
    private String type;

    @Column(nullable = false, length = 255)
    /** 内容 */
    private String content;

    @Column
    /** 关联业务 ID */
    private Long refId;

    @Column(nullable = false)
    /** 是否已读 */
    private Boolean isRead;

    @Column(nullable = false)
    /** 生成时间 */
    private LocalDateTime createdAt;

    @Column
    /** 合并条数：同类通知合并展示时累加，1 表示没有合并 */
    private Integer mergeCount;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Integer getMergeCount() {
        return mergeCount;
    }

    public void setMergeCount(Integer mergeCount) {
        this.mergeCount = mergeCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
