package io.github.zxcbecause.pricemonitor.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.zxcbecause.pricemonitor.domain.Marketplace;
import io.github.zxcbecause.pricemonitor.monitoring.AlertPolicy;
import io.github.zxcbecause.pricemonitor.monitoring.PriceChangedEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PriceAlertListenerTest {

    @Test
    void formatsTargetReachedMessage() {
        var event = new PriceChangedEvent(1L, Marketplace.WILDBERRIES, "123", "Кроссовки",
                100_000, 85_000, 90_000L, null, 42L, Instant.now());

        String text = PriceAlertListener.format(event, AlertPolicy.Reason.TARGET_REACHED);

        assertThat(text)
                .contains("Кроссовки")
                .contains("1 000 ₽ → 850 ₽ (−15%)")
                .contains("Цель 900 ₽ достигнута")
                .contains("https://www.wildberries.ru/catalog/123/detail.aspx");
    }
}
