package com.storephone;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.storephone.config.ConnectionFactory;
import com.storephone.config.DatabaseConfig;
import com.storephone.dao.ProductDAO;
import com.storephone.dao.jdbc.JdbcProductDAO;
import com.storephone.dto.PageResult;
import com.storephone.dto.ProductFilterCriteria;
import com.storephone.model.Product;
import com.storephone.security.AppSession;
import com.storephone.service.AuthorizationService;
import com.storephone.service.ProductService;
import com.storephone.util.DatabaseInitializer;
import com.storephone.util.ImageStorage;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProductCatalogFilterTest {

    @Test
    void shouldSupportGuestCatalogMultiFilterPriceRangesSortAndPagination(@TempDir Path tempDir) throws Exception {
        ConnectionFactory cf = new ConnectionFactory(DatabaseConfig.load());
        assumeTrue(cf.ping(), "Bỏ qua khi MySQL chưa khởi chạy.");
        DatabaseInitializer.initializeSchema(cf, false);

        ProductDAO productDAO = new JdbcProductDAO(cf);
        AppSession guestSession = new AppSession(); // GUEST chưa đăng nhập
        ProductService productService = new ProductService(
                productDAO, new ImageStorage(tempDir), new AuthorizationService(guestSession));

        String tag = "CAT_" + UUID.randomUUID().toString().substring(0, 6);
        List<Long> createdIds = new ArrayList<>();

        try {
            // Tạo 11 máy active + 1 máy inactive để kiểm thử TC-06..TC-10
            for (int i = 1; i <= 11; i++) {
                Product p = new Product();
                p.setName(tag + " Phone " + i);
                p.setFactory(i <= 5 ? "BrandAlpha" : (i <= 9 ? "BrandBeta" : "BrandGamma"));
                p.setTarget("Chơi game / Cấu hình cao");
                p.setPrice(BigDecimal.valueOf(i * 3_000_000L)); // 3M .. 33M
                p.setQuantity(10);
                p.setShortDesc("Máy test " + i);
                p.setDetailDesc("Chi tiết " + i);
                p.setActive(true);
                createdIds.add(productDAO.insert(p).getId());
            }
            Product inactive = new Product();
            inactive.setName(tag + " Hidden");
            inactive.setFactory("BrandAlpha");
            inactive.setTarget("Chơi game / Cấu hình cao");
            inactive.setPrice(new BigDecimal("9000000"));
            inactive.setQuantity(5);
            inactive.setShortDesc("Ẩn");
            inactive.setActive(false);
            createdIds.add(productDAO.insert(inactive).getId());

            // TC-06 & TC-10: GUEST xem danh sách chỉ thấy 11 máy active; pageSize=10 -> trang 1 (10), trang 2 (1)
            ProductFilterCriteria page1Crit = new ProductFilterCriteria();
            page1Crit.setKeyword(tag);
            page1Crit.setPageSize(10);
            page1Crit.setPage(1);
            PageResult<Product> page1 = productService.searchPublicCatalog(page1Crit);
            assertEquals(11, page1.getTotalElements());
            assertEquals(10, page1.getItems().size());
            assertEquals(2, page1.getTotalPages());

            page1Crit.setPage(2);
            PageResult<Product> page2 = productService.searchPublicCatalog(page1Crit);
            assertEquals(1, page2.getItems().size());

            // TC-07: Từ khóa không tồn tại -> total=0, items rỗng
            ProductFilterCriteria emptyCrit = new ProductFilterCriteria();
            emptyCrit.setKeyword("KHONG_TON_TAI_" + tag);
            PageResult<Product> emptyRes = productService.searchPublicCatalog(emptyCrit);
            assertEquals(0, emptyRes.getTotalElements());
            assertTrue(emptyRes.getItems().isEmpty());

            // TC-08 & TC-09: Lọc 2 hãng (BrandAlpha, BrandBeta) + 2 khoảng giá OR (<=6M HOẶC >=24M) + Sort PRICE_DESC
            ProductFilterCriteria combo = new ProductFilterCriteria();
            combo.setKeyword(tag);
            combo.setFactories(List.of("BrandAlpha", "BrandBeta"));
            combo.setPriceRanges(List.of(
                    new ProductFilterCriteria.PriceRange(null, new BigDecimal("6000000")),
                    new ProductFilterCriteria.PriceRange(new BigDecimal("24000000"), null)
            ));
            combo.setSortBy(ProductFilterCriteria.SortBy.PRICE_DESC);
            PageResult<Product> filtered = productService.searchPublicCatalog(combo);
            // BrandAlpha: i=1(3M),2(6M); BrandBeta: i=8(24M),9(27M) -> tổng 4 máy theo thứ tự 27M, 24M, 6M, 3M
            assertEquals(4, filtered.getTotalElements());
            assertEquals(new BigDecimal("27000000"), filtered.getItems().get(0).getPrice().stripTrailingZeros().setScale(0));
            assertEquals(new BigDecimal("3000000"), filtered.getItems().get(3).getPrice().stripTrailingZeros().setScale(0));
        } finally {
            for (Long id : createdIds) {
                productDAO.deleteById(id);
            }
        }
    }
}
