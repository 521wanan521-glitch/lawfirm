package com.lawfirm.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    Page<Document> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    List<Document> findByCaseId(Long caseId);
}
