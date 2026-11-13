package com.storephone.view;

import com.storephone.controller.NavigationController;
import com.storephone.controller.NavigationRoutes;
import com.storephone.dto.CartSummary;
import com.storephone.model.CartItem;
import com.storephone.service.CartService;
import com.storephone.util.MoneyFormatter;
import com.storephone.util.UiTaskRunner;
import com.storephone.view.table.CartItemTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

public class CartPanel extends JPanel {
    private final CartService cartService;
    private final NavigationController nav;
    private final CartItemTableModel tableModel = new CartItemTableModel();
    private final JTable table = new JTable(tableModel);
    private final JLabel summaryLabel = new JLabel("Tổng cộng: 0 máy — 0 ₫");
    private final JButton checkoutBtn = UiTheme.primaryButton("Tiến hành Đặt hàng »");

    public CartPanel(CartService cartService, NavigationController nav) {
        super(new BorderLayout(12, 12));
        this.cartService = cartService;
        this.nav = nav;
        setBackground(UiTheme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        buildUi();
    }

    private void buildUi() {
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        JLabel title = new JLabel("Giỏ Hàng Cá Nhân");
        title.setFont(UiTheme.FONT_TITLE);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton decBtn = UiTheme.navButton("- Giảm 1");
        JButton incBtn = UiTheme.navButton("+ Tăng 1");
        JButton setQtyBtn = UiTheme.navButton("Nhập số lượng...");
        JButton removeBtn = UiTheme.navButton("Xóa khỏi giỏ");
        JButton continueBtn = UiTheme.navButton("« Tiếp tục mua sắm");

        actions.add(decBtn);
        actions.add(incBtn);
        actions.add(setQtyBtn);
        actions.add(removeBtn);
        actions.add(continueBtn);

        top.add(title, BorderLayout.NORTH);
        top.add(actions, BorderLayout.SOUTH);

        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setBackground(UiTheme.CARD_BG);
        bottom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)
        ));
        summaryLabel.setFont(UiTheme.FONT_BRAND);
        summaryLabel.setForeground(UiTheme.PRIMARY);
        bottom.add(summaryLabel, BorderLayout.WEST);
        bottom.add(checkoutBtn, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        decBtn.addActionListener(e -> withSelected(i -> changeQty(i.getId(), i.getQuantity() - 1)));
        incBtn.addActionListener(e -> withSelected(i -> changeQty(i.getId(), i.getQuantity() + 1)));
        setQtyBtn.addActionListener(e -> withSelected(this::promptQuantity));
        removeBtn.addActionListener(e -> withSelected(this::removeItem));
        continueBtn.addActionListener(e -> nav.navigateTo(NavigationRoutes.CATALOG));
        checkoutBtn.addActionListener(e -> nav.navigateTo(NavigationRoutes.CHECKOUT));
    }

    public void reloadCart() {
        UiTaskRunner.runAsync(
                cartService::getMyCart,
                this::applyCart,
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Lỗi tải giỏ hàng", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void applyCart(CartSummary summary) {
        tableModel.setRows(summary.items());
        summaryLabel.setText("Tổng cộng: " + summary.totalQuantity() + " máy — " + MoneyFormatter.formatVnd(summary.totalAmount()));
        checkoutBtn.setEnabled(!summary.isEmpty());
    }

    private void withSelected(java.util.function.Consumer<CartItem> action) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một sản phẩm trong giỏ hàng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        CartItem item = tableModel.getAt(row);
        if (item != null) {
            action.accept(item);
        }
    }

    private void changeQty(Long itemId, int nextQty) {
        if (nextQty <= 0) {
            JOptionPane.showMessageDialog(this, "Số lượng tối thiểu là 1. Nếu không mua nữa, hãy bấm 'Xóa khỏi giỏ'.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        UiTaskRunner.runAsync(
                () -> cartService.updateItemQuantity(itemId, nextQty),
                this::applyCart,
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Không thể cập nhật số lượng", JOptionPane.WARNING_MESSAGE)
        );
    }

    private void promptQuantity(CartItem item) {
        String raw = JOptionPane.showInputDialog(this, "Nhập số lượng mới cho '" + item.getProductName() + "':", item.getQuantity());
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            int qty = Integer.parseInt(raw.trim());
            changeQty(item.getId(), qty);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số lượng phải là số nguyên hợp lệ.", "Lỗi đầu vào", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void removeItem(CartItem item) {
        UiTaskRunner.runAsync(
                () -> cartService.removeItem(item.getId()),
                this::applyCart,
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Lỗi xóa sản phẩm", JOptionPane.ERROR_MESSAGE)
        );
    }
}

