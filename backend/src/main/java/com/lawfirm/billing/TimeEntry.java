package com.lawfirm.billing;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 律师工时记录
 */
@Getter
@Setter
@Entity
@Table(name = "bill_time_entry", indexes = {
        @Index(name = "idx_time_case", columnList = "caseId"),
        @Index(name = "idx_time_user", columnList = "userId")
})
public class TimeEntry extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long caseId;

    @Column(nullable = false)
    private LocalDate workDate;

    /** 工时（小时） */
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal hours;

    /** 费率（元/小时） */
    @Column(precision = 12, scale = 2)
    private BigDecimal rate;

    /** 金额 = hours × rate */
    @Column(precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeEntryStatus status = TimeEntryStatus.SUBMITTED;

    /** 所属账单（开票后回填） */
    @Column
    private Long invoiceId;
}
