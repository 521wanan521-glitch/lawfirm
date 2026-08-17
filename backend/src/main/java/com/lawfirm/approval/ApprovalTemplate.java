package com.lawfirm.approval;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 审批模板（流程类型定义）
 */
@Getter
@Setter
@Entity
@Table(name = "appr_template")
public class ApprovalTemplate extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalType type;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean enabled = true;
}
