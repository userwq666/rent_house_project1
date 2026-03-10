package com.renthouse.controller;

import com.renthouse.domain.Account;
import com.renthouse.dto.UpdateAccountStatusRequest;
import com.renthouse.dto.UpdateUserRestrictionRequest;
import com.renthouse.dto.UserAdminResponse;
import com.renthouse.enums.AccountType;
import com.renthouse.repository.AccountRepository;
import com.renthouse.service.AdminUserService;
import com.renthouse.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台用户管理接口
 */
@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private AccountRepository accountRepository;

    @GetMapping
    public ResponseEntity<List<UserAdminResponse>> getAllUsers() {
        verifyAdmin();
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @PutMapping("/{userId}/restrictions")
    public ResponseEntity<UserAdminResponse> updateRestrictions(
            @PathVariable Long userId,
            @RequestBody UpdateUserRestrictionRequest request) {
        verifyAdmin();
        return ResponseEntity.ok(adminUserService.updateRestrictions(userId, request));
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<UserAdminResponse> updateStatus(
            @PathVariable Long userId,
            @RequestBody UpdateAccountStatusRequest request) {
        verifyAdmin();
        return ResponseEntity.ok(adminUserService.updateStatus(userId, request));
    }

    private void verifyAdmin() {
        Long accountId = AuthUtil.getCurrentAccountId();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        if (account.getAccountType() != AccountType.ADMIN) {
            throw new RuntimeException("权限不足");
        }
    }
}
