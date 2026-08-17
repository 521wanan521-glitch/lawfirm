package com.lawfirm.approval.dto;

import com.lawfirm.approval.ApprovalStatus;
import com.lawfirm.approval.ApprovalType;

import java.time.LocalDateTime;

public record InstanceView(
        Long id,
        String templateName,
        ApprovalType type,
        String title,
        String content,
        Long applicantId,
        String applicantName,
        Long approverId,
        String approverName,
        ApprovalStatus status,
        String comment,
        LocalDateTime decidedAt,
        Long caseId,
        LocalDateTime createdAt
) {
}
