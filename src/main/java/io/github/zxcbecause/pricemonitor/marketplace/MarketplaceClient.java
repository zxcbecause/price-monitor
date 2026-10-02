package io.github.zxcbecause.pricemonitor.marketplace;

import io.github.zxcbecause.pricemonitor.domain.Marketplace;

/**
 * One implementation per marketplace. New marketplaces plug in by adding a bean.
 */
public interface MarketplaceClient {

    Marketplace marketplace();

    /**
     * @throws PriceFetchException when the product is missing or the API is unavailable
     */
    ProductPrice fetchPrice(String sku);
}
