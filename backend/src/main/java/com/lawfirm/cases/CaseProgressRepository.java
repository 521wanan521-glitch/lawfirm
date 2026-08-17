package com.lawfirm.cases;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseProgressRepository extends JpaRepository<CaseProgress, Long> {

    Page<CaseProgress> findByCaseIdOrderByProgressDateDesc(Long caseId, Pageable pageable);
}
