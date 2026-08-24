package com.lawfirm.client.dto;

import com.lawfirm.client.Client;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClientRequest(
        @NotBlank(message = "客户名称不能为空") String name,
        @NotNull(message = "客户类型不能为空") Client.Type type,
        String idNumber,
        String industry,
        String address,
        String phone,
        String email,
        Client.Level level,
        String source,
        Long ownerId,
        String remark,
        Boolean consultant
) {
}
