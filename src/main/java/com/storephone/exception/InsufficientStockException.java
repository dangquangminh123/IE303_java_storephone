package com.storephone.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String productName, int available, int requested) {
        super("Sản phẩm '" + productName + "' chỉ còn " + available + " máy (yêu cầu: " + requested + ").");
    }

    public InsufficientStockException(String message) {
        super(message);
    }
}
