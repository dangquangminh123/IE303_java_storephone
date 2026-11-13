package com.storephone.dao;

import com.storephone.dto.PageResult;
import com.storephone.dto.ProductFilterCriteria;
import com.storephone.model.Product;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface ProductDAO {
    Optional<Product> findById(Long id);

    Optional<Product> findByIdForUpdate(Connection conn, Long id);

    PageResult<Product> search(ProductFilterCriteria criteria);

    List<String> findDistinctFactories();

    long countAllActive();

    Product insert(Product product);

    void update(Product product);

    void updateActiveStatus(Long productId, boolean active);

    boolean deductStock(Connection conn, Long productId, int quantity);

    void restoreStock(Connection conn, Long productId, int quantity);

    boolean isReferencedInCartOrOrders(Long productId);

    void deleteById(Long productId);
}
