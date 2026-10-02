package io.github.zxcbecause.pricemonitor.marketplace;

/**
 * Current price of a product as reported by the marketplace.
 *
 * @param title        product name (may be null if the API does not return it)
 * @param priceKopecks price the buyer pays right now, in kopecks
 */
public record ProductPrice(String title, long priceKopecks) {
}
