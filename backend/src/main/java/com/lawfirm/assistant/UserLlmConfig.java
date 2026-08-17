package com.lawfirm.assistant;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户自定义 LLM 配置（每个用户可配置自己的厂商与 API Key）。
 * API Key 以 AES-GCM 加密后存储（密钥由 JWT 密钥派生）。
 */
@Getter
@Setter
@Entity
@Table(name = "user_llm_config", indexes = {
        @Index(name = "idx_llm_user", columnList = "userId", unique = true)
})
public class UserLlmConfig extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    /** 厂商标识：deepseek / qwen / glm / kimi / openai / custom */
    @Column(nullable = false, length = 50)
    private String provider;

    /** 加密后的 API Key（Base64） */
    @Column(nullable = false, length = 1000)
    private String apiKeyEnc;

    /** OpenAI 兼容接口地址，如 https://api.deepseek.com */
    @Column(nullable = false, length = 300)
    private String baseUrl;

    @Column(nullable = false, length = 100)
    private String model;
}
