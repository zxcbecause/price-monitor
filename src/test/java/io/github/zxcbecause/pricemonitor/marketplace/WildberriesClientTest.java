package io.github.zxcbecause.pricemonitor.marketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class WildberriesClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void takesCheapestSizeFromNewFormat() throws Exception {
        JsonNode body = mapper.readTree("""
                {"products": [{"id": 123, "name": "Кроссовки", "sizes": [
                    {"price": {"basic": 500000, "product": 349900}},
                    {"price": {"basic": 500000, "product": 329900}},
                    {}
                ]}]}
                """);

        ProductPrice price = WildberriesClient.parse("123", body);

        assertThat(price.title()).isEqualTo("Кроссовки");
        assertThat(price.priceKopecks()).isEqualTo(329_900L);
    }

    @Test
    void supportsOldFormat() throws Exception {
        JsonNode body = mapper.readTree("""
                {"data": {"products": [{"id": 777, "name": "Чехол", "salePriceU": 59000}]}}
                """);

        assertThat(WildberriesClient.parse("777", body).priceKopecks()).isEqualTo(59_000L);
    }

    @Test
    void failsWhenProductIsMissing() throws Exception {
        JsonNode body = mapper.readTree("{\"products\": []}");

        assertThatThrownBy(() -> WildberriesClient.parse("1", body))
                .isInstanceOf(PriceFetchException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void failsWhenOutOfStock() throws Exception {
        JsonNode body = mapper.readTree("{\"products\": [{\"id\": 5, \"sizes\": [{}]}]}");

        assertThatThrownBy(() -> WildberriesClient.parse("5", body))
                .isInstanceOf(PriceFetchException.class)
                .hasMessageContaining("out of stock");
    }
}
