package com.lawfirm.user;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 系统用户（律所成员）
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user", indexes = {
        @Index(name = "idx_user_username", columnList = "username", unique = true)
})
public class User extends BaseEntity {

    /** 角色 */
    public enum Role {
        ADMIN,      // 系统管理员
        PARTNER,    // 合伙人
        LAWYER,     // 执业律师
        PARALEGAL,  // 律师助理/实习
        STAFF       // 行政人员
    }

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String realName;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** 部门/团队 */
    @Column(length = 50)
    private String department;

    /** 职务/职称 */
    @Column(length = 50)
    private String title;

    /** 状态：true 启用 / false 停用 */
    @Column(nullable = false)
    private Boolean enabled = true;

    @Column
    private LocalDateTime lastLoginAt;
}
