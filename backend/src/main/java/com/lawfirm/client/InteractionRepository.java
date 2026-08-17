package com.lawfirm.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    Page<Interaction> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    void deleteByClientId(Long clientId);
}
