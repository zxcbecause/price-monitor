package io.github.zxcbecause.pricemonitor.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.zxcbecause.pricemonitor.config.MonitorProperties;
import io.github.zxcbecause.pricemonitor.domain.Marketplace;
import io.github.zxcbecause.pricemonitor.domain.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Ozon has no public price API, so this client uses the Seller API
 * ({@code /v5/product/info/prices}) and works for products of your own store.
 * SKU here is Ozon {@code product_id}.
 */
@Component
public class OzonClient implements MarketplaceClient {

    private final RestClient restClient;
    private final boolean configured;

    public OzonClient(RestClient.Builder builder, MonitorProperties properties) {
        MonitorProperties.Ozon ozon = properties.ozon();
        this.configured = ozon.configured();
        this.restClient = builder.baseUrl(ozon.baseUrl())
                .defaultHeader("Client-Id", ozon.clientId())
                .defaultHeader("Api-Key", ozon.apiKey())
                .build();
    }

    @Override
    public Marketplace marketplace() {
        return Marketplace.OZON;
    }

    @Override
    public ProductPrice fetchPrice(String sku) {
        if (!configured) {
            throw new PriceFetchException("Ozon Seller API is not configured (monitor.ozon.client-id / api-key)");
        }
        Map<String, Object> request = Map.of(
                "filter", Map.of("product_id", List.of(sku), "visibility", "ALL"),
                "limit", 1);
        JsonNode body;
        try {
            body = restClient.post()
                    .uri("/v5/product/info/prices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new PriceFetchException("Ozon request failed for " + sku, e);
        }
        return parse(sku, body);
    }

    static ProductPrice parse(String sku, JsonNode body) {
        if (body == null) {
            throw new PriceFetchException("Empty Ozon response for " + sku);
        }
        for (JsonNode item : body.path("items")) {
            if (!sku.equals(item.path("product_id").asText())) {
                continue;
            }
            String raw = item.path("price").path("price").asText("");
            if (raw.isBlank()) {
                throw new PriceFetchException("Ozon returned no price for " + sku);
            }
            long kopecks = Money.toKopecks(new BigDecimal(raw));
            return new ProductPrice(item.path("offer_id").asText(null), kopecks);
        }
        throw new PriceFetchException("Product " + sku + " not found on Ozon");
    }
}
