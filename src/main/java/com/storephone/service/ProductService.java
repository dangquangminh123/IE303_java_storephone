package com.storephone.service;

import com.storephone.dao.ProductDAO;
import com.storephone.dto.PageResult;
import com.storephone.dto.ProductFilterCriteria;
import com.storephone.dto.ProductSaveRequest;
import com.storephone.exception.ResourceNotFoundException;
import com.storephone.exception.ValidationException;
import com.storephone.model.Product;
import com.storephone.util.ImageStorage;
import com.storephone.util.InputValidator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class ProductService {
    private final ProductDAO productDAO;
    private final ImageStorage imageStorage;
    private final AuthorizationService authorizationService;

    public ProductService(
            ProductDAO productDAO,
            ImageStorage imageStorage,
            AuthorizationService authorizationService
    ) {
        this.productDAO = Objects.requireNonNull(productDAO);
        this.imageStorage = Objects.requireNonNull(imageStorage);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    public PageResult<Product> searchPublicCatalog(ProductFilterCriteria criteria) {
        ProductFilterCriteria safe = criteria == null ? new ProductFilterCriteria() : criteria;
        safe.setIncludeInactive(false);
        return productDAO.search(safe);
    }

    public Product getActiveProductDetail(Long productId) {
        if (productId == null) {
            throw new ValidationException("ID sản phẩm không hợp lệ.");
        }
        Product product = productDAO.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy điện thoại ID = " + productId));
        if (!product.isActive()) {
            throw new ResourceNotFoundException("Điện thoại này hiện đã ngừng kinh doanh.");
        }
        return product;
    }

    public List<String> getAvailableFactories() {
        return productDAO.findDistinctFactories();
    }

    public PageResult<Product> searchProductsForAdmin(String keyword, int page, int pageSize) {
        authorizationService.requireAdmin();
        ProductFilterCriteria criteria = new ProductFilterCriteria();
        criteria.setKeyword(keyword);
        criteria.setIncludeInactive(true);
        criteria.setPage(page);
        criteria.setPageSize(pageSize);
        return productDAO.search(criteria);
    }

    public Product getProductForAdmin(Long productId) {
        authorizationService.requireAdmin();
        if (productId == null) {
            throw new ValidationException("ID sản phẩm không hợp lệ.");
        }
        return productDAO.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy điện thoại ID = " + productId));
    }

    public Product createProduct(ProductSaveRequest request) {
        authorizationService.requireAdmin();
        Product validated = validateAndBuildProduct(new Product(), request);
        String storedImage = null;
        try {
            if (request.imageSourceFile() != null) {
                storedImage = imageStorage.saveImage(request.imageSourceFile(), "products");
                validated.setImagePath(storedImage);
            }
            return productDAO.insert(validated);
        } catch (RuntimeException ex) {
            if (storedImage != null) {
                imageStorage.deleteQuietly(storedImage);
            }
            throw ex;
        }
    }

    public Product updateProduct(Long productId, ProductSaveRequest request) {
        authorizationService.requireAdmin();
        Product existing = getProductForAdmin(productId);
        validateAndBuildProduct(existing, request);

        String oldImage = existing.getImagePath();
        String newImage = null;
        try {
            if (request.imageSourceFile() != null) {
                newImage = imageStorage.saveImage(request.imageSourceFile(), "products");
                existing.setImagePath(newImage);
            }
            productDAO.update(existing);
            if (newImage != null && oldImage != null && !oldImage.equals(newImage)) {
                imageStorage.deleteQuietly(oldImage);
            }
            return getProductForAdmin(productId);
        } catch (RuntimeException ex) {
            if (newImage != null) {
                imageStorage.deleteQuietly(newImage);
            }
            throw ex;
        }
    }

    public void setProductActiveStatus(Long productId, boolean active) {
        authorizationService.requireAdmin();
        getProductForAdmin(productId);
        productDAO.updateActiveStatus(productId, active);
    }

    /**
     * Trả về true nếu xóa cứng thành công, false nếu chuyển sang ngừng bán (xóa mềm) do sản phẩm đã phát sinh trong giỏ/đơn hàng.
     */
    public boolean deactivateOrDeleteProduct(Long productId) {
        authorizationService.requireAdmin();
        Product existing = getProductForAdmin(productId);
        if (productDAO.isReferencedInCartOrOrders(productId)) {
            productDAO.updateActiveStatus(productId, false);
            return false;
        }
        String img = existing.getImagePath();
        productDAO.deleteById(productId);
        if (img != null) {
            imageStorage.deleteQuietly(img);
        }
        return true;
    }

    private static Product validateAndBuildProduct(Product target, ProductSaveRequest req) {
        if (req == null) {
            throw new ValidationException("Dữ liệu sản phẩm không hợp lệ.");
        }
        String name = InputValidator.requireNonBlank("name", req.name(), "Tên điện thoại");
        BigDecimal price = InputValidator.requireNonNegativePrice(req.price());
        int qty = InputValidator.requireNonNegativeStock(req.quantity());
        String factory = InputValidator.requireNonBlank("factory", req.factory(), "Hãng sản xuất");
        String purpose = InputValidator.requireNonBlank("target", req.target(), "Mục đích sử dụng");
        String shortDesc = InputValidator.requireNonBlank("shortDesc", req.shortDesc(), "Mô tả ngắn");
        String detailDesc = (req.detailDesc() == null || req.detailDesc().trim().isEmpty())
                ? shortDesc
                : req.detailDesc().trim();

        target.setName(name);
        target.setPrice(price);
        target.setQuantity(qty);
        target.setFactory(factory);
        target.setTarget(purpose);
        target.setShortDesc(shortDesc);
        target.setDetailDesc(detailDesc);
        target.setActive(req.active());
        return target;
    }
}

