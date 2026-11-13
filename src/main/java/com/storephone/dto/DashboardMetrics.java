package com.storephone.dto;

import com.storephone.model.Order;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public record DashboardMetrics(
        long activeUserCount,
        long activeProductCount,
        long pendingOrderCount,
        BigDecimal completedRevenue,
        List<Order> recentOrders,
        List<TopProductStat> topProducts
) {
    public record TopProductStat(
            Long productId,
            String productName,
            long totalSoldQuantity,
            BigDecimal totalRevenue
    ) {
    }

    public DashboardMetrics {
        completedRevenue = completedRevenue == null ? BigDecimal.ZERO : completedRevenue;
        recentOrders = recentOrders == null ? Collections.emptyList() : List.copyOf(recentOrders);
        topProducts = topProducts == null ? Collections.emptyList() : List.copyOf(topProducts);
    }
}
