package com.lawfirm.client.dto;

import jakarta.validation.constraints.NotBlank;

public record ContactRequest(
        @NotBlank(message = "联系人姓名不能为空") String name,
        String phone,
        String email,
        String position,
        Boolean primaryContact
) {
}
