package com.lawfirm.client;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByClientIdOrderByCreatedAtAsc(Long clientId);

    long countByClientId(Long clientId);

    void deleteByClientId(Long clientId);
}
