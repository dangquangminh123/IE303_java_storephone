package com.storephone.service;

import com.storephone.config.ConnectionFactory;
import com.storephone.dao.CartDAO;
import com.storephone.dao.OrderDAO;
import com.storephone.dao.ProductDAO;
import com.storephone.dto.CheckoutRequest;
import com.storephone.exception.DataAccessException;
import com.storephone.exception.InsufficientStockException;
import com.storephone.exception.ValidationException;
import com.storephone.model.Cart;
import com.storephone.model.CartItem;
import com.storephone.model.Order;
import com.storephone.model.OrderItem;
import com.storephone.model.OrderStatus;
import com.storephone.model.Product;
import com.storephone.security.AppSession;
import com.storephone.util.InputValidator;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class CheckoutService {
    private final ConnectionFactory connectionFactory;
    private final CartDAO cartDAO;
    private final ProductDAO productDAO;
    private final OrderDAO orderDAO;
    private final AppSession appSession;
    private final AuthorizationService authorizationService;

    public CheckoutService(
            ConnectionFactory connectionFactory,
            CartDAO cartDAO,
            ProductDAO productDAO,
            OrderDAO orderDAO,
            AppSession appSession,
            AuthorizationService authorizationService
    ) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
        this.cartDAO = Objects.requireNonNull(cartDAO);
        this.productDAO = Objects.requireNonNull(productDAO);
        this.orderDAO = Objects.requireNonNull(orderDAO);
        this.appSession = Objects.requireNonNull(appSession);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    public Order placeOrder(CheckoutRequest request) {
        return placeOrderInternal(request, null);
    }

    public Order placeOrderWithFaultHook(CheckoutRequest request, Runnable afterOrderInsertedHook) {
        return placeOrderInternal(request, afterOrderInsertedHook);
    }

    private Order placeOrderInternal(CheckoutRequest request, Runnable afterOrderInsertedHook) {
        authorizationService.requireAuthenticated();
        if (request == null) {
            throw new ValidationException("Thông tin nhận hàng không hợp lệ.");
        }
        String receiverName = InputValidator.requireNonBlank("receiverName", request.receiverName(), "Họ tên người nhận");
        String receiverPhone = InputValidator.validatePhone(request.receiverPhone(), true);
        String receiverAddress = InputValidator.requireNonBlank("receiverAddress", request.receiverAddress(), "Địa chỉ nhận hàng");

        Long userId = appSession.getCurrentUser().orElseThrow().userId();
        Cart cart = cartDAO.getOrCreateByUserId(userId);
        List<CartItem> rawItems = cartDAO.findItemsByCartId(cart.getId());
        if (rawItems.isEmpty()) {
            throw new ValidationException("cart", "Giỏ hàng đang trống, không thể đặt đơn.");
        }

        List<CartItem> sortedItems = new ArrayList<>(rawItems);
        sortedItems.sort(Comparator.comparing(CartItem::getProductId));

        try (Connection conn = connectionFactory.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                List<OrderItem> snapshots = new ArrayList<>();
                BigDecimal totalAmount = BigDecimal.ZERO;

                for (CartItem ci : sortedItems) {
                    Product locked = productDAO.findByIdForUpdate(conn, ci.getProductId())
                            .orElseThrow(() -> new ValidationException("product",
                                    "Sản phẩm '" + ci.getProductName() + "' không còn tồn tại."));
                    if (!locked.isActive()) {
                        throw new ValidationException("product", "Sản phẩm '" + locked.getName() + "' đã ngừng bán.");
                    }
                    if (locked.getQuantity() < ci.getQuantity()) {
                        throw new InsufficientStockException(locked.getName(), locked.getQuantity(), ci.getQuantity());
                    }
                    if (!productDAO.deductStock(conn, locked.getId(), ci.getQuantity())) {
                        throw new InsufficientStockException(locked.getName(), locked.getQuantity(), ci.getQuantity());
                    }

                    OrderItem oi = new OrderItem();
                    oi.setProductId(locked.getId());
                    oi.setProductName(locked.getName());
                    oi.setQuantity(ci.getQuantity());
                    oi.setUnitPrice(locked.getPrice());
                    snapshots.add(oi);
                    totalAmount = totalAmount.add(oi.getLineTotal());
                }

                Order order = new Order();
                order.setUserId(userId);
                order.setReceiverName(receiverName);
                order.setReceiverPhone(receiverPhone);
                order.setReceiverAddress(receiverAddress);
                order.setStatus(OrderStatus.PENDING);
                order.setTotalAmount(totalAmount);

                Order savedOrder = orderDAO.insertOrder(conn, order);
                if (afterOrderInsertedHook != null) {
                    afterOrderInsertedHook.run();
                }
                orderDAO.insertOrderItems(conn, savedOrder.getId(), snapshots);
                cartDAO.clearCart(conn, cart.getId());

                conn.commit();
                appSession.setCartQuantity(0);
                return orderDAO.findById(savedOrder.getId()).orElse(savedOrder);
            } catch (RuntimeException ex) {
                rollbackQuietly(conn);
                throw ex;
            } catch (Exception ex) {
                rollbackQuietly(conn);
                throw new DataAccessException("Lỗi giao dịch đặt hàng, đã hoàn tác an toàn.", ex);
            } finally {
                try { conn.setAutoCommit(previousAutoCommit); } catch (SQLException ignored) {}
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Không thể mở kết nối CSDL để đặt hàng.", ex);
        }
    }

    private static void rollbackQuietly(Connection conn) {
        try { conn.rollback(); } catch (SQLException ignored) {}
    }
}

