package com.lawfirm.assistant;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 联网搜索配置（AI 助手 search_web 工具使用）。
 * 免注册方案：依次尝试多个公开搜索源，第一个有结果即返回。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.web-search")
public class WebSearchProperties {

    /** 是否启用联网搜索工具 */
    private boolean enabled = true;

    /** 搜索源顺序（前面的优先）：sogou / bing / duckduckgo */
    private List<String> engines = List.of("sogou", "bing", "duckduckgo");

    /** 返回给模型的最大结果条数 */
    private int maxResults = 5;

    /** 单个搜索源的请求超时（秒） */
    private int timeoutSeconds = 12;
}
