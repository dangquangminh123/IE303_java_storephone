package com.storephone.dto;

import com.storephone.model.CartItem;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public record CartSummary(
        Long cartId,
        List<CartItem> items,
        int totalQuantity,
        BigDecimal totalAmount
) {
    public CartSummary {
        items = items == null ? Collections.emptyList() : List.copyOf(items);
        totalQuantity = Math.max(0, totalQuantity);
        totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
