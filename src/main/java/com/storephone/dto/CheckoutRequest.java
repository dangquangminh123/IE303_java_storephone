package com.storephone.dto;

public record CheckoutRequest(
        String receiverName,
        String receiverPhone,
        String receiverAddress
) {
}
