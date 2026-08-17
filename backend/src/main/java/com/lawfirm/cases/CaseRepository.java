package com.lawfirm.cases;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface CaseRepository extends JpaRepository<Case, Long>, JpaSpecificationExecutor<Case> {

    long countByClientId(Long clientId);

    long countByCaseNoStartingWith(String prefix);

    long countByFilingDateBetween(LocalDate start, LocalDate end);

    long countByStatus(CaseStatus status);

    @Query("select c.type as type, count(c) as cnt from Case c group by c.type")
    List<Object[]> countGroupByType();
}
