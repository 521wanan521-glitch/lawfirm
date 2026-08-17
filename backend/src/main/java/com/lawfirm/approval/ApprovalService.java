package com.lawfirm.approval;

import com.lawfirm.approval.dto.DecisionRequest;
import com.lawfirm.approval.dto.InstanceRequest;
import com.lawfirm.approval.dto.InstanceView;
import com.lawfirm.approval.dto.TemplateRequest;
import com.lawfirm.approval.dto.TemplateView;
import com.lawfirm.auth.dto.UserInfo;
import com.lawfirm.common.BizException;
import com.lawfirm.common.PageResult;
import com.lawfirm.security.CurrentUser;
import com.lawfirm.user.User;
import com.lawfirm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalTemplateRepository templateRepository;
    private final ApprovalInstanceRepository instanceRepository;
    private final UserRepository userRepository;

    // ==================== 模板（管理员） ====================

    public List<TemplateView> templates(boolean all) {
        List<ApprovalTemplate> list = all
                ? templateRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt"))
                : templateRepository.findByEnabledTrueOrderByCreatedAtAsc();
        return list.stream().map(t -> new TemplateView(t.getId(), t.getName(), t.getType(),
                t.getDescription(), t.getEnabled())).toList();
    }

    @Transactional
    public TemplateView createTemplate(TemplateRequest request) {
        ApprovalTemplate t = new ApprovalTemplate();
        t.setName(request.name());
        t.setType(request.type());
        t.setDescription(request.description());
        t.setEnabled(true);
        t = templateRepository.save(t);
        return new TemplateView(t.getId(), t.getName(), t.getType(), t.getDescription(), t.getEnabled());
    }

    @Transactional
    public TemplateView updateTemplate(Long id, TemplateRequest request) {
        ApprovalTemplate t = templateRepository.findById(id).orElseThrow(() -> new BizException("模板不存在"));
        t.setName(request.name());
        t.setType(request.type());
        t.setDescription(request.description());
        t = templateRepository.save(t);
        return new TemplateView(t.getId(), t.getName(), t.getType(), t.getDescription(), t.getEnabled());
    }

    @Transactional
    public void setTemplateEnabled(Long id, boolean enabled) {
        ApprovalTemplate t = templateRepository.findById(id).orElseThrow(() -> new BizException("模板不存在"));
        t.setEnabled(enabled);
        templateRepository.save(t);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        if (instanceRepository.count() > 0 && instanceRepository.findAll(Sort.unsorted()).stream()
                .anyMatch(i -> i.getTemplateId().equals(id))) {
            throw new BizException("该模板已被审批单使用，请停用而非删除");
        }
        templateRepository.deleteById(id);
    }

    // ==================== 审批人（合伙人/管理员） ====================

    public List<UserInfo> approvers() {
        return userRepository.findByEnabledTrueOrderByRealNameAsc().stream()
                .filter(u -> u.getRole() == User.Role.ADMIN || u.getRole() == User.Role.PARTNER)
                .map(UserInfo::from).toList();
    }

    // ==================== 审批实例 ====================

    /**
     * 审批单列表。
     * scope=all：管理员/合伙人看全部；scope=todo：待我审批；scope=mine：我提交的
     */
    public PageResult<InstanceView> instances(String scope, ApprovalStatus status, int page, int size) {
        Long me = CurrentUser.id();
        var role = CurrentUser.principal().getRole();
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ApprovalInstance> result;
        switch (scope == null ? "all" : scope) {
            case "todo" -> result = status == null
                    ? instanceRepository.findByApproverIdOrderByCreatedAtDesc(me, pageable)
                    : instanceRepository.findByApproverIdAndStatusOrderByCreatedAtDesc(me, status, pageable);
            case "mine" -> result = status == null
                    ? instanceRepository.findByApplicantIdOrderByCreatedAtDesc(me, pageable)
                    : instanceRepository.findByApplicantIdAndStatusOrderByCreatedAtDesc(me, status, pageable);
            default -> {
                boolean manager = role == User.Role.ADMIN || role == User.Role.PARTNER;
                result = manager
                        ? (status == null
                            ? instanceRepository.findAllByOrderByCreatedAtDesc(pageable)
                            : instanceRepository.findByStatusOrderByCreatedAtDesc(status, pageable))
                        : (status == null
                            ? instanceRepository.findByApplicantIdOrderByCreatedAtDesc(me, pageable)
                            : instanceRepository.findByApplicantIdAndStatusOrderByCreatedAtDesc(me, status, pageable));
            }
        }
        return PageResult.of(result, this::toView);
    }

    @Transactional
    public InstanceView create(InstanceRequest request) {
        templateRepository.findById(request.templateId()).orElseThrow(() -> new BizException("审批类型不存在"));
        User approver = userRepository.findById(request.approverId())
                .orElseThrow(() -> new BizException("审批人不存在"));
        if (approver.getRole() != User.Role.ADMIN && approver.getRole() != User.Role.PARTNER) {
            throw new BizException("审批人必须是合伙人或管理员");
        }
        ApprovalInstance instance = new ApprovalInstance();
        instance.setTemplateId(request.templateId());
        instance.setTitle(request.title());
        instance.setContent(request.content());
        instance.setApplicantId(CurrentUser.id());
        instance.setApproverId(request.approverId());
        instance.setCaseId(request.caseId());
        instance.setStatus(ApprovalStatus.PENDING);
        return toView(instanceRepository.save(instance));
    }

    @Transactional
    public InstanceView decide(Long id, boolean approved, DecisionRequest request) {
        ApprovalInstance instance = getById(id);
        if (instance.getStatus() != ApprovalStatus.PENDING) {
            throw new BizException("该审批已处理");
        }
        if (!instance.getApproverId().equals(CurrentUser.id()) && !CurrentUser.isAdmin()) {
            throw new BizException(403, "只有被指定的审批人可处理");
        }
        instance.setStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        instance.setComment(request.comment());
        instance.setDecidedAt(LocalDateTime.now());
        return toView(instanceRepository.save(instance));
    }

    @Transactional
    public InstanceView cancel(Long id) {
        ApprovalInstance instance = getById(id);
        if (!instance.getApplicantId().equals(CurrentUser.id()) && !CurrentUser.isAdmin()) {
            throw new BizException(403, "只能撤销自己提交的审批");
        }
        if (instance.getStatus() != ApprovalStatus.PENDING) {
            throw new BizException("仅待审批状态可撤销");
        }
        instance.setStatus(ApprovalStatus.CANCELLED);
        return toView(instanceRepository.save(instance));
    }

    private ApprovalInstance getById(Long id) {
        return instanceRepository.findById(id).orElseThrow(() -> new BizException("审批单不存在"));
    }

    private InstanceView toView(ApprovalInstance i) {
        ApprovalTemplate t = templateRepository.findById(i.getTemplateId()).orElse(null);
        String applicantName = userRepository.findById(i.getApplicantId()).map(User::getRealName).orElse("");
        String approverName = userRepository.findById(i.getApproverId()).map(User::getRealName).orElse("");
        return new InstanceView(i.getId(),
                t == null ? "" : t.getName(),
                t == null ? ApprovalType.OTHER : t.getType(),
                i.getTitle(), i.getContent(), i.getApplicantId(), applicantName,
                i.getApproverId(), approverName, i.getStatus(), i.getComment(), i.getDecidedAt(),
                i.getCaseId(), i.getCreatedAt());
    }
}
