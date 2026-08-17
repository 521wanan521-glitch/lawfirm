package com.lawfirm.document.dto;

import java.time.LocalDateTime;

public record DocumentVersionView(
        Integer version,
        Long size,
        Long uploadedBy,
        String uploaderName,
        String remark,
        LocalDateTime createdAt
) {
}
