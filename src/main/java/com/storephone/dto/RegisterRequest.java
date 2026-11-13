package com.storephone.dto;

import java.util.Arrays;

public class RegisterRequest {
    private final String fullName;
    private final String email;
    private final String phone;
    private final String address;
    private final char[] password;
    private final char[] confirmPassword;

    public RegisterRequest(
            String fullName,
            String email,
            String phone,
            String address,
            char[] password,
            char[] confirmPassword
    ) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.password = password == null ? new char[0] : password.clone();
        this.confirmPassword = confirmPassword == null ? new char[0] : confirmPassword.clone();
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public char[] getPassword() {
        return password;
    }

    public char[] getConfirmPassword() {
        return confirmPassword;
    }

    public void clearSensitive() {
        Arrays.fill(password, '\0');
        Arrays.fill(confirmPassword, '\0');
    }
}
