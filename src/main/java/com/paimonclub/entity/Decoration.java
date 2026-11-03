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
 * 个性装扮（t_decoration）
 */
@Entity
@Table(name = "t_decoration")
public class Decoration implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** 主键 */
    private Long id;

    @Column(nullable = false, length = 100)
    /** 名称 */
    private String name;

    @Column(nullable = false, length = 20)
    /** AVATAR_FRAME/TITLE/THEME/POST_BG */
    private String type;

    @Column(length = 500)
    /** 资源地址 */
    private String resourceUrl;

    @Column(nullable = false)
    /** 积分价格 */
    private Integer price;

    @Column(nullable = false)
    /** 1可用 0停用 */
    private Integer status;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

}
