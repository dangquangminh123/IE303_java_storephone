package com.storephone.dto;

import java.util.Arrays;

public class LoginRequest {
    private final String email;
    private final char[] password;

    public LoginRequest(String email, char[] password) {
        this.email = email;
        this.password = password == null ? new char[0] : password.clone();
    }

    public String getEmail() {
        return email;
    }

    public char[] getPassword() {
        return password;
    }

    public void clearSensitive() {
        Arrays.fill(password, '\0');
    }
}
