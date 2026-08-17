package com.lawfirm.auth.dto;

import com.lawfirm.user.User;
import lombok.Data;

/**
 * 用户信息（不含敏感字段）
 */
@Data
public class UserInfo {

    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private User.Role role;
    private String department;
    private String title;
    private Boolean enabled;

    public static UserInfo from(User u) {
        UserInfo info = new UserInfo();
        info.setId(u.getId());
        info.setUsername(u.getUsername());
        info.setRealName(u.getRealName());
        info.setEmail(u.getEmail());
        info.setPhone(u.getPhone());
        info.setRole(u.getRole());
        info.setDepartment(u.getDepartment());
        info.setTitle(u.getTitle());
        info.setEnabled(u.getEnabled());
        return info;
    }
}
