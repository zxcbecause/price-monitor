package io.github.zxcbecause.pricemonitor.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void convertsRublesToKopecksAndBack() {
        assertThat(Money.toKopecks(new BigDecimal("1499.99"))).isEqualTo(149_999L);
        assertThat(Money.toKopecks(new BigDecimal("0.005"))).isEqualTo(1L);
        assertThat(Money.toRubles(149_999L)).isEqualByComparingTo("1499.99");
    }

    @Test
    void formatsForHumans() {
        assertThat(Money.format(100_000L)).isEqualTo("1 000 ₽");
        assertThat(Money.format(123_456L)).isEqualTo("1 234,56 ₽");
    }
}
