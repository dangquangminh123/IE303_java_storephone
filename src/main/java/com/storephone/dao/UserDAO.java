package com.storephone.dao;

import com.storephone.dto.PageResult;
import com.storephone.model.User;
import java.util.Optional;

public interface UserDAO {
    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long excludeUserId);

    PageResult<User> findPage(String keyword, int page, int pageSize);

    long countAll();

    long countActive();

    long countActiveAdmins();

    User insert(User user);

    void updateProfile(User user);

    void updateAdminFields(User user);

    void updateActiveStatus(Long userId, boolean active);
}
