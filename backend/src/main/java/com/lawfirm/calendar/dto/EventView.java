package com.lawfirm.calendar.dto;

import com.lawfirm.calendar.EventType;

import java.time.LocalDateTime;
import java.util.List;

public record EventView(
        Long id,
        String title,
        EventType type,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        String description,
        Long caseId,
        Long creatorId,
        String creatorName,
        List<Long> participantIds,
        List<String> participantNames,
        LocalDateTime createdAt
) {
}
