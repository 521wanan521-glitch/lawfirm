package com.lawfirm.client;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 客户联系人
 */
@Getter
@Setter
@Entity
@Table(name = "crm_contact", indexes = {
        @Index(name = "idx_contact_client", columnList = "clientId")
})
public class Contact extends BaseEntity {

    @Column(nullable = false)
    private Long clientId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 50)
    private String position;

    /** 是否主要联系人 */
    @Column(nullable = false)
    private Boolean primaryContact = false;
}
