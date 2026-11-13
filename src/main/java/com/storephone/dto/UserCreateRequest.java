package com.storephone.dto;

import java.nio.file.Path;
import java.util.Arrays;

public class UserCreateRequest {
    private final String email;
    private final char[] password;
    private final String fullName;
    private final String phone;
    private final String address;
    private final String roleName;
    private final Path avatarSourceFile;
    private final boolean active;

    public UserCreateRequest(
            String email,
            char[] password,
            String fullName,
            String phone,
            String address,
            String roleName,
            Path avatarSourceFile,
            boolean active
    ) {
        this.email = email;
        this.password = password == null ? new char[0] : password.clone();
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.roleName = roleName;
        this.avatarSourceFile = avatarSourceFile;
        this.active = active;
    }

    public String getEmail() {
        return email;
    }

    public char[] getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getRoleName() {
        return roleName;
    }

    public Path getAvatarSourceFile() {
        return avatarSourceFile;
    }

    public boolean isActive() {
        return active;
    }

    public void clearSensitive() {
        Arrays.fill(password, '\0');
    }
}
