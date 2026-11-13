package com.storephone.service;

import com.storephone.dao.CartDAO;
import com.storephone.dao.ProductDAO;
import com.storephone.dto.CartSummary;
import com.storephone.exception.InsufficientStockException;
import com.storephone.exception.ResourceNotFoundException;
import com.storephone.exception.ValidationException;
import com.storephone.model.Cart;
import com.storephone.model.CartItem;
import com.storephone.model.Product;
import com.storephone.security.AppSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class CartService {
    private final CartDAO cartDAO;
    private final ProductDAO productDAO;
    private final AppSession appSession;
    private final AuthorizationService authorizationService;

    public CartService(
            CartDAO cartDAO,
            ProductDAO productDAO,
            AppSession appSession,
            AuthorizationService authorizationService
    ) {
        this.cartDAO = Objects.requireNonNull(cartDAO);
        this.productDAO = Objects.requireNonNull(productDAO);
        this.appSession = Objects.requireNonNull(appSession);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    public CartSummary getMyCart() {
        authorizationService.requireAuthenticated();
        Long userId = appSession.getCurrentUser().orElseThrow().userId();
        Cart cart = cartDAO.getOrCreateByUserId(userId);
        List<CartItem> items = cartDAO.findItemsByCartId(cart.getId());
        int totalQty = items.stream().mapToInt(CartItem::getQuantity).sum();
        BigDecimal totalAmount = items.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (cart.getTotalQuantity() != totalQty) {
            cartDAO.syncTotalQuantity(cart.getId());
        }
        appSession.setCartQuantity(totalQty);
        return new CartSummary(cart.getId(), items, totalQty, totalAmount);
    }

    public CartSummary addProductToCart(Long productId, int quantityToAdd) {
        authorizationService.requireAuthenticated();
        if (productId == null) {
            throw new ValidationException("Sản phẩm không hợp lệ.");
        }
        if (quantityToAdd <= 0) {
            throw new ValidationException("quantity", "Số lượng thêm vào giỏ phải lớn hơn 0.");
        }

        Product product = productDAO.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy điện thoại ID = " + productId));
        if (!product.isActive()) {
            throw new ValidationException("product", "Điện thoại '" + product.getName() + "' hiện đã ngừng kinh doanh.");
        }

        Long userId = appSession.getCurrentUser().orElseThrow().userId();
        Cart cart = cartDAO.getOrCreateByUserId(userId);
        int existingQty = cartDAO.findItemByCartAndProduct(cart.getId(), productId)
                .map(CartItem::getQuantity)
                .orElse(0);

        int desiredTotal = existingQty + quantityToAdd;
        if (desiredTotal > product.getQuantity()) {
            throw new InsufficientStockException(product.getName(), product.getQuantity(), desiredTotal);
        }

        cartDAO.upsertItem(cart.getId(), productId, desiredTotal, product.getPrice());
        return getMyCart();
    }

    public CartSummary updateItemQuantity(Long cartItemId, int newQuantity) {
        authorizationService.requireAuthenticated();
        if (newQuantity <= 0) {
            throw new ValidationException("quantity", "Số lượng sản phẩm trong giỏ phải từ 1 trở lên.");
        }
        CartItem targetItem = findOwnedCartItem(cartItemId);
        Product product = productDAO.findById(targetItem.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không còn tồn tại."));
        if (!product.isActive()) {
            throw new ValidationException("product", "Sản phẩm '" + product.getName() + "' đã ngừng bán.");
        }
        if (newQuantity > product.getQuantity()) {
            throw new InsufficientStockException(product.getName(), product.getQuantity(), newQuantity);
        }

        cartDAO.updateItemQuantity(cartItemId, newQuantity);
        return getMyCart();
    }

    public CartSummary removeItem(Long cartItemId) {
        authorizationService.requireAuthenticated();
        findOwnedCartItem(cartItemId);
        cartDAO.deleteItem(cartItemId);
        return getMyCart();
    }

    private CartItem findOwnedCartItem(Long cartItemId) {
        if (cartItemId == null) {
            throw new ValidationException("Dòng giỏ hàng không hợp lệ.");
        }
        Long userId = appSession.getCurrentUser().orElseThrow().userId();
        Cart cart = cartDAO.getOrCreateByUserId(userId);
        return cartDAO.findItemsByCartId(cart.getId()).stream()
                .filter(i -> cartItemId.equals(i.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng của bạn."));
    }
}
