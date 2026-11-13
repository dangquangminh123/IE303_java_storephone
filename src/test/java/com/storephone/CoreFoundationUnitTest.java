package com.storephone;

import static org.junit.jupiter.api.Assertions.*;

import com.storephone.controller.NavigationController;
import com.storephone.controller.NavigationRoutes;
import com.storephone.dto.UserSession;
import com.storephone.exception.ValidationException;
import com.storephone.model.OrderStatus;
import com.storephone.model.Role;
import com.storephone.security.AppSession;
import com.storephone.security.PasswordHasher;
import com.storephone.service.AuthorizationService;
import com.storephone.util.ImageStorage;
import com.storephone.util.InputValidator;
import com.storephone.util.MoneyFormatter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CoreFoundationUnitTest {

    @Test
    void passwordHasherShouldHashAndVerifyBcrypt() {
        String hash = PasswordHasher.hash("MatKhauAnToan123");
        assertTrue(hash.startsWith("$2a$12$"));
        assertTrue(PasswordHasher.verify("MatKhauAnToan123", hash));
        assertFalse(PasswordHasher.verify("SaiMatKhau", hash));
    }

    @Test
    void inputValidatorShouldValidateVietnameseRules() {
        assertEquals("user@example.com", InputValidator.validateEmail("  USER@Example.com "));
        assertThrows(ValidationException.class, () -> InputValidator.validateEmail("khong-hop-le"));
        assertThrows(ValidationException.class, () -> InputValidator.validatePassword("1234567", 8));
        assertEquals("0912345678", InputValidator.validatePhone("0912345678", true));
        assertThrows(ValidationException.class, () -> InputValidator.validatePhone("abc123", true));
        assertEquals(3, InputValidator.requireNonNegativeStock(3));
        assertThrows(ValidationException.class, () -> InputValidator.requireNonNegativeStock(-1));
    }

    @Test
    void moneyFormatterShouldFormatVietnameseDong() {
        String formatted = MoneyFormatter.formatVnd(new BigDecimal("34990000"));
        assertTrue(formatted.contains("34.990.000"));
        assertTrue(formatted.endsWith("₫"));
    }

    @Test
    void orderStatusTransitionsShouldFollowBusinessRules() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED));
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.COMPLETED));
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.SHIPPING));
        assertTrue(OrderStatus.SHIPPING.canTransitionTo(OrderStatus.COMPLETED));
        assertFalse(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void navigationControllerShouldEnforceGuestUserAdminAccess() {
        AppSession session = new AppSession();
        AuthorizationService authService = new AuthorizationService(session);
        NavigationController nav = new NavigationController(authService);

        assertTrue(nav.navigateTo(NavigationRoutes.CATALOG));
        assertFalse(nav.navigateTo(NavigationRoutes.CART));
        assertFalse(nav.navigateTo(NavigationRoutes.ADMIN_DASHBOARD));

        session.login(new UserSession(10L, "khach@test.vn", "Khách Hàng", Role.USER));
        assertTrue(nav.navigateTo(NavigationRoutes.CART));
        assertFalse(nav.navigateTo(NavigationRoutes.ADMIN_USERS));

        session.login(new UserSession(1L, "admin@test.vn", "Quản Trị", Role.ADMIN));
        assertTrue(nav.navigateTo(NavigationRoutes.ADMIN_USERS));
    }

    @Test
    void imageStorageShouldCopyValidImageWithUuidName(@TempDir Path tempDir) throws Exception {
        Path sourceImg = tempDir.resolve("iphone.png");
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_RGB);
        javax.imageio.ImageIO.write(img, "png", sourceImg.toFile());

        ImageStorage storage = new ImageStorage(tempDir.resolve("data-images"));
        String storedPath = storage.saveImage(sourceImg, "products");

        assertTrue(storedPath.startsWith("products/"));
        assertTrue(storedPath.endsWith(".png"));
        assertTrue(Files.isRegularFile(storage.resolvePath(storedPath)));

        Path fakePng = tempDir.resolve("fake_text.png");
        Files.writeString(fakePng, "not-an-image-at-all");
        assertThrows(ValidationException.class, () -> storage.saveImage(fakePng, "products"));

        Path invalidTxt = tempDir.resolve("malicious.exe");
        Files.write(invalidTxt, new byte[]{1, 2});
        assertThrows(ValidationException.class, () -> storage.saveImage(invalidTxt, "products"));

        storage.deleteQuietly(storedPath);
        assertFalse(Files.exists(storage.resolvePath(storedPath)));
    }
}
