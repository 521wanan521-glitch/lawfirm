package com.lawfirm.client;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 客户跟进记录
 */
@Getter
@Setter
@Entity
@Table(name = "crm_interaction", indexes = {
        @Index(name = "idx_interaction_client", columnList = "clientId")
})
public class Interaction extends BaseEntity {

    public enum Type {
        PHONE, VISIT, EMAIL, WECHAT, MEETING, OTHER
    }

    @Column(nullable = false)
    private Long clientId;

    /** 跟进人 */
    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(nullable = false, length = 2000)
    private String content;

    /** 下次跟进日期 */
    @Column
    private LocalDate nextFollowDate;
}
