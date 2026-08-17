package com.lawfirm.assistant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLlmConfigRepository extends JpaRepository<UserLlmConfig, Long> {

    Optional<UserLlmConfig> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
