package com.lawfirm.assistant;

import com.lawfirm.assistant.dto.LlmConfigRequest;
import com.lawfirm.assistant.dto.LlmConfigView;
import com.lawfirm.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户级 LLM 配置接口（AI 助手"模型设置"）
 */
@RestController
@RequestMapping("/assistant/llm-config")
@RequiredArgsConstructor
public class LlmConfigController {

    private final LlmConfigService llmConfigService;

    @GetMapping
    public ApiResponse<LlmConfigView> get() {
        return ApiResponse.ok(llmConfigService.get());
    }

    @PutMapping
    public ApiResponse<LlmConfigView> save(@Valid @RequestBody LlmConfigRequest request) {
        return ApiResponse.ok(llmConfigService.save(request));
    }

    @DeleteMapping
    public ApiResponse<Void> delete() {
        llmConfigService.delete();
        return ApiResponse.ok();
    }
}
