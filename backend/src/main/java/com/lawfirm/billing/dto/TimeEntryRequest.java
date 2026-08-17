package com.lawfirm.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimeEntryRequest(
        @NotNull(message = "案件不能为空") Long caseId,
        @NotNull(message = "工作日期不能为空") LocalDate workDate,
        @NotNull(message = "工时不能为空") @DecimalMin(value = "0.1", message = "工时需大于 0") BigDecimal hours,
        @DecimalMin(value = "0", message = "费率不能为负") BigDecimal rate,
        @NotBlank(message = "工作内容不能为空") String description
) {
}
