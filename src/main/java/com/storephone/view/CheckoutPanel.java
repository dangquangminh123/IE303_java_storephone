package com.storephone.view;

import com.storephone.controller.NavigationController;
import com.storephone.controller.NavigationRoutes;
import com.storephone.dto.CartSummary;
import com.storephone.dto.CheckoutRequest;
import com.storephone.model.User;
import com.storephone.service.CartService;
import com.storephone.service.CheckoutService;
import com.storephone.service.UserService;
import com.storephone.util.MoneyFormatter;
import com.storephone.util.UiTaskRunner;
import com.storephone.view.table.CartItemTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

public class CheckoutPanel extends JPanel {
    private final CartService cartService;
    private final CheckoutService checkoutService;
    private final UserService userService;
    private final NavigationController nav;

    private final CartItemTableModel previewModel = new CartItemTableModel();
    private final JTextField receiverNameField = new JTextField(22);
    private final JTextField receiverPhoneField = new JTextField(22);
    private final JTextField receiverAddressField = new JTextField(22);
    private final JLabel totalLabel = new JLabel("Tổng thanh toán: 0 ₫");
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton confirmBtn = UiTheme.primaryButton("Xác nhận Đặt hàng");

    public CheckoutPanel(
            CartService cartService,
            CheckoutService checkoutService,
            UserService userService,
            NavigationController nav
    ) {
        super(new BorderLayout(16, 16));
        this.cartService = cartService;
        this.checkoutService = checkoutService;
        this.userService = userService;
        this.nav = nav;
        setBackground(UiTheme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        buildUi();
    }

    private void buildUi() {
        JLabel title = new JLabel("Thanh Toán & Xác Nhận Đơn Hàng");
        title.setFont(UiTheme.FONT_TITLE);
        add(title, BorderLayout.NORTH);

        JTable previewTable = new JTable(previewModel);
        previewTable.setRowHeight(28);
        previewTable.setEnabled(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UiTheme.CARD_BG);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addRow(form, gbc, "Họ tên người nhận *", receiverNameField);
        addRow(form, gbc, "Số điện thoại nhận hàng *", receiverPhoneField);
        addRow(form, gbc, "Địa chỉ giao hàng chi tiết *", receiverAddressField);

        gbc.gridy++;
        gbc.insets = new Insets(10, 0, 6, 0);
        totalLabel.setFont(UiTheme.FONT_BRAND);
        totalLabel.setForeground(UiTheme.PRIMARY);
        form.add(totalLabel, gbc);

        gbc.gridy++;
        errorLabel.setForeground(UiTheme.DANGER);
        form.add(errorLabel, gbc);

        gbc.gridy++;
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton backBtn = UiTheme.navButton("« Quay lại Giỏ hàng");
        backBtn.addActionListener(e -> nav.navigateTo(NavigationRoutes.CART));
        confirmBtn.addActionListener(e -> submitOrder());
        actions.add(confirmBtn);
        actions.add(backBtn);
        form.add(actions, gbc);

        add(new JScrollPane(previewTable), BorderLayout.CENTER);
        add(form, BorderLayout.EAST);
    }

    private static void addRow(JPanel p, GridBagConstraints gbc, String lbl, JTextField field) {
        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 2, 0);
        p.add(new JLabel(lbl), gbc);
        gbc.gridy++;
        p.add(field, gbc);
    }

    public void reloadCheckoutPreview() {
        errorLabel.setText(" ");
        UiTaskRunner.runAsync(
                () -> new Object[]{cartService.getMyCart(), userService.getCurrentUserProfile()},
                res -> {
                    CartSummary summary = (CartSummary) res[0];
                    User profile = (User) res[1];
                    previewModel.setRows(summary.items());
                    totalLabel.setText("Tổng (" + summary.totalQuantity() + " máy): " + MoneyFormatter.formatVnd(summary.totalAmount()));
                    confirmBtn.setEnabled(!summary.isEmpty());
                    if (receiverNameField.getText().isBlank() && profile.getFullName() != null) {
                        receiverNameField.setText(profile.getFullName());
                    }
                    if (receiverPhoneField.getText().isBlank() && profile.getPhone() != null) {
                        receiverPhoneField.setText(profile.getPhone());
                    }
                    if (receiverAddressField.getText().isBlank() && profile.getAddress() != null) {
                        receiverAddressField.setText(profile.getAddress());
                    }
                },
                err -> errorLabel.setText(err.getMessage())
        );
    }

    private void submitOrder() {
        confirmBtn.setEnabled(false);
        errorLabel.setText(" ");
        CheckoutRequest req = new CheckoutRequest(
                receiverNameField.getText(), receiverPhoneField.getText(), receiverAddressField.getText());
        UiTaskRunner.runAsync(
                () -> checkoutService.placeOrder(req),
                order -> {
                    JOptionPane.showMessageDialog(this,
                            "Đặt hàng thành công! Mã đơn #" + order.getId() + " đang chờ xác nhận.",
                            "Hoàn tất đặt hàng", JOptionPane.INFORMATION_MESSAGE);
                    nav.navigateTo(NavigationRoutes.ORDER_HISTORY);
                },
                err -> {
                    confirmBtn.setEnabled(true);
                    errorLabel.setText(err.getMessage());
                }
        );
    }
}

