package com.lawfirm.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InstanceRequest(
        @NotNull(message = "审批类型不能为空") Long templateId,
        @NotBlank(message = "标题不能为空") String title,
        @NotBlank(message = "申请内容不能为空") String content,
        @NotNull(message = "请选择审批人") Long approverId,
        Long caseId
) {
}
