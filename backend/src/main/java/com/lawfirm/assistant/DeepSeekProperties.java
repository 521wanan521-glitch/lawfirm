package com.lawfirm.assistant;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 大模型相关配置
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.deepseek")
public class DeepSeekProperties {

    /** API Key（环境变量 DEEPSEEK_API_KEY 注入） */
    private String apiKey = "";

    /** 接口地址（OpenAI 兼容），默认官方地址 */
    private String baseUrl = "https://api.deepseek.com";

    /** 模型名称：deepseek-chat 支持工具调用（deepseek-reasoner 不支持 function calling） */
    private String model = "deepseek-chat";

    /** 单轮工具调用最大循环次数，防止死循环 */
    private int maxToolRounds = 6;

    /** 单次回答最大 token */
    private int maxTokens = 4096;

    /** 采样温度 */
    private double temperature = 0.3;
}
