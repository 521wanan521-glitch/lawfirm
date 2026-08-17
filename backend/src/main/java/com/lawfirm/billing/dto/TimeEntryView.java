package com.lawfirm.billing.dto;

import com.lawfirm.billing.TimeEntry;
import com.lawfirm.billing.TimeEntryStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TimeEntryView(
        Long id,
        Long userId,
        String userName,
        Long caseId,
        String caseNo,
        String caseTitle,
        LocalDate workDate,
        BigDecimal hours,
        BigDecimal rate,
        BigDecimal amount,
        String description,
        TimeEntryStatus status,
        Long invoiceId,
        LocalDateTime createdAt
) {
    public static TimeEntryView from(TimeEntry t, String userName, String caseNo, String caseTitle) {
        return new TimeEntryView(t.getId(), t.getUserId(), userName, t.getCaseId(), caseNo, caseTitle,
                t.getWorkDate(), t.getHours(), t.getRate(), t.getAmount(), t.getDescription(),
                t.getStatus(), t.getInvoiceId(), t.getCreatedAt());
    }
}
