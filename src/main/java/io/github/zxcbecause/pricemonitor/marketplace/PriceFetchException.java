package io.github.zxcbecause.pricemonitor.marketplace;

public class PriceFetchException extends RuntimeException {

    public PriceFetchException(String message) {
        super(message);
    }

    public PriceFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
