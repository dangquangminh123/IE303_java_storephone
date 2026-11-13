package com.storephone.view;

import com.storephone.controller.NavigationController;
import com.storephone.controller.NavigationRoutes;
import com.storephone.exception.AuthorizationException;
import com.storephone.model.Product;
import com.storephone.security.AppSession;
import com.storephone.service.CartService;
import com.storephone.util.ImageStorage;
import com.storephone.util.MoneyFormatter;
import com.storephone.util.UiTaskRunner;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

public class ProductDetailDialog extends JDialog {
    public ProductDetailDialog(
            Window owner,
            Product product,
            ImageStorage imageStorage,
            AppSession session,
            CartService cartService,
            NavigationController nav
    ) {
        super(owner, "Chi Tiết Điện Thoại — " + product.getName(), ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(20, 16));
        root.setBackground(UiTheme.CARD_BG);
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        root.setPreferredSize(new Dimension(720, 460));

        JLabel imgLabel = new JLabel(imageStorage.loadScaledIcon(product.getImagePath(), 220, 220, product.getFactory()));
        imgLabel.setHorizontalAlignment(JLabel.CENTER);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nameLbl = new JLabel(product.getName());
        nameLbl.setFont(UiTheme.FONT_TITLE);

        JLabel priceLbl = new JLabel(MoneyFormatter.formatVnd(product.getPrice()));
        priceLbl.setFont(UiTheme.FONT_BRAND);
        priceLbl.setForeground(UiTheme.PRIMARY);

        String stockText = product.getQuantity() > 0 ? "Còn hàng: " + product.getQuantity() + " máy" : "Tạm hết hàng";
        JLabel metaLbl = new JLabel("Hãng: " + product.getFactory() + "  |  Nhu cầu: " + product.getTarget() + "  |  " + stockText);
        metaLbl.setFont(UiTheme.FONT_SECTION);
        metaLbl.setForeground(product.getQuantity() > 0 ? UiTheme.SUCCESS : UiTheme.DANGER);

        JLabel shortLbl = new JLabel("<html><b>Điểm nổi bật:</b> " + (product.getShortDesc() == null ? "" : product.getShortDesc()) + "</html>");
        shortLbl.setFont(UiTheme.FONT_BODY);

        JTextArea detailArea = new JTextArea(product.getDetailDesc() == null ? "" : product.getDetailDesc());
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setFont(UiTheme.FONT_BODY);
        detailArea.setBackground(UiTheme.CONTENT_BG);

        info.add(nameLbl);
        info.add(Box.createVerticalStrut(6));
        info.add(priceLbl);
        info.add(Box.createVerticalStrut(6));
        info.add(metaLbl);
        info.add(Box.createVerticalStrut(10));
        info.add(shortLbl);
        info.add(Box.createVerticalStrut(10));
        info.add(new JScrollPane(detailArea));

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        int maxQty = Math.max(1, product.getQuantity());
        JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxQty, 1));
        JButton addCartBtn = UiTheme.primaryButton(product.getQuantity() > 0 ? "Thêm vào giỏ hàng" : "Hết hàng");
        addCartBtn.setEnabled(product.getQuantity() > 0);
        JButton closeBtn = UiTheme.navButton("Đóng");

        addCartBtn.addActionListener(e -> {
            if (!session.isLoggedIn()) {
                dispose();
                JOptionPane.showMessageDialog(owner, "Vui lòng đăng nhập tài khoản USER để thêm sản phẩm vào giỏ hàng.",
                        "Yêu cầu đăng nhập", JOptionPane.INFORMATION_MESSAGE);
                nav.navigateTo(NavigationRoutes.LOGIN);
                return;
            }
            int qty = ((Number) qtySpinner.getValue()).intValue();
            addCartBtn.setEnabled(false);
            UiTaskRunner.runAsync(
                    () -> cartService.addProductToCart(product.getId(), qty),
                    summary -> {
                        addCartBtn.setEnabled(true);
                        JOptionPane.showMessageDialog(this,
                                "Đã thêm " + qty + " máy '" + product.getName() + "' vào giỏ hàng!",
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    },
                    err -> {
                        addCartBtn.setEnabled(true);
                        if (err instanceof AuthorizationException) {
                            dispose();
                            nav.navigateTo(NavigationRoutes.LOGIN);
                        } else {
                            JOptionPane.showMessageDialog(this, err.getMessage(), "Không thể thêm vào giỏ", JOptionPane.WARNING_MESSAGE);
                        }
                    }
            );
        });
        closeBtn.addActionListener(e -> dispose());

        footer.add(new JLabel("Số lượng:"));
        footer.add(qtySpinner);
        footer.add(addCartBtn);
        footer.add(closeBtn);

        root.add(imgLabel, BorderLayout.WEST);
        root.add(info, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
    }
}
