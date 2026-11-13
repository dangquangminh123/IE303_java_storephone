package com.storephone.dto;

import java.math.BigDecimal;
import java.nio.file.Path;

public record ProductSaveRequest(
        String name,
        BigDecimal price,
        String shortDesc,
        String detailDesc,
        int quantity,
        String factory,
        String target,
        Path imageSourceFile,
        boolean active
) {
}
