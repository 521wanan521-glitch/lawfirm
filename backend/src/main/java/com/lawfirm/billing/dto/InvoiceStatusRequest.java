package com.lawfirm.billing.dto;

import com.lawfirm.billing.InvoiceStatus;
import jakarta.validation.constraints.NotNull;

public record InvoiceStatusRequest(
        @NotNull(message = "状态不能为空") InvoiceStatus status
) {
}
