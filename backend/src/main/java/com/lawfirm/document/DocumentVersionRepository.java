package com.lawfirm.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    List<DocumentVersion> findByDocumentIdOrderByVersionDesc(Long documentId);

    Optional<DocumentVersion> findByDocumentIdAndVersion(Long documentId, Integer version);

    void deleteByDocumentId(Long documentId);
}
