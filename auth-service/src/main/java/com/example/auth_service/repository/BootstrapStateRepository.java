package com.example.auth_service.repository;

import com.example.auth_service.entity.BootstrapState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BootstrapStateRepository extends JpaRepository<BootstrapState, Long> {
    boolean existsByBootstrapKey(String bootstrapKey);
}
