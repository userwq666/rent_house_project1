package com.renthouse.controller;

import com.renthouse.dto.ContractDTO;
import com.renthouse.dto.CreateContractRequest;
import com.renthouse.dto.LandlordApproveRequest;
import com.renthouse.dto.StaffOptionResponse;
import com.renthouse.dto.TerminateContractRequest;
import com.renthouse.dto.TerminationCounterpartyDecisionRequest;
import com.renthouse.dto.TerminationDecisionRequest;
import com.renthouse.service.ContractService;
import com.renthouse.service.OperatorAccountService;
import com.renthouse.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
@CrossOrigin(origins = "*")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private OperatorAccountService operatorAccountService;

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

    @GetMapping("/my")
    public ResponseEntity<?> getMyContracts() {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            List<ContractDTO> contracts = contractService.getMyContracts(userId);
            return ResponseEntity.ok(contracts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/staff/my")
    public ResponseEntity<?> getMyStaffContracts() {
        try {
            Long operatorId = AuthUtil.getCurrentOperatorId();
            return ResponseEntity.ok(contractService.getStaffContracts(operatorId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<?> getContractById(@PathVariable Long id) {
        try {
            Long userId = null;
            Long operatorId = null;
            try {
                userId = AuthUtil.getCurrentUserId();
            } catch (Exception ignored) {
                operatorId = AuthUtil.getCurrentOperatorId();
            }
            ContractDTO contract = contractService.getContractById(id, userId, operatorId);
            return ResponseEntity.ok(contract);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/terminate")
    public ResponseEntity<?> terminateContract(@PathVariable Long id,
                                               @RequestBody TerminateContractRequest requestBody) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            contractService.submitTerminationRequest(id, userId, null, requestBody);
            return ResponseEntity.ok("终止申请已提交，等待业务员审核");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("操作失败: " + e.getMessage());
        }
    }

    @PutMapping("/termination/{requestId}/decision")
    public ResponseEntity<?> decideTermination(@PathVariable Long requestId,
                                               @RequestBody TerminationDecisionRequest requestBody) {
        try {
            Long operatorId = AuthUtil.getCurrentOperatorId();
            contractService.decideTermination(requestId, operatorId, requestBody);
            return ResponseEntity.ok(Boolean.TRUE.equals(requestBody.getApprove())
                    ? "已同意终止合同"
                    : "已驳回终止合同");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("操作失败: " + e.getMessage());
        }
    }

    @PutMapping("/termination/{requestId}/counterparty-decision")
    public ResponseEntity<?> decideByCounterparty(@PathVariable Long requestId,
                                                  @RequestBody TerminationCounterpartyDecisionRequest requestBody) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            contractService.decideByCounterparty(requestId, userId, requestBody);
            return ResponseEntity.ok(Boolean.TRUE.equals(requestBody.getApprove())
                    ? "已同意终止申请，等待业务员审核"
                    : "已拒绝终止申请");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("操作失败: " + e.getMessage());
        }
    }

    @GetMapping("/staff/options")
    public ResponseEntity<?> getStaffOptionsForLandlord() {
        try {
            AuthUtil.getCurrentUserId();
            List<StaffOptionResponse> options = operatorAccountService.getEnabledStaffList().stream()
                    .map(staff -> new StaffOptionResponse(staff.getId(), staff.getDisplayName(), staff.getPhone()))
                    .toList();
            return ResponseEntity.ok(options);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取业务员列表失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/landlord/approve")
    public ResponseEntity<?> approveByLandlord(@PathVariable Long id,
                                               @RequestBody(required = false) LandlordApproveRequest request) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            Long staffId = request == null ? null : request.getStaffId();
            contractService.approveByLandlord(id, userId, staffId);
            return ResponseEntity.ok("已分配业务员并进入签约流程");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("操作失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/landlord/reject")
    public ResponseEntity<?> rejectByLandlord(@PathVariable Long id,
                                              @RequestBody(required = false) Map<String, String> requestBody) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            String reason = requestBody == null ? null : requestBody.get("reason");
            contractService.rejectByLandlord(id, userId, reason);
            return ResponseEntity.ok("已拒绝租房申请");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("操作失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/signed-file")
    public ResponseEntity<?> uploadSignedContract(@PathVariable Long id,
                                                  @RequestParam("file") MultipartFile file) {
        try {
            Long operatorId = AuthUtil.getCurrentOperatorId();
            contractService.uploadSignedContract(id, operatorId, file);
            return ResponseEntity.ok("上传成功，已提交管理员审核");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("上传失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/admin/approve")
    public ResponseEntity<?> approveByAdmin(@PathVariable Long id) {
        try {
            Long operatorId = AuthUtil.getCurrentOperatorId();
            contractService.approveContract(id, operatorId);
            return ResponseEntity.ok("合同已生效");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("操作失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/admin/reject")
    public ResponseEntity<?> rejectByAdmin(@PathVariable Long id,
                                           @RequestBody Map<String, String> requestBody) {
        try {
            Long operatorId = AuthUtil.getCurrentOperatorId();
            String reason = requestBody.get("reason");
            contractService.rejectContract(id, operatorId, reason);
            return ResponseEntity.ok("合同已驳回并退回业务员补充");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("操作失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/admin/terminate")
    public ResponseEntity<?> adminTerminate(@PathVariable Long id) {
        try {
            Long operatorId = AuthUtil.getCurrentOperatorId();
            contractService.adminTerminate(id, operatorId);
            return ResponseEntity.ok("合同已终止");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("操作失败: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllContracts() {
        try {
            Long operatorId = AuthUtil.getCurrentOperatorId();
            List<ContractDTO> contracts = contractService.getAllContracts(operatorId);
            return ResponseEntity.ok(contracts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }
}
