package com.lawfirm.approval.dto;

import com.lawfirm.approval.ApprovalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TemplateRequest(
        @NotBlank(message = "模板名称不能为空") String name,
        @NotNull(message = "审批类型不能为空") ApprovalType type,
        String description
) {
}
