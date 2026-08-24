package com.lawfirm.client.dto;

import com.lawfirm.client.Client;

import java.time.LocalDateTime;

public record ClientView(
        Long id,
        String name,
        Client.Type type,
        String idNumber,
        String industry,
        String address,
        String phone,
        String email,
        Client.Level level,
        String source,
        Long ownerId,
        String ownerName,
        String remark,
        Boolean consultant,
        long contactCount,
        long caseCount,
        LocalDateTime createdAt
) {
}
