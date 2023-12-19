package com.mmo.mmoserver.auth.repo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepo extends JpaRepository<LoginHistoryEntity, Long> {
    // Additional custom queries can be added here
}
