package com.storephone.util;

import com.storephone.exception.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MoneyFormatter {
    private static final Locale VI_LOCALE = Locale.forLanguageTag("vi-VN");

    private MoneyFormatter() {
    }

    public static String formatVnd(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount.setScale(0, RoundingMode.HALF_UP);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(VI_LOCALE);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(safe) + " ₫";
    }

    public static BigDecimal parseVnd(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            throw new ValidationException("price", "Giá sản phẩm không được để trống.");
        }
        String normalized = rawInput.trim()
                .replace("₫", "")
                .replace("VND", "")
                .replace("vnd", "")
                .replace(".", "")
                .replace(",", "")
                .replace(" ", "");
        try {
            BigDecimal value = new BigDecimal(normalized);
            if (value.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("price", "Giá sản phẩm không được nhỏ hơn 0.");
            }
            return value.setScale(0, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new ValidationException("price", "Giá tiền không hợp lệ.");
        }
    }
}
