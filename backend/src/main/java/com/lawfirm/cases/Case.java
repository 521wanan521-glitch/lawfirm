package com.lawfirm.cases;

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
 * 案件
 */
@Getter
@Setter
@Entity
@Table(name = "case_case", indexes = {
        @Index(name = "idx_case_no", columnList = "caseNo", unique = true),
        @Index(name = "idx_case_status", columnList = "status"),
        @Index(name = "idx_case_client", columnList = "clientId")
})
public class Case extends BaseEntity {

    /** 案号，如 LF2024-0001 */
    @Column(nullable = false, length = 30)
    private String caseNo;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private Long clientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseStatus status = CaseStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority = Priority.MEDIUM;

    /** 主办律师 */
    @Column(nullable = false)
    private Long leadLawyerId;

    /** 协办律师 */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "case_co_lawyer", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "lawyer_id")
    private List<Long> coLawyerIds = new ArrayList<>();

    /** 受理法院 */
    @Column(length = 100)
    private String court;

    /** 标的额（元） */
    @Column(precision = 18, scale = 2)
    private BigDecimal caseAmount;

    @Column
    private LocalDate filingDate;

    @Column
    private LocalDate closeDate;

    /** 案情摘要 */
    @Column(length = 2000)
    private String description;

    /** 办理结果 */
    @Column(length = 2000)
    private String result;

    /** 收费金额（元） */
    @Column(precision = 18, scale = 2)
    private BigDecimal fee;
}
