package com.marketplace.inventory.exception;

public class InsufficientStockException extends RuntimeException {

    private final String sku;
    private final Integer requestedQty;
    private final Integer availableQty;

    public InsufficientStockException(String sku, Integer requestedQty, Integer availableQty) {
        super(String.format("Insufficient stock for SKU %s: requested %d, available %d",
            sku, requestedQty, availableQty));
        this.sku = sku;
        this.requestedQty = requestedQty;
        this.availableQty = availableQty;
    }

    public String getSku() {
        return sku;
    }

    public Integer getRequestedQty() {
        return requestedQty;
    }

    public Integer getAvailableQty() {
        return availableQty;
    }
}
