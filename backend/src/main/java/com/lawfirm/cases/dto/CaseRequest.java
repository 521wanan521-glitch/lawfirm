package com.lawfirm.cases.dto;

import com.lawfirm.cases.CaseType;
import com.lawfirm.cases.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CaseRequest(
        @NotNull(message = "客户不能为空") Long clientId,
        @NotBlank(message = "案件名称不能为空") String title,
        @NotNull(message = "案件类型不能为空") CaseType type,
        Priority priority,
        @NotNull(message = "主办律师不能为空") Long leadLawyerId,
        List<Long> coLawyerIds,
        String court,
        BigDecimal caseAmount,
        LocalDate filingDate,
        String description,
        BigDecimal fee
) {
}
