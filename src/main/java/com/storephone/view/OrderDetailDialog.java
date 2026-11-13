package com.storephone.view;

import com.storephone.dto.OrderDetailView;
import com.storephone.model.Order;
import com.storephone.model.OrderItem;
import com.storephone.util.MoneyFormatter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class OrderDetailDialog extends JDialog {
    public OrderDetailDialog(Window owner, OrderDetailView detail) {
        super(owner, "Chi Tiết Đơn Hàng #" + detail.order().getId(), ModalityType.APPLICATION_MODAL);
        Order o = detail.order();

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(UiTheme.CARD_BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        root.setPreferredSize(new Dimension(660, 420));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(new JLabel("Mã đơn: #" + o.getId() + "   |   Trạng thái: " + o.getStatus().getDisplayName()));
        header.add(new JLabel("Người nhận: " + o.getReceiverName() + "   |   SĐT: " + o.getReceiverPhone()));
        header.add(new JLabel("Địa chỉ giao hàng: " + o.getReceiverAddress()));

        DefaultTableModel tm = new DefaultTableModel(
                new Object[]{"Mã SP", "Tên điện thoại (Snapshot)", "Đơn giá lúc mua", "SL", "Thành tiền"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (OrderItem item : detail.items()) {
            tm.addRow(new Object[]{
                    item.getProductId(),
                    item.getProductName(),
                    MoneyFormatter.formatVnd(item.getUnitPrice()),
                    item.getQuantity(),
                    MoneyFormatter.formatVnd(item.getLineTotal())
            });
        }
        JTable table = new JTable(tm);
        table.setRowHeight(28);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel totalLbl = new JLabel("Tổng thanh toán: " + MoneyFormatter.formatVnd(o.getTotalAmount()));
        totalLbl.setFont(UiTheme.FONT_BRAND);
        totalLbl.setForeground(UiTheme.PRIMARY);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        JButton closeBtn = UiTheme.navButton("Đóng");
        closeBtn.addActionListener(e -> dispose());
        right.add(closeBtn);

        footer.add(totalLbl, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);
        root.add(new JScrollPane(table), BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
    }
}
