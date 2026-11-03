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
 * 积分商品（t_product）
 */
@Entity
@Table(name = "t_product")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** 主键 */
    private Long id;

    @Column(nullable = false, length = 100)
    /** 商品名 */
    private String name;

    @Column(length = 500)
    /** 描述 */
    private String description;

    @Column(length = 500)
    /** 图片 */
    private String image;

    @Column(nullable = false)
    /** 积分价格 */
    private Integer pricePoints;

    @Column(nullable = false)
    /** 库存 */
    private Integer stock;

    @Column(nullable = false, length = 20)
    /** DECORATION/PHYSICAL */
    private String type;

    @Column(nullable = false)
    /** 1上架 0下架 */
    private Integer status;

    @Column(nullable = false)
    /** 创建时间 */
    private LocalDateTime createdAt;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getPricePoints() {
        return pricePoints;
    }

    public void setPricePoints(Integer pricePoints) {
        this.pricePoints = pricePoints;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
