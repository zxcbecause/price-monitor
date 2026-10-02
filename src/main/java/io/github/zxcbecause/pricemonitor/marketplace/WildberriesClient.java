package io.github.zxcbecause.pricemonitor.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.zxcbecause.pricemonitor.config.MonitorProperties;
import io.github.zxcbecause.pricemonitor.domain.Marketplace;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Uses the public product card endpoint of Wildberries (no API key needed).
 * Prices in the response are in kopecks.
 */
@Component
public class WildberriesClient implements MarketplaceClient {

    private final RestClient restClient;
    private final String dest;

    public WildberriesClient(RestClient.Builder builder, MonitorProperties properties) {
        this.restClient = builder.baseUrl(properties.wildberries().baseUrl()).build();
        this.dest = properties.wildberries().dest();
    }

    @Override
    public Marketplace marketplace() {
        return Marketplace.WILDBERRIES;
    }

    @Override
    public ProductPrice fetchPrice(String sku) {
        JsonNode body;
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/cards/v4/detail")
                            .queryParam("appType", 1)
                            .queryParam("curr", "rub")
                            .queryParam("dest", dest)
                            .queryParam("nm", sku)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new PriceFetchException("Wildberries request failed for " + sku, e);
        }
        return parse(sku, body);
    }

    /**
     * Supports both response shapes WB has used: {@code {"products": [...]}} and
     * {@code {"data": {"products": [...]}}}. Takes the lowest in-stock size price.
     */
    static ProductPrice parse(String sku, JsonNode body) {
        if (body == null) {
            throw new PriceFetchException("Empty Wildberries response for " + sku);
        }
        JsonNode products = body.has("products") ? body.path("products") : body.path("data").path("products");
        for (JsonNode product : products) {
            if (!sku.equals(product.path("id").asText())) {
                continue;
            }
            long best = Long.MAX_VALUE;
            for (JsonNode size : product.path("sizes")) {
                long price = size.path("price").path("product").asLong(0);
                if (price > 0 && price < best) {
                    best = price;
                }
            }
            if (best == Long.MAX_VALUE) {
                // older format kept the price on the product itself
                best = product.path("salePriceU").asLong(0);
            }
            if (best <= 0) {
                throw new PriceFetchException("Product " + sku + " is out of stock on Wildberries");
            }
            return new ProductPrice(product.path("name").asText(null), best);
        }
        throw new PriceFetchException("Product " + sku + " not found on Wildberries");
    }
}
