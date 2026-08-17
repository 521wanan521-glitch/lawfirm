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

/**
 * 客户
 */
@Getter
@Setter
@Entity
@Table(name = "crm_client", indexes = {
        @Index(name = "idx_client_name", columnList = "name")
})
public class Client extends BaseEntity {

    public enum Type {
        PERSONAL,   // 个人客户
        COMPANY     // 企业客户
    }

    public enum Level {
        A, B, C     // A 重要 / B 普通 / C 潜在
    }

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    /** 证件号码 / 统一社会信用代码 */
    @Column(length = 50)
    private String idNumber;

    @Column(length = 50)
    private String industry;

    @Column(length = 200)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Level level = Level.C;

    @Column(length = 50)
    private String source;

    /** 负责人 */
    @Column
    private Long ownerId;

    @Column(length = 500)
    private String remark;
}
