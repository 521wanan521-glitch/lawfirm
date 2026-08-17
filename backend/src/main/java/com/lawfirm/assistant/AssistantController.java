package com.lawfirm.assistant;

import com.lawfirm.assistant.dto.ActionView;
import com.lawfirm.assistant.dto.ChatRequest;
import com.lawfirm.assistant.dto.MessageView;
import com.lawfirm.assistant.dto.RenameRequest;
import com.lawfirm.assistant.dto.SessionView;
import com.lawfirm.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    /** 流式对话（SSE），客户端请使用 fetch + ReadableStream 读取 */
    @PostMapping("/chat")
    public SseEmitter chat(@Valid @RequestBody ChatRequest request) {
        return assistantService.chat(request);
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SessionView>> sessions() {
        return ApiResponse.ok(assistantService.sessions());
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<MessageView>> messages(@PathVariable Long id) {
        return ApiResponse.ok(assistantService.messages(id));
    }

    @PutMapping("/sessions/{id}")
    public ApiResponse<SessionView> rename(@PathVariable Long id, @Valid @RequestBody RenameRequest request) {
        return ApiResponse.ok(assistantService.rename(id, request.title()));
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable Long id) {
        assistantService.deleteSession(id);
        return ApiResponse.ok();
    }

    // ---------- 写操作确认（human-in-the-loop） ----------

    /** 当前用户待确认的操作列表（可选按会话过滤） */
    @GetMapping("/actions")
    public ApiResponse<List<ActionView>> pendingActions(@RequestParam(required = false) Long sessionId) {
        return ApiResponse.ok(assistantService.pendingActions(sessionId));
    }

    /** 确认执行写操作 */
    @PostMapping("/actions/{id}/confirm")
    public ApiResponse<Map<String, Object>> confirm(@PathVariable Long id) {
        return ApiResponse.ok(assistantService.confirmAction(id));
    }

    /** 取消写操作 */
    @PostMapping("/actions/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        assistantService.cancelAction(id);
        return ApiResponse.ok();
    }
}
