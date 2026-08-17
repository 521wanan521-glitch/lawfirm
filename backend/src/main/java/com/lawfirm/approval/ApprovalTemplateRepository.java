package com.lawfirm.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalTemplateRepository extends JpaRepository<ApprovalTemplate, Long> {

    List<ApprovalTemplate> findByEnabledTrueOrderByCreatedAtAsc();
}
