package com.lawfirm.approval;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, Long> {

    Page<ApprovalInstance> findByApplicantIdOrderByCreatedAtDesc(Long applicantId, Pageable pageable);

    Page<ApprovalInstance> findByApplicantIdAndStatusOrderByCreatedAtDesc(Long applicantId, ApprovalStatus status, Pageable pageable);

    Page<ApprovalInstance> findByApproverIdOrderByCreatedAtDesc(Long approverId, Pageable pageable);

    Page<ApprovalInstance> findByApproverIdAndStatusOrderByCreatedAtDesc(Long approverId, ApprovalStatus status, Pageable pageable);

    Page<ApprovalInstance> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ApprovalInstance> findByStatusOrderByCreatedAtDesc(ApprovalStatus status, Pageable pageable);

    long countByStatus(ApprovalStatus status);
}
