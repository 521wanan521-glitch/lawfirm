package com.lawfirm.billing.dto;

import com.lawfirm.billing.Invoice;
import com.lawfirm.billing.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceView(
        Long id,
        String invoiceNo,
        Long clientId,
        String clientName,
        Long userId,
        String userName,
        LocalDate issueDate,
        LocalDate dueDate,
        InvoiceStatus status,
        BigDecimal totalAmount,
        int timeEntryCount,
        String remark,
        LocalDateTime createdAt
) {
    public static InvoiceView from(Invoice inv, String clientName, String userName, int timeEntryCount) {
        return new InvoiceView(inv.getId(), inv.getInvoiceNo(), inv.getClientId(), clientName,
                inv.getUserId(), userName, inv.getIssueDate(), inv.getDueDate(), inv.getStatus(),
                inv.getTotalAmount(), timeEntryCount, inv.getRemark(), inv.getCreatedAt());
    }
}
