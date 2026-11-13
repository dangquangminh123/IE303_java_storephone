package com.storephone.dto;

import java.nio.file.Path;

public record ProfileUpdateRequest(
        String fullName,
        String phone,
        String address,
        Path newAvatarSourceFile
) {
}
