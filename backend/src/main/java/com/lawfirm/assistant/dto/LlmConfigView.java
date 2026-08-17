package com.lawfirm.assistant.dto;

/**
 * 用户 LLM 配置视图（API Key 仅返回掩码）
 */
public record LlmConfigView(
        boolean configured,
        String provider,
        String baseUrl,
        String model,
        String apiKeyMasked
) {
}
