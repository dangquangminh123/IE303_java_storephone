package com.storephone.view;

import com.storephone.controller.NavigationController;
import com.storephone.controller.NavigationRoutes;
import com.storephone.service.HealthCheckService;
import com.storephone.util.UiTaskRunner;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class HomeOverviewPanel extends JPanel {
    private final HealthCheckService healthCheckService;
    private final Consumer<HealthCheckService.StatusSnapshot> statusCallback;
    private final JLabel dbSummaryLabel;
    private final JButton refreshHealthBtn;

    public HomeOverviewPanel(
            HealthCheckService healthCheckService,
            NavigationController nav,
            Consumer<HealthCheckService.StatusSnapshot> statusCallback
    ) {
        super(new BorderLayout(16, 16));
        this.healthCheckService = healthCheckService;
        this.statusCallback = statusCallback;

        setBackground(UiTheme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel hero = new JPanel(new BorderLayout(12, 12));
        hero.setBackground(UiTheme.CARD_BG);
        hero.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)
        ));

        JLabel title = new JLabel("Chào mừng đến với StorePhone Desktop (Java 17 Swing + MySQL)");
        title.setFont(UiTheme.FONT_TITLE);
        title.setForeground(UiTheme.TEXT_PRIMARY);

        dbSummaryLabel = new JLabel("Đang kiểm tra trạng thái kết nối cơ sở dữ liệu MySQL...");
        dbSummaryLabel.setFont(UiTheme.FONT_BODY);
        dbSummaryLabel.setForeground(UiTheme.TEXT_MUTED);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);

        JButton catalogBtn = UiTheme.primaryButton("Khám phá danh mục điện thoại");
        catalogBtn.addActionListener(e -> nav.navigateTo(NavigationRoutes.CATALOG));

        refreshHealthBtn = UiTheme.navButton("Kiểm tra lại kết nối MySQL");
        refreshHealthBtn.addActionListener(e -> triggerHealthCheck());

        actions.add(catalogBtn);
        actions.add(refreshHealthBtn);

        hero.add(title, BorderLayout.NORTH);
        hero.add(dbSummaryLabel, BorderLayout.CENTER);
        hero.add(actions, BorderLayout.SOUTH);

        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 16));
        cards.setOpaque(false);
        cards.add(infoCard("Kiến trúc chuẩn MVC", "Tách biệt hoàn toàn View (Swing) – Controller – Service – DAO (JDBC PreparedStatement)."));
        cards.add(infoCard("Bảo mật & Phân quyền", "Mật khẩu băm BCrypt (work factor 12), kiểm soát truy cập 2 lớp cho GUEST / USER / ADMIN."));
        cards.add(infoCard("Giao dịch Toàn vẹn", "Đặt hàng & Hủy đơn thực thi trên cùng JDBC Connection với commit / rollback nguyên tử."));

        add(hero, BorderLayout.NORTH);
        add(cards, BorderLayout.CENTER);
    }

    public void triggerHealthCheck() {
        refreshHealthBtn.setEnabled(false);
        dbSummaryLabel.setText("Đang kiểm tra kết nối MySQL trên luồng nền (SwingWorker)...");
        UiTaskRunner.runAsync(
                healthCheckService::checkStatus,
                snapshot -> {
                    refreshHealthBtn.setEnabled(true);
                    dbSummaryLabel.setText(snapshot.message());
                    dbSummaryLabel.setForeground(snapshot.connected() ? UiTheme.SUCCESS : UiTheme.DANGER);
                    if (statusCallback != null) {
                        statusCallback.accept(snapshot);
                    }
                },
                err -> {
                    refreshHealthBtn.setEnabled(true);
                    dbSummaryLabel.setText("Không thể kết nối MySQL: " + err.getMessage());
                    dbSummaryLabel.setForeground(UiTheme.DANGER);
                }
        );
    }

    private static JPanel infoCard(String heading, String body) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(UiTheme.CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        JLabel h = new JLabel(heading);
        h.setFont(UiTheme.FONT_SECTION);
        h.setForeground(UiTheme.PRIMARY);
        JLabel b = new JLabel("<html><div style='width:240px;'>" + body + "</div></html>");
        b.setFont(UiTheme.FONT_BODY);
        b.setForeground(UiTheme.TEXT_MUTED);
        p.add(h, BorderLayout.NORTH);
        p.add(b, BorderLayout.CENTER);
        return p;
    }
}
