package io.github.zxcbecause.pricemonitor.domain;

public enum Marketplace {

    WILDBERRIES("Wildberries", "https://www.wildberries.ru/catalog/%s/detail.aspx"),
    OZON("Ozon", "https://www.ozon.ru/product/%s/");

    private final String displayName;
    private final String productUrlTemplate;

    Marketplace(String displayName, String productUrlTemplate) {
        this.displayName = displayName;
        this.productUrlTemplate = productUrlTemplate;
    }

    public String displayName() {
        return displayName;
    }

    public String productUrl(String sku) {
        return productUrlTemplate.formatted(sku);
    }
}
