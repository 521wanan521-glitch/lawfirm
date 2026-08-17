package com.lawfirm.assistant;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 助手的写操作待确认记录（human-in-the-loop）。
 * 写工具被模型调用后先落一条 PENDING 记录，用户在界面确认后才真正执行。
 */
@Getter
@Setter
@Entity
@Table(name = "assistant_action", indexes = {
        @Index(name = "idx_action_user", columnList = "userId"),
        @Index(name = "idx_action_session", columnList = "sessionId")
})
public class AssistantPendingAction extends BaseEntity {

    public enum Status {
        PENDING,    // 待用户确认
        EXECUTED,   // 已确认并执行
        CANCELLED   // 已取消
    }

    @Column(nullable = false)
    private Long sessionId;

    /** 发起人（确认时校验必须为同一人） */
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String toolName;

    /** 工具参数 JSON（执行时使用，界面展示时对密码类字段脱敏） */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String arguments;

    /** 人类可读的操作摘要，展示在确认卡片上 */
    @Column(nullable = false, length = 500)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    /** 执行结果 JSON（确认执行后回填） */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String result;

    @Column
    private LocalDateTime decidedAt;
}
