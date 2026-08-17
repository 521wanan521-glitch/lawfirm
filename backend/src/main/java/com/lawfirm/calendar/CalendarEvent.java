package com.lawfirm.calendar;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 日程事件
 */
@Getter
@Setter
@Entity
@Table(name = "cal_event", indexes = {
        @Index(name = "idx_event_start", columnList = "startTime"),
        @Index(name = "idx_event_creator", columnList = "creatorId")
})
public class CalendarEvent extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventType type = EventType.TASK;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column(length = 100)
    private String location;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Long creatorId;

    /** 关联案件（可选） */
    @Column
    private Long caseId;

    /** 受邀参与人 */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cal_participant", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "user_id")
    private List<Long> participantIds = new ArrayList<>();
}
