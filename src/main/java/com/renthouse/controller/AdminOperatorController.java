package com.renthouse.controller;

import com.renthouse.domain.OperatorAccount;
import com.renthouse.dto.CreateStaffRequest;
import com.renthouse.dto.OperatorAccountResponse;
import com.renthouse.dto.UpdateOperatorStatusRequest;
import com.renthouse.enums.OperatorRole;
import com.renthouse.service.OperatorAccountService;
import com.renthouse.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/operators")
@CrossOrigin(origins = "*")
public class AdminOperatorController {

    @Autowired
    private OperatorAccountService operatorAccountService;

    @GetMapping("/staff")
    public ResponseEntity<?> getStaffList() {
        verifyAdmin();
        List<OperatorAccountResponse> data = operatorAccountService.getStaffList().stream().map(this::convert).toList();
        return ResponseEntity.ok(data);
    }

    @PostMapping("/staff")
    public ResponseEntity<?> createStaff(@RequestBody CreateStaffRequest request) {
        verifyAdmin();
        OperatorAccount created = operatorAccountService.createStaff(
                request.getUsername(),
                request.getPassword(),
                request.getDisplayName(),
                request.getPhone()
        );
        return ResponseEntity.ok(convert(created));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStaffStatus(@PathVariable Long id, @RequestBody UpdateOperatorStatusRequest request) {
        verifyAdmin();
        OperatorAccount updated = operatorAccountService.updateEnabled(id, request.getEnabled());
        return ResponseEntity.ok(convert(updated));
    }

    private void verifyAdmin() {
        Long operatorId = AuthUtil.getCurrentOperatorId();
        OperatorAccount operator = operatorAccountService.requireAdmin(operatorId);
        if (operator.getRole() != OperatorRole.ADMIN) {
            throw new RuntimeException("权限不足");
        }
    }

    private OperatorAccountResponse convert(OperatorAccount account) {
        OperatorAccountResponse resp = new OperatorAccountResponse();
        resp.setId(account.getId());
        resp.setUsername(account.getUsername());
        resp.setDisplayName(account.getDisplayName());
        resp.setPhone(account.getPhone());
        resp.setRole(account.getRole());
        resp.setEnabled(account.getEnabled());
        resp.setCreatedAt(account.getCreatedAt());
        return resp;
    }
}
