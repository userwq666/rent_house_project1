package com.renthouse.controller;

import com.renthouse.domain.Account;
import com.renthouse.dto.*;
import com.renthouse.enums.AccountType;
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
            AccountType targetType = parseTargetType(request.getAccountType());
            if (targetType != AccountType.USER) {
                Long operatorId = AuthUtil.getCurrentOperatorId();
                operatorAccountService.requireAdmin(operatorId);
            }

            Account account = accountService.register(request, targetType);
            String principalType = principalTypeOf(account.getAccountType());
            return ResponseEntity.ok(new AuthResponse(
                    null,
                    principalType.equals("USER") ? account.getId() : null,
                    principalType.equals("OPERATOR") ? account.getId() : null,
                    account.getUsername(),
                    account.getAccountType().name(),
                    principalType,
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
            Account account = accountService.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

            if (!Boolean.TRUE.equals(account.getEnabled())) {
                return ResponseEntity.badRequest().body(new AuthResponse(
                        null, null, null, null, null, null, "账号已被禁用"
                ));
            }

            if (!accountService.validatePassword(request.getPassword(), account.getPassword())) {
                return ResponseEntity.badRequest().body(new AuthResponse(
                        null, null, null, null, null, null, "用户名或密码错误"
                ));
            }

            String token = jwtUtil.generateToken(account.getId(), account.getAccountType());
            String principalType = principalTypeOf(account.getAccountType());

            return ResponseEntity.ok(new AuthResponse(
                    token,
                    principalType.equals("USER") ? account.getId() : null,
                    principalType.equals("OPERATOR") ? account.getId() : null,
                    account.getUsername(),
                    account.getAccountType().name(),
                    principalType,
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
            Long accountId = AuthUtil.getCurrentAccountId();
            Account account = accountService.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("账号不存在"));

            Map<String, Object> result = new HashMap<>();
            String principalType = principalTypeOf(account.getAccountType());
            result.put("principalType", principalType);
            result.put("account", toSafeAccount(account));
            if ("USER".equals(principalType)) {
                result.put("user", toSafeAccount(account));
            } else {
                result.put("operator", toSafeAccount(account));
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取用户信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            Long accountId = AuthUtil.getCurrentUserId();
            Account account = accountService.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            return ResponseEntity.ok(toProfileResponse(account));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取个人信息失败: " + e.getMessage());
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        try {
            Long accountId = AuthUtil.getCurrentUserId();
            Account account = accountService.updateProfile(
                    accountId,
                    request.getRealName(),
                    request.getPhone(),
                    request.getEmail(),
                    request.getIdCard()
            );
            return ResponseEntity.ok(toProfileResponse(account));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新个人信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/contact/{username}")
    public ResponseEntity<?> lookupContact(@PathVariable String username) {
        try {
            String target = username == null ? "" : username.trim();
            if (target.isEmpty()) {
                return ResponseEntity.badRequest().body("用户名不能为空");
            }

            Long currentAccountId = AuthUtil.getCurrentAccountId();
            Account current = accountService.findById(currentAccountId)
                    .orElseThrow(() -> new RuntimeException("当前账号不存在"));
            if (target.equals(current.getUsername())) {
                return ResponseEntity.badRequest().body("不能添加自己为联系人");
            }

            Account account = accountService.findByUsername(target)
                    .orElseThrow(() -> new RuntimeException("联系人不存在"));

            ContactLookupResponse resp = new ContactLookupResponse();
            resp.setPrincipalType(principalTypeOf(account.getAccountType()));
            if (account.getAccountType() == AccountType.USER) {
                resp.setUserId(account.getId());
            } else {
                resp.setOperatorId(account.getId());
            }
            resp.setUsername(account.getUsername());
            resp.setDisplayName(displayNameOf(account));
            resp.setPhone(account.getPhone());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取联系人失败: " + e.getMessage());
        }
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<?> getUserInfoByUsername(@PathVariable String username) {
        try {
            Long currentUserId = AuthUtil.getCurrentUserId();
            Account current = accountService.findById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            if (current.getUsername().equals(username)) {
                return ResponseEntity.badRequest().body("不能添加自己为联系人");
            }

            Account account = accountService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            if (account.getAccountType() != AccountType.USER) {
                throw new RuntimeException("该账号不是普通用户");
            }

            return ResponseEntity.ok(toProfileResponse(account));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取用户信息失败: " + e.getMessage());
        }
    }

    private AccountType parseTargetType(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            return AccountType.USER;
        }
        try {
            return AccountType.valueOf(accountType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("accountType 非法");
        }
    }

    private String principalTypeOf(AccountType accountType) {
        return accountType == AccountType.USER ? "USER" : "OPERATOR";
    }

    private String displayNameOf(Account account) {
        if (account.getDisplayName() != null && !account.getDisplayName().isBlank()) {
            return account.getDisplayName();
        }
        if (account.getRealName() != null && !account.getRealName().isBlank()) {
            return account.getRealName();
        }
        return account.getUsername();
    }

    private Map<String, Object> toSafeAccount(Account account) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", account.getId());
        map.put("username", account.getUsername());
        map.put("accountType", account.getAccountType().name());
        map.put("realName", account.getRealName());
        map.put("displayName", displayNameOf(account));
        map.put("phone", account.getPhone());
        map.put("email", account.getEmail());
        map.put("idCard", account.getIdCard());
        map.put("enabled", account.getEnabled());
        map.put("canPublish", account.getCanPublish());
        map.put("canRent", account.getCanRent());
        return map;
    }

    private UserProfileResponse toProfileResponse(Account account) {
        return new UserProfileResponse(
                account.getId(),
                account.getId(),
                account.getUsername(),
                account.getAccountType().name(),
                account.getRealName(),
                account.getPhone(),
                account.getEmail(),
                account.getIdCard()
        );
    }
}
