package com.lawfirm.assistant.dto;

import java.time.LocalDateTime;

public record MessageView(
        Long id,
        String role,
        String content,
        LocalDateTime createdAt
) {
}
