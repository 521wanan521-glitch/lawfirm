package com.lawfirm.assistant.dto;

import java.time.LocalDateTime;

public record SessionView(
        Long id,
        String title,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt
) {
}
