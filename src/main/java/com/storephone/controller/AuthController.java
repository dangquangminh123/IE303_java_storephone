package com.storephone.controller;

import com.storephone.dto.LoginRequest;
import com.storephone.dto.RegisterRequest;
import com.storephone.dto.UserSession;
import com.storephone.model.User;
import com.storephone.service.AuthService;
import java.util.Objects;

public class AuthController {
    private final AuthService authService;
    private final NavigationController navigationController;

    public AuthController(AuthService authService, NavigationController navigationController) {
        this.authService = Objects.requireNonNull(authService);
        this.navigationController = Objects.requireNonNull(navigationController);
    }

    public UserSession login(LoginRequest request) {
        return authService.login(request);
    }

    public void navigateAfterLogin(UserSession session) {
        if (session != null && session.isAdmin()) {
            navigationController.navigateTo(NavigationRoutes.ADMIN_DASHBOARD);
        } else {
            navigationController.navigateTo(NavigationRoutes.HOME);
        }
    }

    public User register(RegisterRequest request) {
        return authService.register(request);
    }

    public void logout() {
        authService.logout();
        navigationController.navigateTo(NavigationRoutes.HOME);
    }
}
