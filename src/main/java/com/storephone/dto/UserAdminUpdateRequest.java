package com.storephone.dto;

import java.nio.file.Path;

public record UserAdminUpdateRequest(
        Long userId,
        String fullName,
        String phone,
        String address,
        String roleName,
        Path newAvatarSourceFile,
        boolean active
) {
}
