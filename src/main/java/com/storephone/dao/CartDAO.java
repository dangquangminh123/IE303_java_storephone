package com.storephone.dao;

import com.storephone.model.Cart;
import com.storephone.model.CartItem;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface CartDAO {
    Cart getOrCreateByUserId(Long userId);

    Optional<Cart> findByUserId(Long userId);

    List<CartItem> findItemsByCartId(Long cartId);

    Optional<CartItem> findItemByCartAndProduct(Long cartId, Long productId);

    void upsertItem(Long cartId, Long productId, int quantity, BigDecimal unitPrice);

    void updateItemQuantity(Long cartItemId, int quantity);

    void deleteItem(Long cartItemId);

    void syncTotalQuantity(Long cartId);

    void syncTotalQuantity(Connection conn, Long cartId);

    void clearCart(Connection conn, Long cartId);
}
