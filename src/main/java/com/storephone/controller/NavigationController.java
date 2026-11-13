package com.storephone.controller;

import com.storephone.exception.AuthorizationException;
import com.storephone.service.AuthorizationService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NavigationController {
    private final AuthorizationService authorizationService;
    private final Map<String, Runnable> routeEnterHooks = new ConcurrentHashMap<>();
    private Consumer<String> routeSwitcher;
    private BiConsumer<String, String> errorNotifier;
    private String currentRoute = NavigationRoutes.HOME;

    public NavigationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public void bindView(Consumer<String> routeSwitcher, BiConsumer<String, String> errorNotifier) {
        this.routeSwitcher = routeSwitcher;
        this.errorNotifier = errorNotifier;
    }

    public void onRouteShown(String route, Runnable hook) {
        if (route != null && hook != null) {
            routeEnterHooks.put(route, hook);
        }
    }

    public boolean navigateTo(String route) {
        try {
            if (NavigationRoutes.isAdminRoute(route)) {
                authorizationService.requireAdmin();
            } else if (NavigationRoutes.requiresLogin(route)) {
                authorizationService.requireAuthenticated();
            }
            this.currentRoute = route;
            if (routeSwitcher != null) {
                routeSwitcher.accept(route);
            }
            Runnable hook = routeEnterHooks.get(route);
            if (hook != null) {
                hook.run();
            }
            return true;
        } catch (AuthorizationException ex) {
            if (errorNotifier != null) {
                errorNotifier.accept("Từ chối truy cập", ex.getMessage());
            }
            if (!NavigationRoutes.isAdminRoute(route) && NavigationRoutes.requiresLogin(route) && routeSwitcher != null) {
                this.currentRoute = NavigationRoutes.LOGIN;
                routeSwitcher.accept(NavigationRoutes.LOGIN);
                Runnable loginHook = routeEnterHooks.get(NavigationRoutes.LOGIN);
                if (loginHook != null) {
                    loginHook.run();
                }
            }
            return false;
        }
    }

    public String getCurrentRoute() {
        return currentRoute;
    }
}

