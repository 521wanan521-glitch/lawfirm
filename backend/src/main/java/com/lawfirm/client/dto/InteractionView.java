package com.lawfirm.client.dto;

import com.lawfirm.client.Interaction;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record InteractionView(
        Long id,
        Interaction.Type type,
        String content,
        LocalDate nextFollowDate,
        Long userId,
        String userName,
        LocalDateTime createdAt
) {
}
