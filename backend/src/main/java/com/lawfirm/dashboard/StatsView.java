package com.lawfirm.dashboard;

import java.math.BigDecimal;
import java.util.List;

/**
 * 经营统计
 */
public record StatsView(
        List<NameCount> casesByType,
        List<MonthlyPoint> monthlyTrend,
        List<LawyerHours> lawyerHoursTop
) {
    public record NameCount(String name, long count) {
    }

    public record MonthlyPoint(String month, long newCases, BigDecimal revenue) {
    }

    public record LawyerHours(Long userId, String userName, BigDecimal hours) {
    }
}
