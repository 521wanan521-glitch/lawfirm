package com.lawfirm.cases;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CaseRepository extends JpaRepository<Case, Long>, JpaSpecificationExecutor<Case> {

    long countByClientId(Long clientId);

    long countByCaseNoStartingWith(String prefix);

    /** 查某前缀下字典序最大的案号（如 LF2026-0009） */
    @Query("select max(c.caseNo) from Case c where c.caseNo like :prefix")
    String findMaxCaseNo(@Param("prefix") String prefix);

    long countByFilingDateBetween(LocalDate start, LocalDate end);

    long countByStatus(CaseStatus status);

    @Query("select c.type as type, count(c) as cnt from Case c group by c.type")
    List<Object[]> countGroupByType();
}
