package com.lawfirm.client.dto;

import com.lawfirm.client.Interaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record InteractionRequest(
        @NotNull(message = "跟进方式不能为空") Interaction.Type type,
        @NotBlank(message = "跟进内容不能为空") String content,
        LocalDate nextFollowDate
) {
}
