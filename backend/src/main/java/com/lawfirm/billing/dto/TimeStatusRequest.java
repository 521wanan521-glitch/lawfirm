package com.lawfirm.billing.dto;

import com.lawfirm.billing.TimeEntryStatus;
import jakarta.validation.constraints.NotNull;

public record TimeStatusRequest(
        @NotNull(message = "状态不能为空") TimeEntryStatus status
) {
}
