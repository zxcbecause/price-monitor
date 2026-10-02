package io.github.zxcbecause.pricemonitor.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.zxcbecause.pricemonitor.domain.Marketplace;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AlertPolicyTest {

    private final AlertPolicy policy = new AlertPolicy(10);

    @Test
    void priceRiseNeverAlerts() {
        assertThat(policy.evaluate(event(1000_00, 1200_00, 2000_00L, null))).isEmpty();
    }

    @Test
    void targetReachedWinsOverSmallDrop() {
        assertThat(policy.evaluate(event(1000_00, 990_00, 995_00L, null)))
                .contains(AlertPolicy.Reason.TARGET_REACHED);
    }

    @Test
    void exactlyTargetPriceCounts() {
        assertThat(policy.evaluate(event(1000_00, 900_00, 900_00L, null)))
                .contains(AlertPolicy.Reason.TARGET_REACHED);
    }

    @Test
    void bigDropUsesDefaultThreshold() {
        assertThat(policy.evaluate(event(1000_00, 900_00, null, null)))
                .contains(AlertPolicy.Reason.BIG_DROP);
        assertThat(policy.evaluate(event(1000_00, 901_00, null, null))).isEmpty();
    }

    @Test
    void productThresholdOverridesDefault() {
        assertThat(policy.evaluate(event(1000_00, 970_00, null, 3)))
                .contains(AlertPolicy.Reason.BIG_DROP);
        assertThat(policy.evaluate(event(1000_00, 850_00, null, 20))).isEmpty();
    }

    private static PriceChangedEvent event(long oldPrice, long newPrice, Long target, Integer threshold) {
        return new PriceChangedEvent(1L, Marketplace.WILDBERRIES, "123", "Test", oldPrice, newPrice,
                target, threshold, 42L, Instant.now());
    }
}
