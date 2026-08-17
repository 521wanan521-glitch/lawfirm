package com.lawfirm.assistant;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 助手会话
 */
@Getter
@Setter
@Entity
@Table(name = "chat_session", indexes = {
        @Index(name = "idx_chat_session_user", columnList = "userId")
})
public class ChatSession extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    /** 会话标题（默认取首条消息前若干字） */
    @Column(nullable = false, length = 200)
    private String title;

    /** 最近一次消息时间，用于会话排序 */
    @Column
    private LocalDateTime lastMessageAt;
}
