package com.lawfirm.billing.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record InvoiceRequest(
        @NotNull(message = "客户不能为空") Long clientId,
        @NotEmpty(message = "请至少选择一条工时记录") List<Long> timeEntryIds,
        @NotNull(message = "开票日期不能为空") LocalDate issueDate,
        LocalDate dueDate,
        String remark
) {
}
