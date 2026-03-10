package com.renthouse.util;

import com.renthouse.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 认证工具类
 */
public class AuthUtil {

    /**
     * 从 SecurityContext 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof AuthenticatedUser) {
                return ((AuthenticatedUser) principal).getUserId();
            }
            if (principal instanceof Long) {
                return (Long) principal;
            }
        }
        throw new RuntimeException("未登录或登录已过期");
    }

    /**
     * 从 SecurityContext 获取当前账号ID
     */
    public static Long getCurrentAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof AuthenticatedUser) {
                return ((AuthenticatedUser) principal).getAccountId();
            }
        }
        throw new RuntimeException("未登录或登录已过期");
    }

    /**
     * 检查是否已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
