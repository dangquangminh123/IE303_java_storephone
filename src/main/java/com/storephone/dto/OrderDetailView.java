package com.storephone.dto;

import com.storephone.model.Order;
import com.storephone.model.OrderItem;
import java.util.Collections;
import java.util.List;

public record OrderDetailView(
        Order order,
        List<OrderItem> items
) {
    public OrderDetailView {
        items = items == null ? Collections.emptyList() : List.copyOf(items);
    }
}
