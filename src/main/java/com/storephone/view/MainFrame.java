package com.storephone.view;

import com.storephone.config.AppContext;
import com.storephone.controller.NavigationController;
import com.storephone.controller.NavigationRoutes;
import com.storephone.dto.UserSession;
import com.storephone.security.AppSession;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class MainFrame extends JFrame {
    private final AppSession session;
    private final NavigationController nav;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentCards = new JPanel(cardLayout);
    private final JPanel adminMenuSection = new JPanel();

    private final JLabel sessionLabel = new JLabel();
    private final JButton cartHeaderBtn = UiTheme.primaryButton("Giỏ hàng (0)");
    private final JButton loginBtn = UiTheme.primaryButton("Đăng nhập");
    private final JButton registerBtn = UiTheme.navButton("Đăng ký");
    private final JButton logoutBtn = UiTheme.navButton("Đăng xuất");
    private final JLabel statusBarLabel = new JLabel("Khởi tạo giao diện thành công.");
    private final HomeOverviewPanel homePanel;
    private final LoginPanel loginPanel;
    private final RegisterPanel registerPanel;
    private final ProfilePanel profilePanel;
    private final AdminUserPanel adminUserPanel;
    private final AdminProductPanel adminProductPanel;
    private final CatalogPanel catalogPanel;
    private final CartPanel cartPanel;
    private final CheckoutPanel checkoutPanel;
    private final OrderHistoryPanel orderHistoryPanel;
    private final AdminOrderPanel adminOrderPanel;
    private final AdminDashboardPanel adminDashboardPanel;

    public MainFrame(AppContext context) {
        super("StorePhone — Phần Mềm Quản Lý & Bán Điện Thoại");
        this.session = context.getAppSession();
        this.nav = context.getNavigationController();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 760);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        this.homePanel = new HomeOverviewPanel(
                context.getHealthCheckService(),
                nav,
                snap -> statusBarLabel.setText(snap.message())
        );
        this.loginPanel = new LoginPanel(context.getAuthController(), nav);
        this.registerPanel = new RegisterPanel(context.getAuthController(), nav, loginPanel::prefillEmail);
        this.profilePanel = new ProfilePanel(context.getUserService(), context.getImageStorage());
        this.adminUserPanel = new AdminUserPanel(context.getUserService(), context.getImageStorage());
        this.adminProductPanel = new AdminProductPanel(context.getProductService(), context.getImageStorage());
        this.catalogPanel = new CatalogPanel(
                context.getProductService(), context.getCartService(), context.getImageStorage(), session, nav);
        this.cartPanel = new CartPanel(context.getCartService(), nav);
        this.checkoutPanel = new CheckoutPanel(
                context.getCartService(), context.getCheckoutService(), context.getUserService(), nav);
        this.orderHistoryPanel = new OrderHistoryPanel(context.getOrderService());
        this.adminOrderPanel = new AdminOrderPanel(context.getOrderService());
        this.adminDashboardPanel = new AdminDashboardPanel(context.getDashboardService());

        nav.onRouteShown(NavigationRoutes.LOGIN, loginPanel::resetForm);
        nav.onRouteShown(NavigationRoutes.REGISTER, registerPanel::resetForm);
        nav.onRouteShown(NavigationRoutes.PROFILE, profilePanel::reloadProfile);
        nav.onRouteShown(NavigationRoutes.ADMIN_USERS, adminUserPanel::reloadPage);
        nav.onRouteShown(NavigationRoutes.ADMIN_PRODUCTS, adminProductPanel::reloadPage);
        nav.onRouteShown(NavigationRoutes.CATALOG, catalogPanel::reloadCatalog);
        nav.onRouteShown(NavigationRoutes.CART, cartPanel::reloadCart);
        nav.onRouteShown(NavigationRoutes.CHECKOUT, checkoutPanel::reloadCheckoutPreview);
        nav.onRouteShown(NavigationRoutes.ORDER_HISTORY, orderHistoryPanel::reloadHistory);
        nav.onRouteShown(NavigationRoutes.ADMIN_ORDERS, adminOrderPanel::reloadPage);
        nav.onRouteShown(NavigationRoutes.ADMIN_DASHBOARD, adminDashboardPanel::reloadMetrics);

        add(buildHeader(context), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildContentArea(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        nav.bindView(
                route -> cardLayout.show(contentCards, route),
                (title, msg) -> JOptionPane.showMessageDialog(this, msg, title, JOptionPane.WARNING_MESSAGE)
        );
        session.addChangeListener(this::refreshSessionUi);

        refreshSessionUi();
        homePanel.triggerHealthCheck();
    }

    private JPanel buildHeader(AppContext context) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(UiTheme.HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel brand = new JLabel("STOREPHONE DESKTOP");
        brand.setFont(UiTheme.FONT_BRAND);
        brand.setForeground(Color.WHITE);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        sessionLabel.setFont(UiTheme.FONT_BODY);
        sessionLabel.setForeground(new Color(226, 232, 240));

        cartHeaderBtn.addActionListener(e -> nav.navigateTo(NavigationRoutes.CART));
        loginBtn.addActionListener(e -> nav.navigateTo(NavigationRoutes.LOGIN));
        registerBtn.addActionListener(e -> nav.navigateTo(NavigationRoutes.REGISTER));
        logoutBtn.addActionListener(e -> context.getAuthController().logout());

        right.add(sessionLabel);
        right.add(cartHeaderBtn);
        right.add(loginBtn);
        right.add(registerBtn);
        right.add(logoutBtn);

        header.add(brand, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UiTheme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(235, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(16, 14, 16, 14)
        ));

        sidebar.add(sectionTitle("CỬA HÀNG ĐIỆN THOẠI"));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(sidebarItem("Trang chủ", NavigationRoutes.HOME));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(sidebarItem("Danh mục sản phẩm", NavigationRoutes.CATALOG));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(sidebarItem("Giỏ hàng của tôi", NavigationRoutes.CART));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(sidebarItem("Lịch sử đơn hàng", NavigationRoutes.ORDER_HISTORY));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(sidebarItem("Hồ sơ cá nhân", NavigationRoutes.PROFILE));

        sidebar.add(Box.createVerticalStrut(20));
        adminMenuSection.setLayout(new BoxLayout(adminMenuSection, BoxLayout.Y_AXIS));
        adminMenuSection.setOpaque(false);
        adminMenuSection.add(sectionTitle("QUẢN TRỊ HỆ THỐNG"));
        adminMenuSection.add(Box.createVerticalStrut(8));
        adminMenuSection.add(sidebarItem("Bảng điều khiển Admin", NavigationRoutes.ADMIN_DASHBOARD));
        adminMenuSection.add(Box.createVerticalStrut(6));
        adminMenuSection.add(sidebarItem("Quản lý Điện thoại", NavigationRoutes.ADMIN_PRODUCTS));
        adminMenuSection.add(Box.createVerticalStrut(6));
        adminMenuSection.add(sidebarItem("Quản lý Đơn hàng", NavigationRoutes.ADMIN_ORDERS));
        adminMenuSection.add(Box.createVerticalStrut(6));
        adminMenuSection.add(sidebarItem("Quản lý Người dùng", NavigationRoutes.ADMIN_USERS));

        sidebar.add(adminMenuSection);
        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JPanel buildContentArea() {
        contentCards.setBackground(UiTheme.CONTENT_BG);
        contentCards.add(homePanel, NavigationRoutes.HOME);
        contentCards.add(catalogPanel, NavigationRoutes.CATALOG);
        contentCards.add(loginPanel, NavigationRoutes.LOGIN);
        contentCards.add(registerPanel, NavigationRoutes.REGISTER);
        contentCards.add(cartPanel, NavigationRoutes.CART);
        contentCards.add(checkoutPanel, NavigationRoutes.CHECKOUT);
        contentCards.add(orderHistoryPanel, NavigationRoutes.ORDER_HISTORY);
        contentCards.add(profilePanel, NavigationRoutes.PROFILE);
        contentCards.add(adminDashboardPanel, NavigationRoutes.ADMIN_DASHBOARD);
        contentCards.add(adminProductPanel, NavigationRoutes.ADMIN_PRODUCTS);
        contentCards.add(adminOrderPanel, NavigationRoutes.ADMIN_ORDERS);
        contentCards.add(adminUserPanel, NavigationRoutes.ADMIN_USERS);
        return contentCards;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
        statusBarLabel.setFont(UiTheme.FONT_BODY);
        statusBarLabel.setForeground(UiTheme.TEXT_MUTED);
        bar.add(statusBarLabel, BorderLayout.WEST);
        return bar;
    }

    private void refreshSessionUi() {
        Optional<UserSession> current = session.getCurrentUser();
        if (current.isPresent()) {
            UserSession u = current.get();
            sessionLabel.setText("Xin chào, " + u.fullName() + " [" + u.roleName() + "]");
            cartHeaderBtn.setText("Giỏ hàng (" + session.getCartQuantity() + ")");
            cartHeaderBtn.setVisible(true);
            loginBtn.setVisible(false);
            registerBtn.setVisible(false);
            logoutBtn.setVisible(true);
            adminMenuSection.setVisible(u.isAdmin());
        } else {
            sessionLabel.setText("Chế độ: Khách vãng lai (GUEST)");
            cartHeaderBtn.setText("Giỏ hàng (0)");
            cartHeaderBtn.setVisible(false);
            loginBtn.setVisible(true);
            registerBtn.setVisible(true);
            logoutBtn.setVisible(false);
            adminMenuSection.setVisible(false);
        }
        revalidate();
        repaint();
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiTheme.FONT_SECTION);
        label.setForeground(UiTheme.TEXT_MUTED);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JButton sidebarItem(String label, String route) {
        JButton btn = UiTheme.navButton(label);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.addActionListener(e -> nav.navigateTo(route));
        return btn;
    }
}
