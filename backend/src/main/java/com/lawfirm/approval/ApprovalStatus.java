package com.lawfirm.approval;

/**
 * 审批状态
 */
public enum ApprovalStatus {
    PENDING,    // 待审批
    APPROVED,   // 已通过
    REJECTED,   // 已驳回
    CANCELLED   // 已撤销
}
