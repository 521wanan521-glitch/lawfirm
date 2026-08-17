package com.lawfirm.cases;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 案件进程记录（含状态变更日志）
 */
@Getter
@Setter
@Entity
@Table(name = "case_progress", indexes = {
        @Index(name = "idx_progress_case", columnList = "caseId")
})
public class CaseProgress extends BaseEntity {

    @Column(nullable = false)
    private Long caseId;

    @Column(nullable = false)
    private Long userId;

    /** 进程发生日期 */
    @Column(nullable = false)
    private LocalDate progressDate;

    /** 记录内容 */
    @Column(nullable = false, length = 2000)
    private String content;

    /** 本次变更后的状态（非状态变更记录为 null） */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CaseStatus newStatus;
}
