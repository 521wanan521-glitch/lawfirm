package com.lawfirm.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lawfirm.common.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * DeepSeek（OpenAI 兼容）流式客户端。
 * 仅使用 JDK 内置 HttpClient + Jackson，不引入额外依赖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeepSeekClient {

    private final ObjectMapper mapper;
    private final DeepSeekProperties props;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /** 一次流式对话中模型要求调用的单个工具 */
    public record ToolCall(String id, String name, String arguments) {
    }

    /** 流式对话的聚合结果 */
    public record StreamResult(String content, List<ToolCall> toolCalls) {
    }

    /**
     * 发起一次流式对话。
     *
     * @param messages 完整消息列表（system + 历史 + 当前 user）
     * @param tools    工具定义（可为空数组）
     * @param onDelta  收到文本增量时回调（用于向前端逐字推送）
     * @return 聚合后的完整文本与工具调用列表
     */
    public StreamResult stream(List<Map<String, Object>> messages, JsonNode tools, Consumer<String> onDelta) {
        if (!StringUtils.hasText(props.getApiKey())) {
            throw new BizException("未配置 DeepSeek API Key（环境变量 DEEPSEEK_API_KEY）");
        }

        ObjectNode body = mapper.createObjectNode();
        body.put("model", props.getModel());
        body.put("stream", true);
        body.put("temperature", props.getTemperature());
        body.put("max_tokens", props.getMaxTokens());
        ArrayNode msgs = body.putArray("messages");
        for (Map<String, Object> m : messages) {
            msgs.add(mapper.valueToTree(m));
        }
        body.set("tools", tools == null ? mapper.createArrayNode() : tools);
        body.put("tool_choice", "auto");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.getBaseUrl() + "/chat/completions"))
                .timeout(Duration.ofSeconds(600))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + props.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<java.io.InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("调用 DeepSeek 被中断");
        } catch (Exception e) {
            throw new BizException("调用 DeepSeek 失败：" + e.getMessage());
        }

        if (response.statusCode() != 200) {
            String err = readAll(response.body());
            log.error("DeepSeek 接口返回 {}: {}", response.statusCode(), err);
            throw new BizException("DeepSeek 接口调用失败(" + response.statusCode() + ")：" + truncate(err, 500));
        }

        StringBuilder content = new StringBuilder();
        Map<Integer, ToolCallAccum> accum = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                if (!line.startsWith("data:")) continue;
                String payload = line.substring(5).trim();
                if ("[DONE]".equals(payload)) break;
                try {
                    JsonNode node = mapper.readTree(payload);
                    JsonNode choices = node.path("choices");
                    if (!choices.isArray() || choices.isEmpty()) continue;
                    JsonNode delta = choices.get(0).path("delta");
                    if (delta.hasNonNull("content")) {
                        String text = delta.get("content").asText();
                        if (!text.isEmpty()) {
                            content.append(text);
                            if (onDelta != null) onDelta.accept(text);
                        }
                    }
                    if (delta.has("tool_calls")) {
                        for (JsonNode tc : delta.get("tool_calls")) {
                            int index = tc.path("index").asInt();
                            ToolCallAccum a = accum.computeIfAbsent(index, k -> new ToolCallAccum());
                            if (tc.hasNonNull("id")) a.id = tc.get("id").asText();
                            JsonNode fn = tc.path("function");
                            if (fn.hasNonNull("name")) a.name = fn.get("name").asText();
                            if (fn.hasNonNull("arguments")) a.arguments.append(fn.get("arguments").asText());
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析 DeepSeek 流式响应失败：{}", payload, e);
                }
            }
        } catch (Exception e) {
            throw new BizException("读取 DeepSeek 流式响应失败：" + e.getMessage());
        }

        List<ToolCall> toolCalls = new ArrayList<>();
        for (ToolCallAccum a : accum.values()) {
            if (StringUtils.hasText(a.name)) {
                toolCalls.add(new ToolCall(a.id, a.name, a.arguments.toString()));
            }
        }
        return new StreamResult(content.toString(), toolCalls);
    }

    private static class ToolCallAccum {
        String id;
        String name;
        final StringBuilder arguments = new StringBuilder();
    }

    private String readAll(java.io.InputStream in) {
        try {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
