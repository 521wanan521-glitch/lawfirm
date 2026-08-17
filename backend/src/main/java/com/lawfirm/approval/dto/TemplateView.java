package com.lawfirm.approval.dto;

import com.lawfirm.approval.ApprovalType;

public record TemplateView(
        Long id,
        String name,
        ApprovalType type,
        String description,
        Boolean enabled
) {
}
