package com.storephone.config;

import com.storephone.controller.AuthController;
import com.storephone.controller.NavigationController;
import com.storephone.dao.CartDAO;
import com.storephone.dao.DashboardDAO;
import com.storephone.dao.OrderDAO;
import com.storephone.dao.ProductDAO;
import com.storephone.dao.RoleDAO;
import com.storephone.dao.UserDAO;
import com.storephone.dao.jdbc.JdbcCartDAO;
import com.storephone.dao.jdbc.JdbcDashboardDAO;
import com.storephone.dao.jdbc.JdbcOrderDAO;
import com.storephone.dao.jdbc.JdbcProductDAO;
import com.storephone.dao.jdbc.JdbcRoleDAO;
import com.storephone.dao.jdbc.JdbcUserDAO;
import com.storephone.security.AppSession;
import com.storephone.service.AuthService;
import com.storephone.service.AuthorizationService;
import com.storephone.service.CartService;
import com.storephone.service.CheckoutService;
import com.storephone.service.DashboardService;
import com.storephone.service.HealthCheckService;
import com.storephone.service.OrderService;
import com.storephone.service.ProductService;
import com.storephone.service.UserService;
import com.storephone.util.ImageStorage;

/**
 * Khởi tạo và gắn kết tập trung các thành phần ứng dụng theo Constructor Injection (không dùng Spring IoC).
 */
public class AppContext {
    private final DatabaseConfig databaseConfig;
    private final ConnectionFactory connectionFactory;
    private final AppSession appSession;
    private final ImageStorage imageStorage;

    private final RoleDAO roleDAO;
    private final UserDAO userDAO;
    private final ProductDAO productDAO;
    private final CartDAO cartDAO;
    private final OrderDAO orderDAO;
    private final DashboardDAO dashboardDAO;

    private final AuthorizationService authorizationService;
    private final AuthService authService;
    private final UserService userService;
    private final ProductService productService;
    private final CartService cartService;
    private final CheckoutService checkoutService;
    private final OrderService orderService;
    private final DashboardService dashboardService;
    private final HealthCheckService healthCheckService;
    private final NavigationController navigationController;
    private final AuthController authController;

    public AppContext(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
        this.connectionFactory = new ConnectionFactory(databaseConfig);
        this.appSession = new AppSession();
        this.imageStorage = new ImageStorage(databaseConfig.getImagesDirectory());

        this.roleDAO = new JdbcRoleDAO(connectionFactory);
        this.userDAO = new JdbcUserDAO(connectionFactory);
        this.productDAO = new JdbcProductDAO(connectionFactory);
        this.cartDAO = new JdbcCartDAO(connectionFactory);
        this.orderDAO = new JdbcOrderDAO(connectionFactory);
        this.dashboardDAO = new JdbcDashboardDAO(connectionFactory);

        this.authorizationService = new AuthorizationService(appSession);
        this.authService = new AuthService(userDAO, roleDAO, cartDAO, appSession);
        this.userService = new UserService(userDAO, roleDAO, cartDAO, imageStorage, appSession, authorizationService);
        this.productService = new ProductService(productDAO, imageStorage, authorizationService);
        this.cartService = new CartService(cartDAO, productDAO, appSession, authorizationService);
        this.checkoutService = new CheckoutService(connectionFactory, cartDAO, productDAO, orderDAO, appSession, authorizationService);
        this.orderService = new OrderService(connectionFactory, orderDAO, productDAO, appSession, authorizationService);
        this.dashboardService = new DashboardService(dashboardDAO, authorizationService);
        this.healthCheckService = new HealthCheckService(connectionFactory, roleDAO, userDAO, productDAO);
        this.navigationController = new NavigationController(authorizationService);
        this.authController = new AuthController(authService, navigationController);
    }

    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public AppSession getAppSession() {
        return appSession;
    }

    public ImageStorage getImageStorage() {
        return imageStorage;
    }

    public RoleDAO getRoleDAO() {
        return roleDAO;
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    public ProductDAO getProductDAO() {
        return productDAO;
    }

    public CartDAO getCartDAO() {
        return cartDAO;
    }

    public OrderDAO getOrderDAO() {
        return orderDAO;
    }

    public AuthorizationService getAuthorizationService() {
        return authorizationService;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public UserService getUserService() {
        return userService;
    }

    public ProductService getProductService() {
        return productService;
    }

    public CartService getCartService() {
        return cartService;
    }

    public CheckoutService getCheckoutService() {
        return checkoutService;
    }

    public OrderService getOrderService() {
        return orderService;
    }

    public DashboardService getDashboardService() {
        return dashboardService;
    }

    public HealthCheckService getHealthCheckService() {
        return healthCheckService;
    }

    public NavigationController getNavigationController() {
        return navigationController;
    }

    public AuthController getAuthController() {
        return authController;
    }
}

