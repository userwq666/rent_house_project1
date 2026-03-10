package com.renthouse.controller;

import com.renthouse.domain.Account;
import com.renthouse.domain.OperatorAccount;
import com.renthouse.domain.User;
import com.renthouse.dto.AuthResponse;
import com.renthouse.dto.ContactLookupResponse;
import com.renthouse.dto.LoginRequest;
import com.renthouse.dto.UpdateProfileRequest;
import com.renthouse.dto.UserProfileResponse;
import com.renthouse.dto.UserRegisterRequest;
import com.renthouse.service.AccountService;
import com.renthouse.service.OperatorAccountService;
import com.renthouse.util.AuthUtil;
import com.renthouse.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class NewAuthController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private OperatorAccountService operatorAccountService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegisterRequest request) {
        try {
            User user = accountService.register(request);
            return ResponseEntity.ok(new AuthResponse(
                    null,
                    user.getId(),
                    null,
                    user.getAccount().getUsername(),
                    user.getAccount().getAccountType().name(),
                    "USER",
                    "注册成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AuthResponse(
                    null, null, null, null, null, null, "注册失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // first try operator account (admin/staff)
            OperatorAccount operator = operatorAccountService.findByUsername(request.getUsername()).orElse(null);
            if (operator != null) {
                if (!Boolean.TRUE.equals(operator.getEnabled())) {
                    return ResponseEntity.badRequest().body(AuthResponse.operator(
                            null, null, operator.getUsername(), operator.getRole().name(), "账号已被禁用"
                    ));
                }
                if (!operatorAccountService.validatePassword(request.getPassword(), operator.getPassword())) {
                    return ResponseEntity.badRequest().body(AuthResponse.operator(
                            null, null, operator.getUsername(), operator.getRole().name(), "用户名或密码错误"
                    ));
                }
                String token = jwtUtil.generateOperatorToken(operator.getId(), operator.getRole());
                return ResponseEntity.ok(AuthResponse.operator(
                        token,
                        operator.getId(),
                        operator.getUsername(),
                        operator.getRole().name(),
                        "登录成功"
                ));
            }

            Account account = accountService.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

            if (!account.getEnabled()) {
                return ResponseEntity.badRequest().body(new AuthResponse(
                        null, null, null, null, null, null, "账号已被禁用"
                ));
            }

            if (!accountService.validatePassword(request.getPassword(), account.getPassword())) {
                return ResponseEntity.badRequest().body(new AuthResponse(
                        null, null, null, null, null, null, "用户名或密码错误"
                ));
            }

            User user = accountService.findUserByAccountId(account.getId())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            String token = jwtUtil.generateUserToken(user.getId(), account.getId());

            return ResponseEntity.ok(new AuthResponse(
                    token,
                    user.getId(),
                    null,
                    account.getUsername(),
                    account.getAccountType().name(),
                    "USER",
                    "登录成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AuthResponse(
                    null, null, null, null, null, null, "登录失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentPrincipal() {
        try {
            Map<String, Object> result = new HashMap<>();
            try {
                Long userId = AuthUtil.getCurrentUserId();
                User user = accountService.findUserById(userId)
                        .orElseThrow(() -> new RuntimeException("用户不存在"));
                result.put("principalType", "USER");
                result.put("user", user);
            } catch (Exception ignored) {
                Long operatorId = AuthUtil.getCurrentOperatorId();
                OperatorAccount operator = operatorAccountService.findById(operatorId)
                        .orElseThrow(() -> new RuntimeException("操作员不存在"));
                result.put("principalType", "OPERATOR");
                result.put("operator", operator);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取用户信息失败: " + e.getMessage());
        }
    }

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

    @GetMapping("/contact/{username}")
    public ResponseEntity<?> lookupContact(@PathVariable String username) {
        try {
            String target = username == null ? "" : username.trim();
            if (target.isEmpty()) {
                return ResponseEntity.badRequest().body("用户名不能为空");
            }

            String currentUsername;
            try {
                Long currentUserId = AuthUtil.getCurrentUserId();
                User currentUser = accountService.findUserById(currentUserId)
                        .orElseThrow(() -> new RuntimeException("用户不存在"));
                currentUsername = currentUser.getAccount().getUsername();
            } catch (Exception ignored) {
                Long currentOperatorId = AuthUtil.getCurrentOperatorId();
                OperatorAccount currentOperator = operatorAccountService.findById(currentOperatorId)
                        .orElseThrow(() -> new RuntimeException("操作员不存在"));
                currentUsername = currentOperator.getUsername();
            }

            if (target.equals(currentUsername)) {
                return ResponseEntity.badRequest().body("不能添加自己为联系人");
            }

            OperatorAccount operator = operatorAccountService.findByUsername(target).orElse(null);
            if (operator != null) {
                ContactLookupResponse resp = new ContactLookupResponse();
                resp.setPrincipalType("OPERATOR");
                resp.setOperatorId(operator.getId());
                resp.setUsername(operator.getUsername());
                resp.setDisplayName(operator.getDisplayName());
                resp.setPhone(operator.getPhone());
                return ResponseEntity.ok(resp);
            }

            Account account = accountService.findByUsername(target)
                    .orElseThrow(() -> new RuntimeException("联系人不存在"));
            User user = accountService.findUserByAccountId(account.getId())
                    .orElseThrow(() -> new RuntimeException("联系人不存在"));

            ContactLookupResponse resp = new ContactLookupResponse();
            resp.setPrincipalType("USER");
            resp.setUserId(user.getId());
            resp.setUsername(account.getUsername());
            resp.setDisplayName(user.getRealName());
            resp.setPhone(user.getPhone());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取联系人失败：" + e.getMessage());
        }
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<?> getUserInfoByUsername(@PathVariable String username) {
        try {
            Long currentUserId = AuthUtil.getCurrentUserId();

            User currentUser = accountService.findUserById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            if (currentUser.getAccount().getUsername().equals(username)) {
                return ResponseEntity.badRequest().body("不能添加自己为联系人");
            }

            Account account = accountService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            User user = accountService.findUserByAccountId(account.getId())
                    .orElseThrow(() -> new RuntimeException("用户信息不存在"));

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
