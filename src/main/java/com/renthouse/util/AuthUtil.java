package com.renthouse.util;

import com.renthouse.enums.OperatorRole;
import com.renthouse.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtil {

    private static AuthenticatedUser getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
            return principal;
        }
        throw new RuntimeException("未登录或登录已过期");
    }

    public static Long getCurrentUserId() {
        AuthenticatedUser principal = getPrincipal();
        if (principal.getUserId() == null) {
            throw new RuntimeException("当前账号不是普通用户");
        }
        return principal.getUserId();
    }

    public static Long getCurrentAccountId() {
        AuthenticatedUser principal = getPrincipal();
        if (principal.getAccountId() == null) {
            throw new RuntimeException("当前账号不是普通用户");
        }
        return principal.getAccountId();
    }

    public static Long getCurrentOperatorId() {
        AuthenticatedUser principal = getPrincipal();
        if (principal.getOperatorId() == null) {
            throw new RuntimeException("当前账号不是管理员/业务员");
        }
        return principal.getOperatorId();
    }

    public static OperatorRole getCurrentOperatorRole() {
        AuthenticatedUser principal = getPrincipal();
        if (principal.getOperatorRole() == null) {
            throw new RuntimeException("当前账号不是管理员/业务员");
        }
        return principal.getOperatorRole();
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
