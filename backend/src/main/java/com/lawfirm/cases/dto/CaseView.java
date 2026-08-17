package com.lawfirm.cases.dto;

import com.lawfirm.cases.Case;
import com.lawfirm.cases.CaseStatus;
import com.lawfirm.cases.CaseType;
import com.lawfirm.cases.Priority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CaseView(
        Long id,
        String caseNo,
        String title,
        Long clientId,
        String clientName,
        CaseType type,
        CaseStatus status,
        Priority priority,
        Long leadLawyerId,
        String leadLawyerName,
        List<Long> coLawyerIds,
        List<String> coLawyerNames,
        String court,
        BigDecimal caseAmount,
        LocalDate filingDate,
        LocalDate closeDate,
        String description,
        String result,
        BigDecimal fee,
        LocalDateTime createdAt
) {
    public static CaseView from(Case c, String clientName, String leadLawyerName, List<String> coLawyerNames) {
        return new CaseView(c.getId(), c.getCaseNo(), c.getTitle(), c.getClientId(), clientName,
                c.getType(), c.getStatus(), c.getPriority(), c.getLeadLawyerId(), leadLawyerName,
                c.getCoLawyerIds(), coLawyerNames, c.getCourt(), c.getCaseAmount(), c.getFilingDate(),
                c.getCloseDate(), c.getDescription(), c.getResult(), c.getFee(), c.getCreatedAt());
    }
}
