package com.storephone.dto;

import com.storephone.model.Role;

public record UserSession(
        Long userId,
        String email,
        String fullName,
        String roleName
) {
    public boolean isAdmin() {
        return Role.ADMIN.equalsIgnoreCase(roleName);
    }

    public boolean isUser() {
        return Role.USER.equalsIgnoreCase(roleName);
    }
}
