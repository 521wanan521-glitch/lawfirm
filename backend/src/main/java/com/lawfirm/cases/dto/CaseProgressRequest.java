package com.lawfirm.cases.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CaseProgressRequest(
        @NotBlank(message = "记录内容不能为空") String content,
        @NotNull(message = "进程日期不能为空") LocalDate progressDate
) {
}
