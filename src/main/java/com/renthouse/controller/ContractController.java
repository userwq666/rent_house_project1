package com.renthouse.controller;

import com.renthouse.dto.ContractDTO;
import com.renthouse.dto.CreateContractRequest;
import com.renthouse.dto.TerminateContractRequest;
import com.renthouse.dto.TerminationDecisionRequest;
import com.renthouse.enums.AccountType;
import com.renthouse.repository.AccountRepository;
import com.renthouse.service.ContractService;
import com.renthouse.util.AuthUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 合同 Controller
 */
@RestController
@RequestMapping("/api/contracts")
@CrossOrigin(origins = "*")
@Slf4j
public class ContractController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private AccountRepository accountRepository;

    /**
     * 创建合同（签约）
     */
    @PostMapping
    public ResponseEntity<?> createContract(@RequestBody CreateContractRequest request) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            ContractDTO contract = contractService.createContract(request, userId);
            return ResponseEntity.ok(contract);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("签约失败: " + e.getMessage());
        }
    }

    /**
     * 获取我的合同（作为房主或租客）
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyContracts() {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            Long accountId = AuthUtil.getCurrentAccountId();
            AccountType accountType = accountRepository.findById(accountId)
                    .map(acc -> acc.getAccountType())
                    .orElse(AccountType.USER);

            log.info("contracts/my - userId={}, accountId={}, accountType={}", userId, accountId, accountType);

            List<ContractDTO> contracts = contractService.getMyContracts(userId);

            return ResponseEntity.ok()
                    .header("X-Debug-UserId", userId.toString())
                    .header("X-Debug-AccountId", accountId.toString())
                    .header("X-Debug-AccountType", accountType.name())
                    .header("X-Debug-QuerySource", "getMyContracts")
                    .header("X-Debug-Result-Size", String.valueOf(contracts.size()))
                    .body(contracts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取作为房主的合同
     */
    @GetMapping("/as-landlord")
    public ResponseEntity<?> getLandlordContracts() {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            List<ContractDTO> contracts = contractService.getLandlordContracts(userId);
            return ResponseEntity.ok(contracts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取作为租客的合同
     */
    @GetMapping("/as-tenant")
    public ResponseEntity<?> getTenantContracts() {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            List<ContractDTO> contracts = contractService.getTenantContracts(userId);
            return ResponseEntity.ok(contracts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    /**
     * 根据 ID 获取合同详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getContractById(@PathVariable Long id) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            Long accountId = AuthUtil.getCurrentAccountId();
            ContractDTO contract = contractService.getContractById(id, userId, accountId);
            return ResponseEntity.ok(contract);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    /**
     * 终止合同（退租/强制终止）
     */
    @PutMapping("/{id}/terminate")
    public ResponseEntity<?> terminateContract(@PathVariable Long id,
                                               @RequestBody TerminateContractRequest requestBody) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            Long accountId = AuthUtil.getCurrentAccountId();
            contractService.submitTerminationRequest(id, userId, accountId, requestBody);
            return ResponseEntity.ok(Boolean.TRUE.equals(requestBody.getForce())
                    ? "已强制终止合同"
                    : "终止申请已发出，等待对方确认");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("操作失败: " + e.getMessage());
        }
    }

    /**
     * 终止申请审批（同意/拒绝）
     */
    @PutMapping("/termination/{requestId}/decision")
    public ResponseEntity<?> decideTermination(@PathVariable Long requestId,
                                               @RequestBody TerminationDecisionRequest requestBody) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            contractService.decideTermination(requestId, userId, requestBody);
            return ResponseEntity.ok(Boolean.TRUE.equals(requestBody.getApprove())
                    ? "已同意终止合同"
                    : "已拒绝终止合同");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("操作失败: " + e.getMessage());
        }
    }

    /**
     * 房主审批合同（同意后进入管理员审批）
     */
    @PutMapping("/{id}/landlord/approve")
    public ResponseEntity<?> approveByLandlord(@PathVariable Long id) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            contractService.approveByLandlord(id, userId);
            return ResponseEntity.ok("已提交管理员审批");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("操作失败: " + e.getMessage());
        }
    }

    /**
     * 管理员审批合同（通过）
     */
    @PutMapping("/{id}/admin/approve")
    public ResponseEntity<?> approveByAdmin(@PathVariable Long id) {
        try {
            Long accountId = AuthUtil.getCurrentAccountId();
            contractService.approveContract(id, accountId);
            return ResponseEntity.ok("合同已生效");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("操作失败：" + e.getMessage());
        }
    }
    
    /**
     * 管理员拒绝合同
     */
    @PutMapping("/{id}/admin/reject")
    public ResponseEntity<?> rejectByAdmin(@PathVariable Long id,
                                           @RequestBody Map<String, String> requestBody) {
        try {
            Long accountId = AuthUtil.getCurrentAccountId();
            String reason = requestBody.get("reason");
            contractService.rejectContract(id, accountId, reason);
            return ResponseEntity.ok("合同已被拒绝");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("操作失败：" + e.getMessage());
        }
    }

    /**
     * 管理员直接终止合同
     */
    @PutMapping("/{id}/admin/terminate")
    public ResponseEntity<?> adminTerminate(@PathVariable Long id) {
        try {
            Long accountId = AuthUtil.getCurrentAccountId();
            contractService.adminTerminate(id, accountId);
            return ResponseEntity.ok("合同已终止");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有合同（仅管理员）
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllContracts() {
        try {
            Long accountId = AuthUtil.getCurrentAccountId();
            List<ContractDTO> contracts = contractService.getAllContracts(accountId);
            return ResponseEntity.ok(contracts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }
}
