package com.lawfirm.user.dto;

import com.lawfirm.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度需在 2-50 位之间")
    private String username;

    /** 仅创建时需要 */
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private String email;
    private String phone;

    @NotNull(message = "角色不能为空")
    private User.Role role;

    private String department;
    private String title;
}
