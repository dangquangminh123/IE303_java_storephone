package com.storephone.security;

import com.storephone.dto.UserSession;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class AppSession {
    private volatile UserSession currentUser;
    private volatile int cartQuantity;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public void login(UserSession session) {
        login(session, 0);
    }

    public void login(UserSession session, int initialCartQuantity) {
        this.currentUser = Objects.requireNonNull(session, "Phiên đăng nhập không được rỗng.");
        this.cartQuantity = Math.max(0, initialCartQuantity);
        notifyListeners();
    }

    public void logout() {
        this.currentUser = null;
        this.cartQuantity = 0;
        notifyListeners();
    }

    public void updateCurrentUserProfile(String fullName) {
        UserSession existing = this.currentUser;
        if (existing != null && fullName != null && !fullName.isBlank()) {
            this.currentUser = new UserSession(
                    existing.userId(),
                    existing.email(),
                    fullName.trim(),
                    existing.roleName()
            );
            notifyListeners();
        }
    }

    public Optional<UserSession> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public int getCartQuantity() {
        return cartQuantity;
    }

    public void setCartQuantity(int cartQuantity) {
        this.cartQuantity = Math.max(0, cartQuantity);
        notifyListeners();
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public boolean isUser() {
        return currentUser != null && currentUser.isUser();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}

