package com.lawfirm.document.dto;

import com.lawfirm.document.DocCategory;

import java.time.LocalDateTime;

public record DocumentView(
        Long id,
        String name,
        Long size,
        String contentType,
        DocCategory category,
        Long caseId,
        Long clientId,
        Long folderId,
        String folderName,
        Integer version,
        Long uploadedBy,
        String uploaderName,
        String description,
        LocalDateTime createdAt
) {
}
