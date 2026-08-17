package com.lawfirm.assistant;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * AI 助手会话消息（仅持久化 user / assistant 角色的最终内容）
 */
@Getter
@Setter
@Entity
@Table(name = "chat_message", indexes = {
        @Index(name = "idx_chat_msg_session", columnList = "sessionId")
})
public class ChatMessage extends BaseEntity {

    @Column(nullable = false)
    private Long sessionId;

    /** user / assistant */
    @Column(nullable = false, length = 20)
    private String role;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
}
