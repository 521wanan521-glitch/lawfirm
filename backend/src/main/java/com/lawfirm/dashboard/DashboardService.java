package com.lawfirm.dashboard;

import com.lawfirm.approval.ApprovalInstanceRepository;
import com.lawfirm.approval.ApprovalStatus;
import com.lawfirm.billing.InvoiceRepository;
import com.lawfirm.billing.TimeEntryRepository;
import com.lawfirm.billing.TimeEntryStatus;
import com.lawfirm.calendar.CalendarEventRepository;
import com.lawfirm.cases.Case;
import com.lawfirm.cases.CaseRepository;
import com.lawfirm.cases.CaseStatus;
import com.lawfirm.cases.CaseType;
import com.lawfirm.client.ClientRepository;
import com.lawfirm.dashboard.DashboardSummary.RecentCase;
import com.lawfirm.dashboard.StatsView.LawyerHours;
import com.lawfirm.dashboard.StatsView.MonthlyPoint;
import com.lawfirm.dashboard.StatsView.NameCount;
import com.lawfirm.security.CurrentUser;
import com.lawfirm.user.User;
import com.lawfirm.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CaseRepository caseRepository;
    private final ClientRepository clientRepository;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final InvoiceRepository invoiceRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final UserRepository userRepository;

    public DashboardSummary summary() {
        Long me = CurrentUser.id();
        LocalDate monthStart = YearMonth.now().atDay(1);
        LocalDate monthEnd = YearMonth.now().atEndOfMonth();
        LocalDateTime weekStart = LocalDateTime.now();
        LocalDateTime weekEnd = LocalDateTime.now().plusDays(7);

        Specification<Case> myOpenSpec = (root, query, cb) -> {
            var co = cb.isMember(me, root.get("coLawyerIds"));
            return cb.and(
                    cb.or(cb.equal(root.get("leadLawyerId"), me), co),
                    cb.notEqual(root.get("status"), CaseStatus.CLOSED),
                    cb.notEqual(root.get("status"), CaseStatus.ARCHIVED)
            );
        };

        List<Case> recentCases = caseRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

        return new DashboardSummary(
                caseRepository.count(),
                caseRepository.countByStatus(CaseStatus.ACTIVE),
                caseRepository.countByStatus(CaseStatus.CLOSED),
                clientRepository.count(),
                approvalInstanceRepository.countByStatus(ApprovalStatus.PENDING),
                timeEntryRepository.countByStatus(TimeEntryStatus.SUBMITTED),
                caseRepository.count(myOpenSpec),
                calendarEventRepository.countByStartTimeBetween(weekStart, weekEnd),
                invoiceRepository.sumAmountByIssueDateBetween(monthStart, monthEnd),
                recentCases.stream().map(c -> new RecentCase(c.getId(), c.getCaseNo(), c.getTitle(),
                        c.getStatus().name(),
                        userRepository.findById(c.getLeadLawyerId()).map(User::getRealName).orElse(""))).toList()
        );
    }

    public StatsView stats() {
        // 案件类型分布
        List<Object[]> typeRows = caseRepository.countGroupByType();
        List<NameCount> casesByType = typeRows.stream()
                .map(row -> new NameCount(((CaseType) row[0]).name(), (Long) row[1]))
                .toList();

        // 近 6 个月新增案件与营收
        List<MonthlyPoint> monthlyTrend = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            long newCases = caseRepository.countByFilingDateBetween(start, end);
            BigDecimal revenue = invoiceRepository.sumAmountByIssueDateBetween(start, end);
            monthlyTrend.add(new MonthlyPoint(ym.format(DateTimeFormatter.ofPattern("yyyy-MM")), newCases, revenue));
        }

        // 近 30 天律师工时排行
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();
        List<Object[]> hourRows = timeEntryRepository.sumHoursGroupByUser(start, end);
        Map<Long, String> names = new HashMap<>();
        hourRows.stream().limit(5).forEach(row -> {
            Long uid = (Long) row[0];
            userRepository.findById(uid).ifPresent(u -> names.put(uid, u.getRealName()));
        });
        List<LawyerHours> lawyerHoursTop = hourRows.stream().limit(5)
                .map(row -> new LawyerHours((Long) row[0], names.getOrDefault((Long) row[0], ""), (BigDecimal) row[1]))
                .toList();

        return new StatsView(casesByType, monthlyTrend, lawyerHoursTop);
    }
}
