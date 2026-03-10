package com.renthouse.controller;

import com.renthouse.dto.UpdateAccountStatusRequest;
import com.renthouse.dto.UpdateUserRestrictionRequest;
import com.renthouse.dto.UserAdminResponse;
import com.renthouse.service.AdminUserService;
import com.renthouse.service.OperatorAccountService;
import com.renthouse.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private OperatorAccountService operatorAccountService;

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
        Long operatorId = AuthUtil.getCurrentOperatorId();
        operatorAccountService.requireAdmin(operatorId);
    }
}
