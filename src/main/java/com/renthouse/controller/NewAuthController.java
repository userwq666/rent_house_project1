package com.renthouse.controller;

import com.renthouse.domain.Account;
import com.renthouse.domain.User;
import com.renthouse.dto.AuthResponse;
import com.renthouse.dto.LoginRequest;
import com.renthouse.dto.UpdateProfileRequest;
import com.renthouse.dto.UserProfileResponse;
import com.renthouse.dto.UserRegisterRequest;
import com.renthouse.service.AccountService;
import com.renthouse.util.AuthUtil;
import com.renthouse.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 Controller（新版）
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class NewAuthController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegisterRequest request) {
        try {
            User user = accountService.register(request);
            return ResponseEntity.ok(new AuthResponse(
                    null,
                    user.getId(),
                    user.getAccount().getUsername(),
                    user.getAccount().getAccountType().name(),
                    "注册成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AuthResponse(
                    null, null, null, null, "注册失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Account account = accountService.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

            if (!account.getEnabled()) {
                return ResponseEntity.badRequest().body(new AuthResponse(
                        null, null, null, null, "账号已被禁用"
                ));
            }

            if (!accountService.validatePassword(request.getPassword(), account.getPassword())) {
                return ResponseEntity.badRequest().body(new AuthResponse(
                        null, null, null, null, "用户名或密码错误"
                ));
            }

            User user = accountService.findUserByAccountId(account.getId())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            String token = jwtUtil.generateToken(user.getId(), account.getId());

            return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                account.getUsername(),
                account.getAccountType().name(),
                "登录成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AuthResponse(
                    null, null, null, null, "登录失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            String jwt = token.replace("Bearer ", "");
            if (!jwtUtil.validateToken(jwt)) {
                throw new RuntimeException("Token无效");
            }

            Long accountId = jwtUtil.extractAccountId(jwt);
            User user = accountService.findUserByAccountId(accountId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取个人信息（含账号信息）
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            User user = accountService.findUserById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            Account account = user.getAccount();
            UserProfileResponse resp = new UserProfileResponse(
                    user.getId(),
                    account.getId(),
                    account.getUsername(),
                    account.getAccountType().name(),
                    user.getRealName(),
                    user.getPhone(),
                    user.getEmail(),
                    user.getIdCard()
            );
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取个人信息失败: " + e.getMessage());
        }
    }

    /**
     * 更新个人信息（仅本人）
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            User user = accountService.updateUserProfile(
                    userId,
                    request.getRealName(),
                    request.getPhone(),
                    request.getEmail(),
                    request.getIdCard()
            );
            Account account = user.getAccount();
            UserProfileResponse resp = new UserProfileResponse(
                    user.getId(),
                    account.getId(),
                    account.getUsername(),
                    account.getAccountType().name(),
                    user.getRealName(),
                    user.getPhone(),
                    user.getEmail(),
                    user.getIdCard()
            );
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新个人信息失败：" + e.getMessage());
        }
    }

    /**
     * 根据用户名获取用户信息（用于添加联系人）
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<?> getUserInfoByUsername(@PathVariable String username) {
        try {
            Long currentUserId = AuthUtil.getCurrentUserId();
            
            // 不能添加自己
            User currentUser = accountService.findUserById(currentUserId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            Account currentAccount = accountService.findByUsername(currentUser.getAccount().getUsername())
                .orElseThrow(() -> new RuntimeException("账号不存在"));
            
            if (currentAccount.getUsername().equals(username)) {
                return ResponseEntity.badRequest().body("不能添加自己为联系人");
            }
            
            // 根据用户名查找用户
            Account account = accountService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            User user = accountService.findUserByAccountId(account.getId())
                    .orElseThrow(() -> new RuntimeException("用户信息不存在"));
            
            // 只返回基本信息，不返回敏感信息
            return ResponseEntity.ok(new UserProfileResponse(
                    user.getId(),
                    account.getId(),
                    account.getUsername(),
                    account.getAccountType().name(),
                    user.getRealName(),
                    user.getPhone(),
                    user.getEmail(),
                    user.getIdCard()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取用户信息失败：" + e.getMessage());
        }
    }
}
