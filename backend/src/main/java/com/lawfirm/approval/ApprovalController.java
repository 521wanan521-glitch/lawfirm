package com.lawfirm.approval;

import com.lawfirm.approval.dto.DecisionRequest;
import com.lawfirm.approval.dto.InstanceRequest;
import com.lawfirm.approval.dto.InstanceView;
import com.lawfirm.approval.dto.TemplateRequest;
import com.lawfirm.approval.dto.TemplateView;
import com.lawfirm.auth.dto.UserInfo;
import com.lawfirm.common.ApiResponse;
import com.lawfirm.common.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    // ---------- 模板 ----------

    @GetMapping("/templates")
    public ApiResponse<List<TemplateView>> templates(@RequestParam(defaultValue = "false") boolean all) {
        return ApiResponse.ok(approvalService.templates(all));
    }

    @PostMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TemplateView> createTemplate(@Valid @RequestBody TemplateRequest request) {
        return ApiResponse.ok(approvalService.createTemplate(request));
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TemplateView> updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateRequest request) {
        return ApiResponse.ok(approvalService.updateTemplate(id, request));
    }

    @PutMapping("/templates/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> setTemplateEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        approvalService.setTemplateEnabled(id, enabled);
        return ApiResponse.ok();
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        approvalService.deleteTemplate(id);
        return ApiResponse.ok();
    }

    // ---------- 审批人 ----------

    @GetMapping("/approvers")
    public ApiResponse<List<UserInfo>> approvers() {
        return ApiResponse.ok(approvalService.approvers());
    }

    // ---------- 审批实例 ----------

    @GetMapping("/instances")
    public ApiResponse<PageResult<InstanceView>> instances(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(approvalService.instances(scope, status, page, size));
    }

    @PostMapping("/instances")
    public ApiResponse<InstanceView> create(@Valid @RequestBody InstanceRequest request) {
        return ApiResponse.ok(approvalService.create(request));
    }

    @PutMapping("/instances/{id}/approve")
    public ApiResponse<InstanceView> approve(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.ok(approvalService.decide(id, true, request));
    }

    @PutMapping("/instances/{id}/reject")
    public ApiResponse<InstanceView> reject(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.ok(approvalService.decide(id, false, request));
    }

    @PutMapping("/instances/{id}/cancel")
    public ApiResponse<InstanceView> cancel(@PathVariable Long id) {
        return ApiResponse.ok(approvalService.cancel(id));
    }
}
