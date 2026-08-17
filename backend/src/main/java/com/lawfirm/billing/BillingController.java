package com.lawfirm.billing;

import com.lawfirm.billing.dto.InvoiceRequest;
import com.lawfirm.billing.dto.InvoiceStatusRequest;
import com.lawfirm.billing.dto.InvoiceView;
import com.lawfirm.billing.dto.TimeEntryRequest;
import com.lawfirm.billing.dto.TimeEntryView;
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

import java.time.LocalDate;

@RestController
@RequestMapping("/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    // ==================== 工时 ====================

    @GetMapping("/time-entries")
    public ApiResponse<PageResult<TimeEntryView>> pageTimeEntries(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long caseId,
            @RequestParam(required = false) TimeEntryStatus status,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(billingService.pageTimeEntries(userId, caseId, status, dateFrom, dateTo, page, size));
    }

    @PostMapping("/time-entries")
    public ApiResponse<TimeEntryView> createTimeEntry(@Valid @RequestBody TimeEntryRequest request) {
        return ApiResponse.ok(billingService.createTimeEntry(request));
    }

    @PutMapping("/time-entries/{id}")
    public ApiResponse<TimeEntryView> updateTimeEntry(@PathVariable Long id, @Valid @RequestBody TimeEntryRequest request) {
        return ApiResponse.ok(billingService.updateTimeEntry(id, request));
    }

    @DeleteMapping("/time-entries/{id}")
    public ApiResponse<Void> deleteTimeEntry(@PathVariable Long id) {
        billingService.deleteTimeEntry(id);
        return ApiResponse.ok();
    }

    @PutMapping("/time-entries/{id}/submit")
    public ApiResponse<TimeEntryView> submit(@PathVariable Long id) {
        return ApiResponse.ok(billingService.submit(id));
    }

    @PutMapping("/time-entries/{id}/approve")
    public ApiResponse<TimeEntryView> approve(@PathVariable Long id) {
        return ApiResponse.ok(billingService.review(id, true));
    }

    @PutMapping("/time-entries/{id}/reject")
    public ApiResponse<TimeEntryView> reject(@PathVariable Long id) {
        return ApiResponse.ok(billingService.review(id, false));
    }

    // ==================== 账单 ====================

    @GetMapping("/invoices")
    public ApiResponse<PageResult<InvoiceView>> pageInvoices(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(billingService.pageInvoices(clientId, status, page, size));
    }

    @GetMapping("/invoices/{id}")
    public ApiResponse<InvoiceView> invoiceDetail(@PathVariable Long id) {
        return ApiResponse.ok(billingService.invoiceDetail(id));
    }

    @PostMapping("/invoices")
    public ApiResponse<InvoiceView> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        return ApiResponse.ok(billingService.createInvoice(request));
    }

    @PutMapping("/invoices/{id}/status")
    public ApiResponse<InvoiceView> updateInvoiceStatus(@PathVariable Long id, @Valid @RequestBody InvoiceStatusRequest request) {
        return ApiResponse.ok(billingService.updateInvoiceStatus(id, request));
    }

    @DeleteMapping("/invoices/{id}")
    public ApiResponse<Void> deleteInvoice(@PathVariable Long id) {
        billingService.deleteInvoice(id);
        return ApiResponse.ok();
    }
}
