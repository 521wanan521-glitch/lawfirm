package com.lawfirm.billing;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户账单
 */
@Getter
@Setter
@Entity
@Table(name = "bill_invoice", indexes = {
        @Index(name = "idx_invoice_no", columnList = "invoiceNo", unique = true),
        @Index(name = "idx_invoice_client", columnList = "clientId")
})
public class Invoice extends BaseEntity {

    /** 账单编号，如 INV2024-0001 */
    @Column(nullable = false, length = 30)
    private String invoiceNo;

    @Column(nullable = false)
    private Long clientId;

    /** 开票人 */
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    /** 账单总额（元） */
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** 关联的工时记录 */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bill_invoice_time", joinColumns = @JoinColumn(name = "invoice_id"))
    @Column(name = "time_entry_id")
    private List<Long> timeEntryIds = new ArrayList<>();

    @Column(length = 500)
    private String remark;
}
