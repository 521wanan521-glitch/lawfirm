package com.lawfirm.billing;

/**
 * 工时记录状态
 */
public enum TimeEntryStatus {
    SUBMITTED,  // 已提交（待审核）
    APPROVED,   // 已审核
    BILLED      // 已开票
}
