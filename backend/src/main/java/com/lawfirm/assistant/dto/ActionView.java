package com.lawfirm.assistant.dto;

import java.time.LocalDateTime;

public record ActionView(
        Long id,
        Long sessionId,
        String toolName,
        String arguments,
        String summary,
        String status,
        LocalDateTime createdAt
) {
}
