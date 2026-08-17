package com.lawfirm.dashboard;

import java.math.BigDecimal;
import java.util.List;

/**
 * 仪表盘汇总数据
 */
public record DashboardSummary(
        long totalCases,
        long activeCases,
        long closedCases,
        long totalClients,
        long pendingApprovals,
        long pendingTimeEntries,
        long myOpenCases,
        long upcomingEvents,
        BigDecimal revenueThisMonth,
        List<RecentCase> recentCases
) {
    public record RecentCase(Long id, String caseNo, String title, String status, String leadLawyerName) {
    }
}
