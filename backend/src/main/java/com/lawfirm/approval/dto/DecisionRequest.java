package com.lawfirm.approval.dto;

import jakarta.validation.constraints.NotBlank;

public record DecisionRequest(
        @NotBlank(message = "审批意见不能为空") String comment
) {
}
