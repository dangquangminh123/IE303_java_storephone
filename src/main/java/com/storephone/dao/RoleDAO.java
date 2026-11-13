package com.storephone.dao;

import com.storephone.model.Role;
import java.util.List;
import java.util.Optional;

public interface RoleDAO {
    Optional<Role> findById(Long id);

    Optional<Role> findByName(String name);

    List<Role> findAll();
}
