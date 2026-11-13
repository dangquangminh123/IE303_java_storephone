package com.storephone.util;

import com.storephone.exception.ValidationException;
import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class InputValidator {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^0[0-9]{8,10}$");

    private InputValidator() {
    }

    public static String requireNonBlank(String field, String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field, label + " không được để trống.");
        }
        return value.trim();
    }

    public static String validateEmail(String email) {
        String trimmed = requireNonBlank("email", email, "Email").toLowerCase();
        if (trimmed.length() > 190 || !EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("email", "Định dạng email không hợp lệ.");
        }
        return trimmed;
    }

    public static void validatePassword(String password, int minLength) {
        if (password == null || password.length() < minLength) {
            throw new ValidationException("password", "Mật khẩu phải có tối thiểu " + minLength + " ký tự.");
        }
    }

    public static String validatePhone(String phone, boolean required) {
        if (phone == null || phone.trim().isEmpty()) {
            if (required) {
                throw new ValidationException("phone", "Số điện thoại không được để trống.");
            }
            return null;
        }
        String trimmed = phone.trim().replace(" ", "");
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("phone", "Số điện thoại phải bắt đầu bằng số 0 và gồm 9–11 chữ số.");
        }
        return trimmed;
    }

    public static BigDecimal requireNonNegativePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("price", "Giá sản phẩm phải lớn hơn hoặc bằng 0.");
        }
        return price;
    }

    public static int requireNonNegativeStock(int quantity) {
        if (quantity < 0) {
            throw new ValidationException("quantity", "Số lượng tồn kho không được âm.");
        }
        return quantity;
    }
}
