package com.lawfirm.assistant.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 发起对话请求
 */
public record ChatRequest(
        /** 会话 id，为空则新建会话 */
        Long sessionId,
        @NotBlank(message = "消息不能为空") String message,
        /** 可选：附加案件上下文 */
        Long caseId,
        /** 可选：附加客户上下文 */
        Long clientId
) {
}
