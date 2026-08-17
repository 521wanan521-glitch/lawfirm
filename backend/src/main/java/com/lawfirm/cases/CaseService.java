package com.lawfirm.cases;

import com.lawfirm.cases.dto.CaseProgressRequest;
import com.lawfirm.cases.dto.CaseProgressView;
import com.lawfirm.cases.dto.CaseRequest;
import com.lawfirm.cases.dto.CaseStatusRequest;
import com.lawfirm.cases.dto.CaseView;
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

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseProgressRepository progressRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    public PageResult<CaseView> page(String keyword, Long clientId, CaseStatus status, CaseType type,
                                     Long leadLawyerId, Priority priority, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Case> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("caseNo"), like),
                        cb.like(root.get("title"), like)
                ));
            }
            if (clientId != null) predicates.add(cb.equal(root.get("clientId"), clientId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (type != null) predicates.add(cb.equal(root.get("type"), type));
            if (leadLawyerId != null) predicates.add(cb.equal(root.get("leadLawyerId"), leadLawyerId));
            if (priority != null) predicates.add(cb.equal(root.get("priority"), priority));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Case> result = caseRepository.findAll(spec, pageable);
        return PageResult.of(result, this::toView);
    }

    public CaseView detail(Long id) {
        return toView(getById(id));
    }

    public PageResult<CaseProgressView> progress(Long caseId, int page, int size) {
        getById(caseId);
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CaseProgress> result = progressRepository.findByCaseIdOrderByProgressDateDesc(caseId, pageable);
        Map<Long, String> userNames = userNameMap(result.getContent().stream()
                .map(CaseProgress::getUserId).distinct().toList());
        return PageResult.of(result, p -> new CaseProgressView(p.getId(), p.getContent(), p.getProgressDate(),
                p.getUserId(), userNames.get(p.getUserId()), p.getNewStatus(), p.getCreatedAt()));
    }

    @Transactional
    public CaseView create(CaseRequest request) {
        Case c = new Case();
        apply(c, request);
        c.setCaseNo(generateCaseNo());
        c.setStatus(CaseStatus.NEW);
        Case saved = caseRepository.save(c);
        // 立案自动记录一条进程
        addProgressRecord(saved, "案件立案，案件号：" + saved.getCaseNo(), LocalDate.now(), CaseStatus.NEW);
        return toView(saved);
    }

    @Transactional
    public CaseView update(Long id, CaseRequest request) {
        Case c = getById(id);
        if (c.getStatus() == CaseStatus.CLOSED || c.getStatus() == CaseStatus.ARCHIVED) {
            throw new BizException("已结案/已归档案件不可编辑");
        }
        apply(c, request);
        return toView(caseRepository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        Case c = getById(id);
        if (c.getStatus() == CaseStatus.ACTIVE) {
            throw new BizException("办理中的案件不可删除，可先结案");
        }
        caseRepository.delete(c);
    }

    /** 更新案件状态（自动记录状态变更日志） */
    @Transactional
    public CaseView updateStatus(Long id, CaseStatusRequest request) {
        Case c = getById(id);
        CaseStatus oldStatus = c.getStatus();
        if (oldStatus == request.status()) {
            throw new BizException("案件已处于该状态");
        }
        c.setStatus(request.status());
        if (request.status() == CaseStatus.CLOSED) {
            c.setCloseDate(request.closeDate() != null ? request.closeDate() : LocalDate.now());
            c.setResult(request.result());
        }
        caseRepository.save(c);
        String content = switch (request.status()) {
            case ACTIVE -> "案件开始办理";
            case PAUSED -> "案件暂停办理";
            case CLOSED -> "案件结案" + (StringUtils.hasText(request.result()) ? "：" + request.result() : "");
            case ARCHIVED -> "案件归档";
            case NEW -> "恢复为待立案";
        };
        addProgressRecord(c, content, LocalDate.now(), request.status());
        return toView(c);
    }

    @Transactional
    public CaseProgressView addProgress(Long caseId, CaseProgressRequest request) {
        Case c = getById(caseId);
        CaseProgress p = addProgressRecord(c, request.content(), request.progressDate(), null);
        return new CaseProgressView(p.getId(), p.getContent(), p.getProgressDate(), p.getUserId(),
                CurrentUser.user().getRealName(), p.getNewStatus(), p.getCreatedAt());
    }

    /** 我的案件（主办或协办） */
    public PageResult<CaseView> myCases(int page, int size) {
        Long me = CurrentUser.id();
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Case> spec = (root, query, cb) -> {
            var co = cb.isMember(me, root.get("coLawyerIds"));
            return cb.or(cb.equal(root.get("leadLawyerId"), me), co);
        };
        Page<Case> result = caseRepository.findAll(spec, pageable);
        return PageResult.of(result, this::toView);
    }

    // ---------- 私有方法 ----------

    private CaseProgress addProgressRecord(Case c, String content, LocalDate date, CaseStatus newStatus) {
        CaseProgress p = new CaseProgress();
        p.setCaseId(c.getId());
        p.setUserId(CurrentUser.id());
        p.setProgressDate(date);
        p.setContent(content);
        p.setNewStatus(newStatus);
        return progressRepository.save(p);
    }

    private void apply(Case c, CaseRequest request) {
        clientRepository.findById(request.clientId())
                .orElseThrow(() -> new BizException("客户不存在"));
        userRepository.findById(request.leadLawyerId())
                .orElseThrow(() -> new BizException("主办律师不存在"));
        if (request.coLawyerIds() != null) {
            request.coLawyerIds().forEach(id -> {
                if (!userRepository.existsById(id)) {
                    throw new BizException("协办律师不存在：" + id);
                }
            });
        }
        c.setClientId(request.clientId());
        c.setTitle(request.title());
        c.setType(request.type());
        c.setPriority(request.priority() == null ? Priority.MEDIUM : request.priority());
        c.setLeadLawyerId(request.leadLawyerId());
        c.setCoLawyerIds(request.coLawyerIds() == null ? new ArrayList<>() : new ArrayList<>(request.coLawyerIds()));
        c.setCourt(request.court());
        c.setCaseAmount(request.caseAmount());
        c.setFilingDate(request.filingDate());
        c.setDescription(request.description());
        c.setFee(request.fee());
    }

    private synchronized String generateCaseNo() {
        int year = Year.now().getValue();
        long count = caseRepository.countByCaseNoStartingWith("LF" + year + "-");
        return String.format("LF%d-%04d", year, count + 1);
    }

    private CaseView toView(Case c) {
        String clientName = clientRepository.findById(c.getClientId()).map(x -> x.getName()).orElse("");
        String leadName = userRepository.findById(c.getLeadLawyerId()).map(User::getRealName).orElse("");
        Set<Long> ids = c.getCoLawyerIds() == null ? Set.of() : Set.copyOf(c.getCoLawyerIds());
        List<String> coNames = ids.isEmpty() ? List.of()
                : userRepository.findAllById(ids).stream().map(User::getRealName).toList();
        return CaseView.from(c, clientName, leadName, coNames);
    }

    private Case getById(Long id) {
        return caseRepository.findById(id).orElseThrow(() -> new BizException("案件不存在"));
    }

    private Map<Long, String> userNameMap(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, User::getRealName));
    }
}
