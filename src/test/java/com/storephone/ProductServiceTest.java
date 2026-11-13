package com.storephone;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.storephone.config.ConnectionFactory;
import com.storephone.config.DatabaseConfig;
import com.storephone.dao.ProductDAO;
import com.storephone.dao.jdbc.JdbcProductDAO;
import com.storephone.dto.ProductSaveRequest;
import com.storephone.dto.UserSession;
import com.storephone.exception.AuthorizationException;
import com.storephone.exception.ResourceNotFoundException;
import com.storephone.exception.ValidationException;
import com.storephone.model.Product;
import com.storephone.model.Role;
import com.storephone.security.AppSession;
import com.storephone.service.AuthorizationService;
import com.storephone.service.ProductService;
import com.storephone.util.DatabaseInitializer;
import com.storephone.util.ImageStorage;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProductServiceTest {

    @Test
    void shouldEnforceAdminAuthValidationImageUploadAndSafeDelete(@TempDir Path tempDir) throws Exception {
        ConnectionFactory cf = new ConnectionFactory(DatabaseConfig.load());
        assumeTrue(cf.ping(), "Bỏ qua khi MySQL chưa khởi chạy.");
        DatabaseInitializer.initializeSchema(cf, false);

        ProductDAO productDAO = new JdbcProductDAO(cf);
        ImageStorage imageStorage = new ImageStorage(tempDir.resolve("images"));
        AppSession session = new AppSession();
        ProductService productService = new ProductService(productDAO, imageStorage, new AuthorizationService(session));

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ProductSaveRequest validReq = new ProductSaveRequest(
                "Galaxy S25 Ultra " + suffix,
                new BigDecimal("31990000"),
                "Flagship camera AI",
                "Màn hình Dynamic AMOLED 2X, Snapdragon 8 Elite",
                12,
                "Samsung",
                "Chụp ảnh / Quay phim",
                null,
                true
        );

        // TC-04: USER gọi ProductService.createProduct -> AuthorizationException
        session.login(new UserSession(2L, "user@test.vn", "Khách", Role.USER));
        assertThrows(AuthorizationException.class, () -> productService.createProduct(validReq));

        // Đăng nhập ADMIN
        session.login(new UserSession(1L, "admin@test.vn", "Admin", Role.ADMIN));

        // TC-05: ADMIN thêm sản phẩm giá -1 -> ValidationException, không ghi DB
        ProductSaveRequest negPriceReq = new ProductSaveRequest(
                "Lỗi Giá", new BigDecimal("-1"), "Mô tả", "Chi tiết", 5, "Apple", "Mỏng nhẹ", null, true);
        assertThrows(ValidationException.class, () -> productService.createProduct(negPriceReq));

        Path imgSrc = tempDir.resolve("phone.png");
        ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "png", imgSrc.toFile());

        Product created = null;
        try {
            created = productService.createProduct(new ProductSaveRequest(
                    "Galaxy S25 Ultra " + suffix, new BigDecimal("31990000"),
                    "Flagship camera AI", "Chi tiết đầy đủ", 12,
                    "Samsung", "Chụp ảnh / Quay phim", imgSrc, true));
            assertNotNull(created.getId());
            assertTrue(created.getImagePath().startsWith("products/"));
            assertTrue(Files.isRegularFile(imageStorage.resolvePath(created.getImagePath())));

            // Ngừng bán -> ẩn khỏi Catalog công khai (P06-03)
            productService.setProductActiveStatus(created.getId(), false);
            final Long pid = created.getId();
            assertThrows(ResourceNotFoundException.class, () -> productService.getActiveProductDetail(pid));

            // Xóa cứng thành công khi chưa phát sinh trong giỏ/đơn hàng
            assertTrue(productService.deactivateOrDeleteProduct(pid));
            created = null;
        } finally {
            if (created != null && created.getId() != null) {
                productDAO.deleteById(created.getId());
            }
        }
    }
}
