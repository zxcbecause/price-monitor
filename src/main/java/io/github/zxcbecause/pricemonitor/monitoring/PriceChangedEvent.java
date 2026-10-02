package io.github.zxcbecause.pricemonitor.monitoring;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.zxcbecause.pricemonitor.domain.Marketplace;
import java.time.Instant;

/**
 * Published to Kafka every time a tracked price changes (up or down).
 * Any service can subscribe: alerts, analytics, repricing, etc.
 */
public record PriceChangedEvent(
        Long productId,
        Marketplace marketplace,
        String sku,
        String title,
        long oldPriceKopecks,
        long newPriceKopecks,
        Long targetPriceKopecks,
        Integer dropThresholdPercent,
        Long chatId,
        Instant changedAt
) {

    @JsonIgnore
    public boolean isDrop() {
        return newPriceKopecks < oldPriceKopecks;
    }

    /** Percentage of the drop relative to the old price, e.g. 1000 -> 850 gives 15. Negative for rises. */
    @JsonIgnore
    public double dropPercent() {
        return (oldPriceKopecks - newPriceKopecks) * 100.0 / oldPriceKopecks;
    }
}
