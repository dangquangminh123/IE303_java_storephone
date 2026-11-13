package com.storephone.view;

import com.storephone.model.Order;
import com.storephone.service.OrderService;
import com.storephone.util.UiTaskRunner;
import com.storephone.view.table.OrderTableModel;
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
import javax.swing.SwingUtilities;

public class OrderHistoryPanel extends JPanel {
    private final OrderService orderService;
    private final OrderTableModel tableModel = new OrderTableModel();
    private final JTable table = new JTable(tableModel);

    public OrderHistoryPanel(OrderService orderService) {
        super(new BorderLayout(12, 12));
        this.orderService = orderService;
        setBackground(UiTheme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        JLabel title = new JLabel("Lịch Sử Đơn Hàng Của Tôi");
        title.setFont(UiTheme.FONT_TITLE);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton detailBtn = UiTheme.primaryButton("Xem chi tiết đơn hàng");
        JButton refreshBtn = UiTheme.navButton("Làm mới");
        actions.add(detailBtn);
        actions.add(refreshBtn);

        top.add(title, BorderLayout.NORTH);
        top.add(actions, BorderLayout.SOUTH);

        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        detailBtn.addActionListener(e -> openSelectedDetail());
        refreshBtn.addActionListener(e -> reloadHistory());
    }

    public void reloadHistory() {
        UiTaskRunner.runAsync(
                orderService::getMyOrders,
                tableModel::setRows,
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Lỗi tải lịch sử", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void openSelectedDetail() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đơn hàng để xem chi tiết.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Order o = tableModel.getAt(row);
        if (o == null) {
            return;
        }
        UiTaskRunner.runAsync(
                () -> orderService.getOrderDetail(o.getId()),
                detail -> new OrderDetailDialog(SwingUtilities.getWindowAncestor(this), detail).setVisible(true),
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE)
        );
    }
}
