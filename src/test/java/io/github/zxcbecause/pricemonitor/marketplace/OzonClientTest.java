package io.github.zxcbecause.pricemonitor.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OzonClientTest {

    @Test
    void parsesPriceAsStringOrNumber() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        var asString = mapper.readTree("""
                {"items": [{"product_id": 42, "offer_id": "case-01", "price": {"price": "1299.00"}}]}
                """);
        var asNumber = mapper.readTree("""
                {"items": [{"product_id": 42, "offer_id": "case-01", "price": {"price": 1299.5}}]}
                """);

        assertThat(OzonClient.parse("42", asString).priceKopecks()).isEqualTo(129_900L);
        assertThat(OzonClient.parse("42", asNumber).priceKopecks()).isEqualTo(129_950L);
        assertThat(OzonClient.parse("42", asNumber).title()).isEqualTo("case-01");
    }
}
