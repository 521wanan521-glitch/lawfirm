package com.lawfirm.assistant;

import com.lawfirm.assistant.dto.LlmConfigRequest;
import com.lawfirm.assistant.dto.LlmConfigView;
import com.lawfirm.common.BizException;
import com.lawfirm.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 用户级 LLM 配置：每位用户可选厂商 + 填自己的 API Key（加密存储）。
 * 未配置时使用系统默认（app.deepseek.*）。
 */
@Service
@RequiredArgsConstructor
public class LlmConfigService {

    private final UserLlmConfigRepository repository;
    private final LlmCrypto crypto;

    /** 厂商预设（前端也有一份，仅用于兜底展示） */
    private static final Map<String, String> PROVIDER_NAMES = Map.of(
            "deepseek", "DeepSeek",
            "qwen", "通义千问",
            "glm", "智谱 GLM",
            "kimi", "Kimi 月之暗面",
            "openai", "OpenAI",
            "custom", "自定义");

    /** 当前用户的配置（未配置返回 null） */
    public LlmConfigView get() {
        UserLlmConfig c = repository.findByUserId(CurrentUser.id()).orElse(null);
        if (c == null) {
            return new LlmConfigView(false, "", "", "", "");
        }
        return new LlmConfigView(true, c.getProvider(), c.getBaseUrl(), c.getModel(),
                mask(crypto.decrypt(c.getApiKeyEnc(), c.getUserId())));
    }

    /** 保存（新增或覆盖）当前用户的配置 */
    @Transactional
    public LlmConfigView save(LlmConfigRequest req) {
        if (!StringUtils.hasText(req.apiKey()) || req.apiKey().trim().length() < 8) {
            throw new BizException("API Key 格式不正确");
        }
        if (!req.baseUrl().startsWith("http://") && !req.baseUrl().startsWith("https://")) {
            throw new BizException("接口地址必须以 http(s):// 开头");
        }
        Long me = CurrentUser.id();
        UserLlmConfig c = repository.findByUserId(me).orElseGet(UserLlmConfig::new);
        c.setUserId(me);
        c.setProvider(req.provider().trim());
        c.setBaseUrl(req.baseUrl().trim().replaceAll("/+$", ""));
        c.setModel(req.model().trim());
        c.setApiKeyEnc(crypto.encrypt(req.apiKey().trim(), me));
        c = repository.save(c);
        return new LlmConfigView(true, c.getProvider(), c.getBaseUrl(), c.getModel(),
                mask(crypto.decrypt(c.getApiKeyEnc(), me)));
    }

    /** 删除当前用户的配置（恢复系统默认） */
    @Transactional
    public void delete() {
        repository.deleteByUserId(CurrentUser.id());
    }

    /**
     * 解析当前用户实际生效的 LLM 目标（apiKey/baseUrl/model）。
     * 有用户配置则用用户配置，否则返回 null（表示用系统默认）。
     */
    public LlmTarget resolve() {
        UserLlmConfig c = repository.findByUserId(CurrentUser.id()).orElse(null);
        if (c == null) {
            return null;
        }
        String apiKey = crypto.decrypt(c.getApiKeyEnc(), c.getUserId());
        return new LlmTarget(apiKey, c.getBaseUrl(), c.getModel());
    }

    /** LLM 调用目标（覆盖系统默认配置） */
    public record LlmTarget(String apiKey, String baseUrl, String model) {
    }

    private String mask(String key) {
        if (key == null || key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
