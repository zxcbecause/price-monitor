package io.github.zxcbecause.pricemonitor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tracked_product")
public class TrackedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Marketplace marketplace;

    @Column(nullable = false, length = 64)
    private String sku;

    private String title;

    @Column(name = "target_price_kopecks")
    private Long targetPriceKopecks;

    @Column(name = "drop_threshold_percent")
    private Integer dropThresholdPercent;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "last_price_kopecks")
    private Long lastPriceKopecks;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    protected TrackedProduct() {
    }

    public TrackedProduct(Marketplace marketplace, String sku, Long targetPriceKopecks,
                          Integer dropThresholdPercent, Long chatId) {
        this.marketplace = marketplace;
        this.sku = sku;
        this.targetPriceKopecks = targetPriceKopecks;
        this.dropThresholdPercent = dropThresholdPercent;
        this.chatId = chatId;
        this.createdAt = Instant.now();
    }

    /** Records a fresh observation and returns the previous price (null on the first check). */
    public Long observe(String title, long priceKopecks, Instant at) {
        Long previous = this.lastPriceKopecks;
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        this.lastPriceKopecks = priceKopecks;
        this.lastCheckedAt = at;
        return previous;
    }

    public Long getId() {
        return id;
    }

    public Marketplace getMarketplace() {
        return marketplace;
    }

    public String getSku() {
        return sku;
    }

    public String getTitle() {
        return title;
    }

    public Long getTargetPriceKopecks() {
        return targetPriceKopecks;
    }

    public void setTargetPriceKopecks(Long targetPriceKopecks) {
        this.targetPriceKopecks = targetPriceKopecks;
    }

    public Integer getDropThresholdPercent() {
        return dropThresholdPercent;
    }

    public void setDropThresholdPercent(Integer dropThresholdPercent) {
        this.dropThresholdPercent = dropThresholdPercent;
    }

    public Long getChatId() {
        return chatId;
    }

    public Long getLastPriceKopecks() {
        return lastPriceKopecks;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }
}
