package com.lawfirm.assistant.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 保存用户 LLM 配置请求
 */
public record LlmConfigRequest(
        @NotBlank(message = "厂商不能为空") String provider,
        @NotBlank(message = "API Key 不能为空") String apiKey,
        @NotBlank(message = "接口地址不能为空") String baseUrl,
        @NotBlank(message = "模型名称不能为空") String model
) {
}
