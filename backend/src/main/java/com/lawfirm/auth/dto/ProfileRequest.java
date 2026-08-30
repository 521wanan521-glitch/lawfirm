package com.lawfirm.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新个人资料请求
 */
public record ProfileRequest(
        @NotBlank(message = "姓名不能为空") String realName,
        String email,
        String phone
) {
}
