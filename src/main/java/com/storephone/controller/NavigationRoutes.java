package com.storephone.controller;

public final class NavigationRoutes {
    public static final String HOME = "HOME";
    public static final String CATALOG = "CATALOG";
    public static final String PRODUCT_DETAIL = "PRODUCT_DETAIL";
    public static final String LOGIN = "LOGIN";
    public static final String REGISTER = "REGISTER";
    public static final String CART = "CART";
    public static final String CHECKOUT = "CHECKOUT";
    public static final String ORDER_HISTORY = "ORDER_HISTORY";
    public static final String PROFILE = "PROFILE";

    public static final String ADMIN_DASHBOARD = "ADMIN_DASHBOARD";
    public static final String ADMIN_PRODUCTS = "ADMIN_PRODUCTS";
    public static final String ADMIN_USERS = "ADMIN_USERS";
    public static final String ADMIN_ORDERS = "ADMIN_ORDERS";

    private NavigationRoutes() {
    }

    public static boolean isAdminRoute(String route) {
        return ADMIN_DASHBOARD.equals(route)
                || ADMIN_PRODUCTS.equals(route)
                || ADMIN_USERS.equals(route)
                || ADMIN_ORDERS.equals(route);
    }

    public static boolean requiresLogin(String route) {
        return isAdminRoute(route)
                || CART.equals(route)
                || CHECKOUT.equals(route)
                || ORDER_HISTORY.equals(route)
                || PROFILE.equals(route);
    }
}
