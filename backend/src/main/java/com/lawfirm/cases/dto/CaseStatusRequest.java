package com.lawfirm.cases.dto;

import com.lawfirm.cases.CaseStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CaseStatusRequest(
        @NotNull(message = "状态不能为空") CaseStatus status,
        String result,
        LocalDate closeDate
) {
}
