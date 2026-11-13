package com.storephone.service;

import com.storephone.exception.AuthorizationException;
import com.storephone.security.AppSession;

public class AuthorizationService {
    private final AppSession appSession;

    public AuthorizationService(AppSession appSession) {
        this.appSession = appSession;
    }

    public void requireAuthenticated() {
        if (!appSession.isLoggedIn()) {
            throw new AuthorizationException("Vui lòng đăng nhập để tiếp tục sử dụng chức năng này.");
        }
    }

    public void requireAdmin() {
        requireAuthenticated();
        if (!appSession.isAdmin()) {
            throw new AuthorizationException("Bạn không có quyền truy cập chức năng Quản trị (ADMIN).");
        }
    }
}
