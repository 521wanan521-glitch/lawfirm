package com.lawfirm.cases.dto;

import com.lawfirm.cases.CaseStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CaseProgressView(
        Long id,
        String content,
        LocalDate progressDate,
        Long userId,
        String userName,
        CaseStatus newStatus,
        LocalDateTime createdAt
) {
}
