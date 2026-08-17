package com.lawfirm.approval;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 审批实例（一次具体的审批申请）
 */
@Getter
@Setter
@Entity
@Table(name = "appr_instance", indexes = {
        @Index(name = "idx_appr_applicant", columnList = "applicantId"),
        @Index(name = "idx_appr_approver", columnList = "approverId"),
        @Index(name = "idx_appr_status", columnList = "status")
})
public class ApprovalInstance extends BaseEntity {

    @Column(nullable = false)
    private Long templateId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    /** 申请人 */
    @Column(nullable = false)
    private Long applicantId;

    /** 审批人 */
    @Column(nullable = false)
    private Long approverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    /** 审批意见 */
    @Column(length = 500)
    private String comment;

    @Column
    private LocalDateTime decidedAt;

    /** 关联案件（可选） */
    @Column
    private Long caseId;
}
