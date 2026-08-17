package com.lawfirm.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.assistant.dto.ChatRequest;
import com.lawfirm.assistant.dto.MessageView;
import com.lawfirm.assistant.dto.SessionView;
import com.lawfirm.cases.CaseService;
import com.lawfirm.cases.dto.CaseView;
import com.lawfirm.client.ClientService;
import com.lawfirm.client.dto.ClientView;
import com.lawfirm.common.BizException;
import com.lawfirm.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * AI 助手对话编排：会话管理 + DeepSeek 流式对话 + 工具调用循环 + SSE 推送。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final ObjectMapper mapper;
    private final DeepSeekClient deepSeekClient;
    private final DeepSeekProperties props;
    private final AssistantToolService toolService;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final CaseService caseService;
    private final ClientService clientService;
    private final ExecutorService assistantExecutor;

    /**
     * 发起流式对话（SSE）。
     * 事件：meta / delta / tool / tool_result / done / error
     */
    public SseEmitter chat(ChatRequest request) {
        Long userId = CurrentUser.id();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SseEmitter emitter = new SseEmitter(600000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> log.warn("SSE 连接异常：{}", e.getMessage()));

        assistantExecutor.execute(() -> {
            SecurityContextHolder.getContext().setAuthentication(auth);
            try {
                doChat(request, userId, emitter);
            } catch (Exception e) {
                log.error("AI 对话异常", e);
                send(emitter, "error", Map.of("message", e.getMessage() == null ? "对话失败" : e.getMessage()));
            } finally {
                SecurityContextHolder.clearContext();
                emitter.complete();
            }
        });
        return emitter;
    }

    private void doChat(ChatRequest request, Long userId, SseEmitter emitter) {
        ChatSession session = resolveSession(request, userId);
        send(emitter, "meta", Map.of("sessionId", session.getId(), "title", session.getTitle()));

        String userContent = withContext(request);
        List<ChatMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());

        // 持久化用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(session.getId());
        userMsg.setRole("user");
        userMsg.setContent(request.message());
        messageRepository.save(userMsg);
        session.setLastMessageAt(LocalDateTime.now());
        sessionRepository.save(session);

        // 组装 LLM 消息：system + 历史 + 当前（含上下文）
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(systemMessage());
        for (ChatMessage m : history) {
            messages.add(roleMessage(m.getRole(), m.getContent()));
        }
        messages.add(roleMessage("user", userContent));

        JsonNode tools = toolService.definitions();
        String finalContent = "";
        boolean unresolvedTools = false;

        int maxRounds = Math.max(1, props.getMaxToolRounds());
        for (int round = 0; round < maxRounds; round++) {
            DeepSeekClient.StreamResult result = deepSeekClient.stream(messages, tools,
                    delta -> send(emitter, "delta", Map.of("content", delta)));

            // 追加助手消息
            Map<String, Object> assistantMsg = new LinkedHashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", result.content() == null ? "" : result.content());
            if (!result.toolCalls().isEmpty()) {
                List<Map<String, Object>> tcs = new ArrayList<>();
                for (int i = 0; i < result.toolCalls().size(); i++) {
                    DeepSeekClient.ToolCall tc = result.toolCalls().get(i);
                    Map<String, Object> tcObj = new LinkedHashMap<>();
                    tcObj.put("id", tc.id());
                    tcObj.put("type", "function");
                    Map<String, Object> fn = new LinkedHashMap<>();
                    fn.put("name", tc.name());
                    fn.put("arguments", tc.arguments());
                    tcObj.put("function", fn);
                    tcs.add(tcObj);
                }
                assistantMsg.put("tool_calls", tcs);
            }
            messages.add(assistantMsg);

            if (result.toolCalls().isEmpty()) {
                finalContent = result.content();
                unresolvedTools = false;
                break;
            }

            // 执行工具
            for (DeepSeekClient.ToolCall tc : result.toolCalls()) {
                sendToolEvent(emitter, "tool", tc.id(), tc.name(), tc.arguments(), null, null);
                AssistantToolService.ToolResult tr = toolService.execute(tc.name(), tc.arguments());
                sendToolEvent(emitter, "tool_result", tc.id(), tc.name(), null, tr.ok(), tr.json());

                Map<String, Object> toolMsg = new LinkedHashMap<>();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", tc.id());
                toolMsg.put("content", tr.json());
                messages.add(toolMsg);
            }
            unresolvedTools = true;
        }

        // 兜底：达到最大轮数仍未给出最终回答时，不带工具再请求一次
        if (unresolvedTools || !StringUtils.hasText(finalContent)) {
            DeepSeekClient.StreamResult r = deepSeekClient.stream(messages, mapper.createArrayNode(),
                    delta -> send(emitter, "delta", Map.of("content", delta)));
            finalContent = r.content();
        }

        if (StringUtils.hasText(finalContent)) {
            ChatMessage assistantMsg = new ChatMessage();
            assistantMsg.setSessionId(session.getId());
            assistantMsg.setRole("assistant");
            assistantMsg.setContent(finalContent);
            messageRepository.save(assistantMsg);
            session.setLastMessageAt(LocalDateTime.now());
            sessionRepository.save(session);
        }

        send(emitter, "done", Map.of());
    }

    // ==================== 会话管理 ====================

    public List<SessionView> sessions() {
        Long userId = CurrentUser.id();
        return sessionRepository.findByUserIdOrderByLastMessageAtDesc(userId).stream()
                .map(s -> new SessionView(s.getId(), s.getTitle(), s.getLastMessageAt(), s.getCreatedAt()))
                .toList();
    }

    public List<MessageView> messages(Long sessionId) {
        ChatSession session = getOwnSession(sessionId);
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
                .map(m -> new MessageView(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
    }

    public void deleteSession(Long sessionId) {
        ChatSession session = getOwnSession(sessionId);
        messageRepository.deleteBySessionId(session.getId());
        sessionRepository.delete(session);
    }

    public SessionView rename(Long sessionId, String title) {
        ChatSession session = getOwnSession(sessionId);
        if (StringUtils.hasText(title)) {
            session.setTitle(title.trim());
            session = sessionRepository.save(session);
        }
        return new SessionView(session.getId(), session.getTitle(), session.getLastMessageAt(), session.getCreatedAt());
    }

    // ==================== 私有方法 ====================

    private ChatSession resolveSession(ChatRequest request, Long userId) {
        ChatSession session = null;
        if (request.sessionId() != null) {
            session = sessionRepository.findById(request.sessionId()).orElse(null);
            if (session != null && !session.getUserId().equals(userId)) {
                session = null;
            }
        }
        if (session == null) {
            session = new ChatSession();
            session.setUserId(userId);
            session.setTitle(shortTitle(request.message()));
            session.setLastMessageAt(LocalDateTime.now());
            session = sessionRepository.save(session);
        }
        return session;
    }

    private ChatSession getOwnSession(Long sessionId) {
        ChatSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BizException("会话不存在"));
        if (!s.getUserId().equals(CurrentUser.id())) {
            throw new BizException(403, "无权访问该会话");
        }
        return s;
    }

    private String withContext(ChatRequest request) {
        if (request.caseId() == null && request.clientId() == null) {
            return request.message();
        }
        StringBuilder sb = new StringBuilder();
        try {
            if (request.caseId() != null) {
                CaseView c = caseService.detail(request.caseId());
                sb.append("【当前上下文·案件】").append(c.caseNo()).append(" ").append(c.title())
                        .append("（客户：").append(c.clientName()).append("，状态：").append(c.status()).append("）\n");
            }
            if (request.clientId() != null) {
                ClientView cl = clientService.detail(request.clientId());
                sb.append("【当前上下文·客户】").append(cl.name())
                        .append(cl.phone() == null ? "" : "（" + cl.phone() + "）").append("\n");
            }
        } catch (Exception ignored) {
        }
        sb.append(request.message());
        return sb.toString();
    }

    private Map<String, Object> roleMessage(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content == null ? "" : content);
        return m;
    }

    private Map<String, Object> systemMessage() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String prompt = """
                你是「律所数字化办公系统」内置的智能助手，服务于律师事务所的律师、合伙人、助理与行政人员。你的职责是帮助用户高效处理案件、客户、工时计费、日程、审批、文档、知识库与经营报表相关工作。

                你可以通过工具（function calling）查询或操作系统内的真实数据。请遵守以下规则：
                1. 优先调用工具获取真实数据后再回答，不要凭空猜测案号、金额、日期、状态等事实信息。
                2. 涉及"我的"视角时（我的案件、我的工时、我的日程、待我审批），直接使用相应工具，工具会自动以当前登录人身份过滤数据。
                3. 写操作（记录工时、创建日程、发起审批、记录案件进展）执行前，先简要告知用户你将要做什么；执行后清晰汇报结果（如新工时 id、新日程、审批单号等）。
                4. 不得编造法律条文、判例或案件事实；知识库未检索到依据时，明确说明"未在知识库中检索到相关内容"。
                5. 涉及起草文书时，基于案件/客户的真实信息撰写，并提醒用户由执业律师最终审核。
                6. 回答使用简体中文，专业、简洁、条理清晰，适当使用 Markdown 列表或表格。
                7. 注意保护当事人隐私，不要主动输出无关人员的敏感信息。

                当前时间：""" + now;
        return roleMessage("system", prompt);
    }

    private void sendToolEvent(SseEmitter emitter, String event, String id, String name,
                               String arguments, Boolean ok, String result) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("name", name);
        if (arguments != null) {
            data.put("arguments", arguments);
        }
        if (ok != null) {
            data.put("ok", ok);
        }
        if (result != null) {
            data.put("result", result);
        }
        send(emitter, event, data);
    }

    private void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(mapper.writeValueAsString(data)));
        } catch (IOException e) {
            log.warn("SSE 推送失败（客户端可能已断开）：{}", e.getMessage());
        } catch (Exception e) {
            log.warn("SSE 数据序列化失败", e);
        }
    }

    private String shortTitle(String msg) {
        if (!StringUtils.hasText(msg)) {
            return "新对话";
        }
        String t = msg.trim().replaceAll("\\s+", " ");
        return t.length() <= 20 ? t : t.substring(0, 20) + "…";
    }
}
