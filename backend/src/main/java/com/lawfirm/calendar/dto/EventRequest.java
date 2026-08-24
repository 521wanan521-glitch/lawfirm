package com.lawfirm.calendar.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lawfirm.calendar.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record EventRequest(
        @NotBlank(message = "标题不能为空") String title,
        @NotNull(message = "日程类型不能为空") EventType type,
        @NotNull(message = "开始时间不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
        String location,
        String description,
        Long caseId,
        List<Long> participantIds
) {
}
