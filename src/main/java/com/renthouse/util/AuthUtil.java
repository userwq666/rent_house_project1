package com.renthouse.util;

import com.renthouse.enums.AccountType;
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

    public static Long getCurrentAccountId() {
        AuthenticatedUser principal = getPrincipal();
        if (principal.getAccountId() == null) {
            throw new RuntimeException("当前账号无效");
        }
        return principal.getAccountId();
    }

    public static AccountType getCurrentAccountType() {
        AuthenticatedUser principal = getPrincipal();
        if (principal.getAccountType() == null) {
            throw new RuntimeException("当前账号无效");
        }
        return principal.getAccountType();
    }

    public static Long getCurrentUserId() {
        AuthenticatedUser principal = getPrincipal();
        if (principal.getAccountType() != AccountType.USER) {
            throw new RuntimeException("当前账号不是普通用户");
        }
        return principal.getAccountId();
    }

    public static Long getCurrentOperatorId() {
        AuthenticatedUser principal = getPrincipal();
        if (principal.getAccountType() == AccountType.USER) {
            throw new RuntimeException("当前账号不是管理员/业务员");
        }
        return principal.getAccountId();
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
