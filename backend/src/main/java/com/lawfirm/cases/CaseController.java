package com.lawfirm.cases;

import com.lawfirm.cases.dto.CaseProgressRequest;
import com.lawfirm.cases.dto.CaseProgressView;
import com.lawfirm.cases.dto.CaseRequest;
import com.lawfirm.cases.dto.CaseStatusRequest;
import com.lawfirm.cases.dto.CaseView;
import com.lawfirm.common.ApiResponse;
import com.lawfirm.common.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    @GetMapping
    public ApiResponse<PageResult<CaseView>> page(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Long clientId,
                                                  @RequestParam(required = false) CaseStatus status,
                                                  @RequestParam(required = false) CaseType type,
                                                  @RequestParam(required = false) Long leadLawyerId,
                                                  @RequestParam(required = false) Priority priority,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(caseService.page(keyword, clientId, status, type, leadLawyerId, priority, page, size));
    }

    @GetMapping("/my")
    public ApiResponse<PageResult<CaseView>> myCases(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(caseService.myCases(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CaseView> detail(@PathVariable Long id) {
        return ApiResponse.ok(caseService.detail(id));
    }

    @PostMapping
    public ApiResponse<CaseView> create(@Valid @RequestBody CaseRequest request) {
        return ApiResponse.ok(caseService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CaseView> update(@PathVariable Long id, @Valid @RequestBody CaseRequest request) {
        return ApiResponse.ok(caseService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<CaseView> updateStatus(@PathVariable Long id, @Valid @RequestBody CaseStatusRequest request) {
        return ApiResponse.ok(caseService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        caseService.delete(id);
        return ApiResponse.ok();
    }

    // ---------- 案件进程 ----------

    @GetMapping("/{id}/progress")
    public ApiResponse<PageResult<CaseProgressView>> progress(@PathVariable Long id,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(caseService.progress(id, page, size));
    }

    @PostMapping("/{id}/progress")
    public ApiResponse<CaseProgressView> addProgress(@PathVariable Long id, @Valid @RequestBody CaseProgressRequest request) {
        return ApiResponse.ok(caseService.addProgress(id, request));
    }
}
