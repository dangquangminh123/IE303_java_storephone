package com.storephone.view;

import com.storephone.dto.DashboardMetrics;
import com.storephone.service.DashboardService;
import com.storephone.util.MoneyFormatter;
import com.storephone.util.UiTaskRunner;
import com.storephone.view.table.OrderTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class AdminDashboardPanel extends JPanel {
    private final DashboardService dashboardService;

    private final JLabel activeUsersVal = new JLabel("0");
    private final JLabel activeProductsVal = new JLabel("0");
    private final JLabel pendingOrdersVal = new JLabel("0");
    private final JLabel completedRevenueVal = new JLabel("0 ₫");

    private final OrderTableModel recentOrderModel = new OrderTableModel();
    private final DefaultTableModel topProductModel = new DefaultTableModel(
            new Object[]{"Mã SP", "Tên điện thoại", "SL đã bán", "Doanh thu"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    public AdminDashboardPanel(DashboardService dashboardService) {
        super(new BorderLayout(16, 16));
        this.dashboardService = dashboardService;
        setBackground(UiTheme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JLabel title = new JLabel("Bảng Điều Khiển Thống Kê (ADMIN)");
        title.setFont(UiTheme.FONT_TITLE);
        JButton refreshBtn = UiTheme.primaryButton("Làm mới số liệu");
        refreshBtn.addActionListener(e -> reloadMetrics());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(refreshBtn);
        topBar.add(title, BorderLayout.WEST);
        topBar.add(right, BorderLayout.EAST);

        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 14, 14));
        kpiGrid.setOpaque(false);
        kpiGrid.add(kpiCard("Người dùng hoạt động", activeUsersVal));
        kpiGrid.add(kpiCard("Điện thoại đang bán", activeProductsVal));
        kpiGrid.add(kpiCard("Đơn chờ duyệt (PENDING)", pendingOrdersVal));
        kpiGrid.add(kpiCard("Doanh thu hoàn tất", completedRevenueVal));

        JPanel headerArea = new JPanel(new BorderLayout(0, 14));
        headerArea.setOpaque(false);
        headerArea.add(topBar, BorderLayout.NORTH);
        headerArea.add(kpiGrid, BorderLayout.CENTER);

        JPanel tablesGrid = new JPanel(new GridLayout(1, 2, 16, 16));
        tablesGrid.setOpaque(false);
        tablesGrid.add(tableBox("5 Đơn Hàng Mới Nhất", new JTable(recentOrderModel)));
        tablesGrid.add(tableBox("Top 5 Điện Thoại Bán Chạy (Đơn COMPLETED)", new JTable(topProductModel)));

        add(headerArea, BorderLayout.NORTH);
        add(tablesGrid, BorderLayout.CENTER);
    }

    private static JPanel kpiCard(String caption, JLabel valueLabel) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBackground(UiTheme.CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));
        JLabel cap = new JLabel(caption);
        cap.setFont(UiTheme.FONT_SECTION);
        cap.setForeground(UiTheme.TEXT_MUTED);
        valueLabel.setFont(UiTheme.FONT_TITLE);
        valueLabel.setForeground(UiTheme.PRIMARY);
        p.add(cap, BorderLayout.NORTH);
        p.add(valueLabel, BorderLayout.CENTER);
        return p;
    }

    private static JPanel tableBox(String heading, JTable table) {
        table.setRowHeight(28);
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(UiTheme.CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));
        JLabel h = new JLabel(heading);
        h.setFont(UiTheme.FONT_SECTION);
        p.add(h, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    public void reloadMetrics() {
        UiTaskRunner.runAsync(
                dashboardService::getDashboardMetrics,
                this::applyMetrics,
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Lỗi tải Dashboard", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void applyMetrics(DashboardMetrics m) {
        activeUsersVal.setText(String.valueOf(m.activeUserCount()));
        activeProductsVal.setText(String.valueOf(m.activeProductCount()));
        pendingOrdersVal.setText(String.valueOf(m.pendingOrderCount()));
        completedRevenueVal.setText(MoneyFormatter.formatVnd(m.completedRevenue()));

        recentOrderModel.setRows(m.recentOrders());
        topProductModel.setRowCount(0);
        for (DashboardMetrics.TopProductStat tp : m.topProducts()) {
            topProductModel.addRow(new Object[]{
                    tp.productId(),
                    tp.productName(),
                    tp.totalSoldQuantity(),
                    MoneyFormatter.formatVnd(tp.totalRevenue())
            });
        }
    }
}
