package com.lawfirm.billing;

import com.lawfirm.billing.dto.InvoiceRequest;
import com.lawfirm.billing.dto.InvoiceStatusRequest;
import com.lawfirm.billing.dto.InvoiceView;
import com.lawfirm.billing.dto.TimeEntryRequest;
import com.lawfirm.billing.dto.TimeEntryView;
import com.lawfirm.cases.Case;
import com.lawfirm.cases.CaseRepository;
import com.lawfirm.client.ClientRepository;
import com.lawfirm.common.BizException;
import com.lawfirm.common.PageResult;
import com.lawfirm.security.CurrentUser;
import com.lawfirm.user.User;
import com.lawfirm.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final TimeEntryRepository timeEntryRepository;
    private final InvoiceRepository invoiceRepository;
    private final CaseRepository caseRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    // ==================== 工时 ====================

    /**
     * 工时分页查询。非管理员默认只看自己的工时。
     */
    public PageResult<TimeEntryView> pageTimeEntries(Long userId, Long caseId, TimeEntryStatus status,
                                                     LocalDate dateFrom, LocalDate dateTo, int page, int size) {
        boolean admin = CurrentUser.isAdmin();
        Long effectiveUserId = (userId != null && admin) ? userId : (admin ? null : CurrentUser.id());
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "workDate"));
        Specification<TimeEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (effectiveUserId != null) predicates.add(cb.equal(root.get("userId"), effectiveUserId));
            if (caseId != null) predicates.add(cb.equal(root.get("caseId"), caseId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (dateFrom != null) predicates.add(cb.greaterThanOrEqualTo(root.get("workDate"), dateFrom));
            if (dateTo != null) predicates.add(cb.lessThanOrEqualTo(root.get("workDate"), dateTo));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<TimeEntry> result = timeEntryRepository.findAll(spec, pageable);
        Map<Long, String> userNames = userNameMap(result.getContent().stream().map(TimeEntry::getUserId).distinct().toList());
        return PageResult.of(result, t -> toTimeView(t, userNames));
    }

    @Transactional
    public TimeEntryView createTimeEntry(TimeEntryRequest request) {
        validateCase(request.caseId());
        TimeEntry t = new TimeEntry();
        t.setUserId(CurrentUser.id());
        apply(t, request);
        return toTimeView(timeEntryRepository.save(t), Map.of());
    }

    @Transactional
    public TimeEntryView updateTimeEntry(Long id, TimeEntryRequest request) {
        TimeEntry t = getOwnTimeEntry(id);
        if (t.getStatus() != TimeEntryStatus.SUBMITTED) {
            throw new BizException("已审核或已开票的工时不可修改");
        }
        apply(t, request);
        return toTimeView(timeEntryRepository.save(t), Map.of());
    }

    @Transactional
    public void deleteTimeEntry(Long id) {
        TimeEntry t = getOwnTimeEntry(id);
        if (t.getStatus() != TimeEntryStatus.SUBMITTED) {
            throw new BizException("已审核或已开票的工时不可删除");
        }
        timeEntryRepository.delete(t);
    }

    /** 提交审核 */
    @Transactional
    public TimeEntryView submit(Long id) {
        TimeEntry t = getOwnTimeEntry(id);
        if (t.getStatus() != TimeEntryStatus.SUBMITTED) {
            throw new BizException("仅待审核状态的工时可提交");
        }
        t.setStatus(TimeEntryStatus.SUBMITTED);
        return toTimeView(timeEntryRepository.save(t), Map.of());
    }

    /** 审核（合伙人/管理员） */
    @Transactional
    public TimeEntryView review(Long id, boolean approved) {
        requireManager();
        TimeEntry t = timeEntryRepository.findById(id).orElseThrow(() -> new BizException("工时记录不存在"));
        if (t.getStatus() != TimeEntryStatus.SUBMITTED) {
            throw new BizException("仅待审核状态的工时可审核");
        }
        t.setStatus(approved ? TimeEntryStatus.APPROVED : TimeEntryStatus.SUBMITTED);
        return toTimeView(timeEntryRepository.save(t), Map.of());
    }

    // ==================== 账单 ====================

    public PageResult<InvoiceView> pageInvoices(Long clientId, InvoiceStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Invoice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (clientId != null) predicates.add(cb.equal(root.get("clientId"), clientId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Invoice> result = invoiceRepository.findAll(spec, pageable);
        Map<Long, String> userNames = userNameMap(result.getContent().stream().map(Invoice::getUserId).distinct().toList());
        return PageResult.of(result, inv -> toInvoiceView(inv, userNames));
    }

    public InvoiceView invoiceDetail(Long id) {
        Invoice inv = invoiceRepository.findById(id).orElseThrow(() -> new BizException("账单不存在"));
        return toInvoiceView(inv, userNameMap(List.of(inv.getUserId())));
    }

    /** 创建账单：将选中的已审核工时打包 */
    @Transactional
    public InvoiceView createInvoice(InvoiceRequest request) {
        requireManager();
        List<TimeEntry> entries = timeEntryRepository.findByIdIn(request.timeEntryIds());
        if (entries.size() != request.timeEntryIds().size()) {
            throw new BizException("部分工时记录不存在");
        }
        Set<Long> clientIds = entries.stream().map(t -> {
            Case c = caseRepository.findById(t.getCaseId()).orElseThrow(() -> new BizException("案件不存在"));
            return c.getClientId();
        }).collect(Collectors.toSet());
        if (clientIds.size() > 1 || !clientIds.contains(request.clientId())) {
            throw new BizException("所选工时必须属于同一客户，且与账单客户一致");
        }
        for (TimeEntry t : entries) {
            if (t.getStatus() != TimeEntryStatus.APPROVED) {
                throw new BizException("存在未审核的工时记录，无法开票");
            }
            if (t.getInvoiceId() != null) {
                throw new BizException("存在已开票的工时记录，无法重复开票");
            }
        }
        BigDecimal total = entries.stream()
                .map(TimeEntry::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Invoice inv = new Invoice();
        inv.setInvoiceNo(generateInvoiceNo());
        inv.setClientId(request.clientId());
        inv.setUserId(CurrentUser.id());
        inv.setIssueDate(request.issueDate());
        inv.setDueDate(request.dueDate());
        inv.setRemark(request.remark());
        inv.setStatus(InvoiceStatus.DRAFT);
        inv.setTotalAmount(total);
        inv.setTimeEntryIds(new ArrayList<>(request.timeEntryIds()));
        inv = invoiceRepository.save(inv);

        for (TimeEntry t : entries) {
            t.setInvoiceId(inv.getId());
            t.setStatus(TimeEntryStatus.BILLED);
        }
        timeEntryRepository.saveAll(entries);
        return toInvoiceView(inv, Map.of());
    }

    @Transactional
    public InvoiceView updateInvoiceStatus(Long id, InvoiceStatusRequest request) {
        Invoice inv = invoiceRepository.findById(id).orElseThrow(() -> new BizException("账单不存在"));
        InvoiceStatus target = request.status();
        switch (target) {
            case ISSUED -> {
                if (inv.getStatus() != InvoiceStatus.DRAFT) throw new BizException("仅草稿账单可开票");
            }
            case PAID -> {
                if (inv.getStatus() != InvoiceStatus.ISSUED) throw new BizException("仅已开票账单可标记收款");
            }
            case VOID -> {
                if (inv.getStatus() == InvoiceStatus.PAID) throw new BizException("已收款账单不可作废");
            }
            default -> throw new BizException("不支持的状态变更");
        }
        inv.setStatus(target);
        if (target == InvoiceStatus.VOID) {
            // 作废后释放工时，恢复为已审核
            List<TimeEntry> entries = timeEntryRepository.findByIdIn(inv.getTimeEntryIds());
            for (TimeEntry t : entries) {
                t.setInvoiceId(null);
                t.setStatus(TimeEntryStatus.APPROVED);
            }
            timeEntryRepository.saveAll(entries);
            inv.setTimeEntryIds(new ArrayList<>());
        }
        return toInvoiceView(invoiceRepository.save(inv), Map.of());
    }

    @Transactional
    public void deleteInvoice(Long id) {
        requireManager();
        Invoice inv = invoiceRepository.findById(id).orElseThrow(() -> new BizException("账单不存在"));
        if (inv.getStatus() != InvoiceStatus.DRAFT) {
            throw new BizException("仅草稿账单可删除");
        }
        List<TimeEntry> entries = timeEntryRepository.findByIdIn(inv.getTimeEntryIds());
        for (TimeEntry t : entries) {
            t.setInvoiceId(null);
            t.setStatus(TimeEntryStatus.APPROVED);
        }
        timeEntryRepository.saveAll(entries);
        invoiceRepository.delete(inv);
    }

    // ==================== 私有方法 ====================

    private void apply(TimeEntry t, TimeEntryRequest request) {
        t.setCaseId(request.caseId());
        t.setWorkDate(request.workDate());
        t.setHours(request.hours());
        t.setRate(request.rate());
        t.setDescription(request.description());
        t.setAmount(request.rate() == null ? null : request.hours().multiply(request.rate()));
    }

    private TimeEntry getOwnTimeEntry(Long id) {
        TimeEntry t = timeEntryRepository.findById(id).orElseThrow(() -> new BizException("工时记录不存在"));
        if (!t.getUserId().equals(CurrentUser.id()) && !CurrentUser.isAdmin()) {
            throw new BizException(403, "只能操作自己的工时记录");
        }
        return t;
    }

    private void validateCase(Long caseId) {
        caseRepository.findById(caseId).orElseThrow(() -> new BizException("案件不存在"));
    }

    private void requireManager() {
        var role = CurrentUser.principal().getRole();
        if (role != User.Role.ADMIN && role != User.Role.PARTNER) {
            throw new BizException(403, "该操作仅限合伙人或管理员");
        }
    }

    private synchronized String generateInvoiceNo() {
        int year = Year.now().getValue();
        long count = invoiceRepository.countByInvoiceNoStartingWith("INV" + year + "-");
        return String.format("INV%d-%04d", year, count + 1);
    }

    private TimeEntryView toTimeView(TimeEntry t, Map<Long, String> userNames) {
        String userName = userNames.getOrDefault(t.getUserId(), "");
        String caseNo = "", caseTitle = "";
        Case c = caseRepository.findById(t.getCaseId()).orElse(null);
        if (c != null) {
            caseNo = c.getCaseNo();
            caseTitle = c.getTitle();
        }
        return TimeEntryView.from(t, userName, caseNo, caseTitle);
    }

    private InvoiceView toInvoiceView(Invoice inv, Map<Long, String> userNames) {
        String clientName = clientRepository.findById(inv.getClientId()).map(c -> c.getName()).orElse("");
        String userName = userNames.getOrDefault(inv.getUserId(), "");
        return InvoiceView.from(inv, clientName, userName, inv.getTimeEntryIds().size());
    }

    private Map<Long, String> userNameMap(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, User::getRealName));
    }
}
