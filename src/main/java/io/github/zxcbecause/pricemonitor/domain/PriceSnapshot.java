package io.github.zxcbecause.pricemonitor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "price_snapshot")
public class PriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "price_kopecks", nullable = false)
    private long priceKopecks;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    protected PriceSnapshot() {
    }

    public PriceSnapshot(Long productId, long priceKopecks, Instant capturedAt) {
        this.productId = productId;
        this.priceKopecks = priceKopecks;
        this.capturedAt = capturedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public long getPriceKopecks() {
        return priceKopecks;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }
}
