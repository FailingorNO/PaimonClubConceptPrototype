package com.paimonclub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 兑换订单（t_order）。
 *
 * <p>状态机：</p>
 * <ul>
 *   <li>虚拟商品：下单即 {@code COMPLETED}，权益当场发放；</li>
 *   <li>实物商品：{@code CREATED}（待发货）→ {@code SHIPPED}（已发货）→ {@code COMPLETED}（已签收）；</li>
 *   <li>{@code CREATED} 状态下用户可取消，{@code CANCELED} 并原路退还积分、恢复库存。</li>
 * </ul>
 */
@Entity
@Table(name = "t_order")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** 主键 */
    private Long id;

    @Column(nullable = false)
    /** 用户 */
    private Long userId;

    @Column(nullable = false)
    /** 商品 */
    private Long productId;

    @Column(length = 100)
    /** 商品名快照 */
    private String productName;

    @Column(length = 20)
    /** 商品类型快照：VIRTUAL / PHYSICAL */
    private String productType;

    @Column(nullable = false)
    /** 消耗积分 */
    private Integer pointsCost;

    @Column(nullable = false, length = 20)
    /** CREATED / SHIPPED / COMPLETED / CANCELED */
    private String status;

    @Column(length = 50)
    /** 收货人 */
    private String receiverName;

    @Column(length = 30)
    /** 收货电话 */
    private String receiverPhone;

    @Column(length = 255)
    /** 收货地址 */
    private String receiverAddress;

    @Column(length = 50)
    /** 物流单号，发货时填写 */
    private String logisticsNo;

    @Column(length = 255)
    /** 用户备注 */
    private String remark;

    @Column(nullable = false)
    /** 下单时间 */
    private LocalDateTime createdAt;

    @Column
    /** 发货时间 */
    private LocalDateTime shippedAt;

    @Column
    /** 完成时间 */
    private LocalDateTime finishedAt;

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

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public Integer getPointsCost() {
        return pointsCost;
    }

    public void setPointsCost(Integer pointsCost) {
        this.pointsCost = pointsCost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public String getLogisticsNo() {
        return logisticsNo;
    }

    public void setLogisticsNo(String logisticsNo) {
        this.logisticsNo = logisticsNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
