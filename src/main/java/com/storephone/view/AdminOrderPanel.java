package com.storephone.view;

import com.storephone.dto.PageResult;
import com.storephone.model.Order;
import com.storephone.model.OrderStatus;
import com.storephone.service.OrderService;
import com.storephone.util.UiTaskRunner;
import com.storephone.view.table.OrderTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

public class AdminOrderPanel extends JPanel {
    private static final int PAGE_SIZE = 10;

    private final OrderService orderService;
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{
            "Tất cả trạng thái", "PENDING", "CONFIRMED", "SHIPPING", "COMPLETED", "CANCELLED"
    });
    private final OrderTableModel tableModel = new OrderTableModel();
    private final JTable table = new JTable(tableModel);
    private final JLabel pageLabel = new JLabel("Trang 1 / 1");
    private final JButton prevBtn = UiTheme.navButton("« Trước");
    private final JButton nextBtn = UiTheme.navButton("Sau »");

    private int currentPage = 1;
    private int totalPages = 1;

    public AdminOrderPanel(OrderService orderService) {
        super(new BorderLayout(12, 12));
        this.orderService = orderService;
        setBackground(UiTheme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        buildUi();
    }

    private void buildUi() {
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        JLabel title = new JLabel("Quản Lý Đơn Hàng (ADMIN)");
        title.setFont(UiTheme.FONT_TITLE);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        JButton detailBtn = UiTheme.navButton("Xem chi tiết");
        JButton confirmBtn = UiTheme.primaryButton("Xác nhận đơn");
        JButton shipBtn = UiTheme.navButton("Chuyển Giao hàng");
        JButton completeBtn = UiTheme.navButton("Hoàn tất đơn");
        JButton cancelBtn = UiTheme.navButton("Hủy đơn & Hoàn kho");
        JButton refreshBtn = UiTheme.navButton("Làm mới");

        toolbar.add(new JLabel("Trạng thái:"));
        toolbar.add(statusCombo);
        toolbar.add(detailBtn);
        toolbar.add(confirmBtn);
        toolbar.add(shipBtn);
        toolbar.add(completeBtn);
        toolbar.add(cancelBtn);
        toolbar.add(refreshBtn);

        top.add(title, BorderLayout.NORTH);
        top.add(toolbar, BorderLayout.SOUTH);

        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pager.setOpaque(false);
        pager.add(prevBtn);
        pager.add(pageLabel);
        pager.add(nextBtn);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(pager, BorderLayout.SOUTH);

        statusCombo.addActionListener(e -> { currentPage = 1; reloadPage(); });
        refreshBtn.addActionListener(e -> reloadPage());
        prevBtn.addActionListener(e -> { if (currentPage > 1) { currentPage--; reloadPage(); } });
        nextBtn.addActionListener(e -> { if (currentPage < totalPages) { currentPage++; reloadPage(); } });

        detailBtn.addActionListener(e -> withSelected(this::viewDetail));
        confirmBtn.addActionListener(e -> withSelected(o -> transition(o, OrderStatus.CONFIRMED)));
        shipBtn.addActionListener(e -> withSelected(o -> transition(o, OrderStatus.SHIPPING)));
        completeBtn.addActionListener(e -> withSelected(o -> transition(o, OrderStatus.COMPLETED)));
        cancelBtn.addActionListener(e -> withSelected(o -> transition(o, OrderStatus.CANCELLED)));
    }

    public void reloadPage() {
        int idx = statusCombo.getSelectedIndex();
        OrderStatus filter = idx <= 0 ? null : OrderStatus.valueOf(String.valueOf(statusCombo.getSelectedItem()));
        UiTaskRunner.runAsync(
                () -> orderService.searchOrdersForAdmin(filter, currentPage, PAGE_SIZE),
                this::applyPage,
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Lỗi tải đơn hàng", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void applyPage(PageResult<Order> page) {
        this.totalPages = page.getTotalPages();
        if (currentPage > totalPages) {
            currentPage = totalPages;
            reloadPage();
            return;
        }
        tableModel.setRows(page.getItems());
        pageLabel.setText("Trang " + page.getPage() + " / " + totalPages + " (Tổng: " + page.getTotalElements() + ")");
        prevBtn.setEnabled(page.getPage() > 1);
        nextBtn.setEnabled(page.getPage() < totalPages);
    }

    private void withSelected(java.util.function.Consumer<Order> action) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đơn hàng trong bảng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Order o = tableModel.getAt(row);
        if (o != null) {
            action.accept(o);
        }
    }

    private void viewDetail(Order o) {
        UiTaskRunner.runAsync(
                () -> orderService.getOrderDetail(o.getId()),
                detail -> new OrderDetailDialog(SwingUtilities.getWindowAncestor(this), detail).setVisible(true),
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void transition(Order o, OrderStatus next) {
        if (next == OrderStatus.CANCELLED) {
            int c = JOptionPane.showConfirmDialog(this,
                    "Xác nhận hủy đơn #" + o.getId() + " và hoàn trả tồn kho?",
                    "Xác nhận hủy đơn", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) {
                return;
            }
        }
        UiTaskRunner.runAsync(
                () -> orderService.updateOrderStatus(o.getId(), next),
                updated -> reloadPage(),
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Chuyển trạng thái không hợp lệ", JOptionPane.WARNING_MESSAGE)
        );
    }
}

