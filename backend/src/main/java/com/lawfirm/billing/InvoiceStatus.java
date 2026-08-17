package com.lawfirm.billing;

/**
 * 账单状态
 */
public enum InvoiceStatus {
    DRAFT,  // 草稿
    ISSUED, // 已开票
    PAID,   // 已收款
    VOID    // 已作废
}
