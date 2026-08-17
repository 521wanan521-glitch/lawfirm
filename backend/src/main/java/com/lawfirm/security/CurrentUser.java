package com.lawfirm.security;

import com.lawfirm.common.BizException;
import com.lawfirm.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 获取当前登录用户的工具类
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UserPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new BizException(401, "未登录");
    }

    public static Long id() {
        return principal().getId();
    }

    public static User user() {
        return principal().getUser();
    }

    public static boolean hasRole(User.Role role) {
        return principal().getRole() == role;
    }

    public static boolean isAdmin() {
        return hasRole(User.Role.ADMIN);
    }

    /** 管理员或合伙人（可查看全所数据） */
    public static boolean isManager() {
        return hasRole(User.Role.ADMIN) || hasRole(User.Role.PARTNER);
    }
}
