package com.renthouse.service;

import com.renthouse.domain.Contract;
import com.renthouse.domain.House;
import com.renthouse.domain.OperatorAccount;
import com.renthouse.domain.TerminationRequest;
import com.renthouse.domain.User;
import com.renthouse.dto.ContractDTO;
import com.renthouse.dto.CreateContractRequest;
import com.renthouse.dto.TerminateContractRequest;
import com.renthouse.dto.TerminationDecisionRequest;
import com.renthouse.enums.ContractStatus;
import com.renthouse.enums.HouseStatus;
import com.renthouse.enums.MessageType;
import com.renthouse.enums.OperatorRole;
import com.renthouse.enums.TerminationStatus;
import com.renthouse.repository.ContractRepository;
import com.renthouse.repository.HouseRepository;
import com.renthouse.repository.OperatorAccountRepository;
import com.renthouse.repository.TerminationRequestRepository;
import com.renthouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OperatorAccountRepository operatorAccountRepository;

    @Autowired
    private OperatorAccountService operatorAccountService;

    @Autowired
    private TerminationRequestRepository terminationRequestRepository;

    @Autowired
    private MessageService messageService;

    @Transactional
    public ContractDTO createContract(CreateContractRequest request, Long currentUserId) {
        House house = houseRepository.findById(request.getHouseId())
                .orElseThrow(() -> new RuntimeException("房源不存在"));

        if (house.getStatus() != HouseStatus.AVAILABLE) {
            throw new RuntimeException("房源不可租");
        }

        if (house.getOwner().getId().equals(currentUserId)) {
            throw new RuntimeException("不能租自己的房子");
        }

        User tenant = userRepository.findById(request.getTenantId())
                .orElseThrow(() -> new RuntimeException("租客不存在"));

        if (!tenant.getId().equals(currentUserId)) {
            throw new RuntimeException("只能为自己创建合同");
        }

        contractRepository.findActiveContractByHouseId(house.getId())
                .ifPresent(c -> {
                    throw new RuntimeException("该房源已有有效合同");
                });

        Contract contract = new Contract();
        contract.setHouse(house);
        contract.setLandlord(house.getOwner());
        contract.setTenant(tenant);
        contract.setRentPrice(request.getRentPrice());
        contract.setDeposit(request.getDeposit());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setStatus(ContractStatus.PENDING_LANDLORD_APPROVAL);
        contract.setSignedDate(LocalDateTime.now());
        contract.setNotes(request.getNotes());

        Contract saved = contractRepository.save(contract);
        house.setStatus(HouseStatus.PENDING);
        houseRepository.save(house);

        messageService.sendMessage(
                tenant.getId(),
                house.getOwner().getId(),
                "租房申请",
                String.format("%s 申请租用您的房源《%s》，租金：%s，押金：%s，租期：%s 至 %s",
                        tenant.getRealName(),
                        house.getTitle(),
                        request.getRentPrice(),
                        request.getDeposit(),
                        request.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        request.getEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))),
                MessageType.USER_CHAT,
                contract.getId(),
                null,
                true
        );

        return convertToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ContractDTO> getMyContracts(Long userId) {
        return contractRepository.findByUserId(userId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractDTO> getLandlordContracts(Long userId) {
        return contractRepository.findByLandlordId(userId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractDTO> getTenantContracts(Long userId) {
        return contractRepository.findByTenantId(userId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractDTO> getStaffContracts(Long operatorId) {
        return contractRepository.findByAssignedStaffId(operatorId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public ContractDTO getContractById(Long contractId, Long userId, Long operatorId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));
        checkContractPermission(contract, userId, operatorId);
        return convertToDTO(contract);
    }

    @Transactional
    public void submitTerminationRequest(Long contractId, Long userId, Long operatorId, TerminateContractRequest request) {
        if (operatorId != null) {
            throw new RuntimeException("仅合同当事人可以发起终止");
        }

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        checkContractPermission(contract, userId, null);

        if (contract.getStatus() != ContractStatus.ACTIVE && contract.getStatus() != ContractStatus.TERMINATION_PENDING) {
            throw new RuntimeException("合同状态异常，无法终止");
        }

        terminationRequestRepository.findByContractIdAndStatus(contractId, TerminationStatus.PENDING)
                .ifPresent(req -> {
                    throw new RuntimeException("已有待处理的终止申请");
                });

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Long staffId = contract.getAssignedStaffId();
        if (staffId == null) {
            staffId = operatorAccountService.pickRandomEnabledStaff().getId();
            contract.setAssignedStaffId(staffId);
        }

        TerminationRequest terminationRequest = new TerminationRequest();
        terminationRequest.setContract(contract);
        terminationRequest.setRequester(requester);
        terminationRequest.setResponder(resolveCounterparty(contract, userId));
        terminationRequest.setReason(request.getReason());
        terminationRequest.setReviewStaffId(staffId);
        terminationRequestRepository.save(terminationRequest);

        contract.setStatus(ContractStatus.TERMINATION_PENDING_STAFF_REVIEW);
        contractRepository.save(contract);

        messageService.notifyStaff(
                staffId,
                "合同终止待审核",
                String.format("合同《%s》(ID:%d) 发起终止申请，请审核", contract.getHouse().getTitle(), contract.getId()),
                contract.getId(),
                terminationRequest.getId(),
                true,
                MessageType.TERMINATION_PENDING_STAFF_REVIEW
        );

        messageService.sendMessage(
                null,
                contract.getLandlord().getId(),
                "合同终止申请已提交",
                "终止申请已提交业务员审核",
                MessageType.TERMINATION_REQUEST,
                contract.getId(),
                terminationRequest.getId(),
                false
        );

        messageService.sendMessage(
                null,
                contract.getTenant().getId(),
                "合同终止申请已提交",
                "终止申请已提交业务员审核",
                MessageType.TERMINATION_REQUEST,
                contract.getId(),
                terminationRequest.getId(),
                false
        );
    }

    @Transactional
    public void decideTermination(Long requestId, Long operatorId, TerminationDecisionRequest decision) {
        TerminationRequest terminationRequest = terminationRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("终止申请不存在"));

        OperatorAccount operator = operatorAccountRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("业务员不存在"));

        if (operator.getRole() != OperatorRole.STAFF && operator.getRole() != OperatorRole.ADMIN) {
            throw new RuntimeException("无权操作该终止申请");
        }

        if (operator.getRole() == OperatorRole.STAFF && !operatorId.equals(terminationRequest.getReviewStaffId())) {
            throw new RuntimeException("非当前分配业务员，无法审核");
        }

        if (terminationRequest.getStatus() != TerminationStatus.PENDING) {
            throw new RuntimeException("终止申请已处理");
        }

        Contract contract = terminationRequest.getContract();

        if (Boolean.TRUE.equals(decision.getApprove())) {
            terminationRequest.setStatus(TerminationStatus.APPROVED);
            terminationRequestRepository.save(terminationRequest);
            finalizeTermination(contract);

            messageService.sendMessage(null, contract.getLandlord().getId(), "合同已终止", "业务员已审核通过，合同已终止", MessageType.TERMINATION_RESPONSE, contract.getId(), terminationRequest.getId(), false);
            messageService.sendMessage(null, contract.getTenant().getId(), "合同已终止", "业务员已审核通过，合同已终止", MessageType.TERMINATION_RESPONSE, contract.getId(), terminationRequest.getId(), false);
        } else {
            terminationRequest.setStatus(TerminationStatus.REJECTED);
            terminationRequest.setForceReason(decision.getComment());
            terminationRequestRepository.save(terminationRequest);
            contract.setStatus(ContractStatus.ACTIVE);
            contractRepository.save(contract);

            String comment = decision.getComment() == null ? "无" : decision.getComment();
            messageService.sendMessage(null, contract.getLandlord().getId(), "终止申请被驳回", "业务员驳回终止申请，说明：" + comment, MessageType.TERMINATION_RESPONSE, contract.getId(), terminationRequest.getId(), false);
            messageService.sendMessage(null, contract.getTenant().getId(), "终止申请被驳回", "业务员驳回终止申请，说明：" + comment, MessageType.TERMINATION_RESPONSE, contract.getId(), terminationRequest.getId(), false);
        }
    }

    @Transactional
    public void adminTerminate(Long contractId, Long operatorId) {
        OperatorAccount account = operatorAccountRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        if (account.getRole() != OperatorRole.ADMIN) {
            throw new RuntimeException("权限不足");
        }
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));
        finalizeTermination(contract);
    }

    @Transactional(readOnly = true)
    public List<ContractDTO> getAllContracts(Long operatorId) {
        OperatorAccount account = operatorAccountRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        if (account.getRole() != OperatorRole.ADMIN) {
            throw new RuntimeException("权限不足");
        }

        return contractRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private void checkContractPermission(Contract contract, Long userId, Long operatorId) {
        if (userId != null && (contract.getLandlord().getId().equals(userId) || contract.getTenant().getId().equals(userId))) {
            return;
        }

        if (operatorId != null) {
            OperatorAccount operator = operatorAccountRepository.findById(operatorId)
                    .orElseThrow(() -> new RuntimeException("账号不存在"));
            if (operator.getRole() == OperatorRole.ADMIN) {
                return;
            }
            if (operator.getRole() == OperatorRole.STAFF && operatorId.equals(contract.getAssignedStaffId())) {
                return;
            }
        }

        throw new RuntimeException("权限不足");
    }

    private void finalizeTermination(Contract contract) {
        contract.setStatus(ContractStatus.TERMINATED);
        contractRepository.save(contract);

        House house = contract.getHouse();
        house.setStatus(HouseStatus.AVAILABLE);
        houseRepository.save(house);
    }

    @Transactional
    public void approveContract(Long contractId, Long adminOperatorId) {
        OperatorAccount account = operatorAccountRepository.findById(adminOperatorId)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        if (account.getRole() != OperatorRole.ADMIN) {
            throw new RuntimeException("权限不足");
        }

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        if (contract.getStatus() != ContractStatus.PENDING_ADMIN_APPROVAL) {
            throw new RuntimeException("合同状态异常，无法审批");
        }

        contract.setStatus(ContractStatus.ACTIVE);
        contractRepository.save(contract);

        House house = contract.getHouse();
        house.setStatus(HouseStatus.RENTED);
        houseRepository.save(house);

        messageService.sendMessage(null, contract.getTenant().getId(), "合同审批通过通知", "合同已通过管理员审批并生效", MessageType.CONTRACT_APPROVAL_NOTICE, contract.getId(), null, false);
        messageService.sendMessage(null, contract.getLandlord().getId(), "合同审批通过通知", "合同已通过管理员审批并生效", MessageType.CONTRACT_APPROVAL_NOTICE, contract.getId(), null, false);
        if (contract.getAssignedStaffId() != null) {
            messageService.notifyStaff(contract.getAssignedStaffId(), "合同审批通过", "你跟进的合同已审批通过", contract.getId(), null, false, MessageType.CONTRACT_APPROVAL_NOTICE);
        }
    }

    @Transactional
    public void approveByLandlord(Long contractId, Long landlordId, Long preferredStaffId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        if (!contract.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("无权审批该合同");
        }

        if (contract.getStatus() != ContractStatus.PENDING_LANDLORD_APPROVAL) {
            throw new RuntimeException("合同状态异常，无法审批");
        }

        Long staffId = preferredStaffId;
        if (staffId != null) {
            OperatorAccount staff = operatorAccountRepository.findById(staffId)
                    .orElseThrow(() -> new RuntimeException("指定业务员不存在"));
            if (staff.getRole() != OperatorRole.STAFF || !Boolean.TRUE.equals(staff.getEnabled())) {
                throw new RuntimeException("指定业务员不可用");
            }
        } else {
            staffId = contract.getHouse().getAssignedStaffId();
            if (staffId == null) {
                staffId = operatorAccountService.pickRandomEnabledStaff().getId();
            }
        }

        contract.setAssignedStaffId(staffId);
        contract.setStatus(ContractStatus.PENDING_STAFF_SIGNING);
        contractRepository.save(contract);

        messageService.sendMessage(null, contract.getTenant().getId(), "合同待签约", "房东已同意，业务员将联系双方签约", MessageType.CONTRACT_SIGNING_NOTICE, contract.getId(), null, false);
        messageService.sendMessage(null, contract.getLandlord().getId(), "合同待签约", "请配合业务员安排签约", MessageType.CONTRACT_SIGNING_NOTICE, contract.getId(), null, false);
        messageService.notifyStaff(staffId, "合同待签约", "请联系房东与租客完成签约并上传合同", contract.getId(), null, true, MessageType.CONTRACT_PENDING_STAFF_SIGNING);
    }

    @Transactional
    public void uploadSignedContract(Long contractId, Long operatorId, MultipartFile file) {
        OperatorAccount operator = operatorAccountRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("业务员不存在"));
        if (operator.getRole() != OperatorRole.STAFF && operator.getRole() != OperatorRole.ADMIN) {
            throw new RuntimeException("权限不足");
        }

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        if (contract.getStatus() != ContractStatus.PENDING_STAFF_SIGNING) {
            throw new RuntimeException("当前状态不可上传签约文件");
        }

        if (operator.getRole() == OperatorRole.STAFF && !operatorId.equals(contract.getAssignedStaffId())) {
            throw new RuntimeException("非当前分配业务员，无法上传");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new RuntimeException("文件名不能为空");
        }
        String lowerName = originalName.toLowerCase();
        if (!(lowerName.endsWith(".pdf") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png"))) {
            throw new RuntimeException("仅支持 pdf/jpg/jpeg/png");
        }
        if (file.getSize() > 20L * 1024 * 1024) {
            throw new RuntimeException("文件不能超过20MB");
        }

        String ext = lowerName.substring(lowerName.lastIndexOf('.'));
        String fileName = UUID.randomUUID() + ext;
        Path dir = Paths.get("uploads", "contracts", String.valueOf(contractId));
        Path target = dir.resolve(fileName);

        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }

        contract.setSignedContractUrl("/uploads/contracts/" + contractId + "/" + fileName);
        contract.setSignedContractName(originalName);
        contract.setSignedContractUploadedAt(LocalDateTime.now());
        contract.setSignedContractUploadedBy(operatorId);
        contract.setStatus(ContractStatus.PENDING_ADMIN_APPROVAL);
        contractRepository.save(contract);

        messageService.notifyAdmins(
                "合同待管理员审核",
                String.format("合同《%s》(ID:%d) 已上传签约文件，请审核", contract.getHouse().getTitle(), contract.getId()),
                contract.getId(),
                null,
                true
        );

        messageService.sendMessage(null, contract.getTenant().getId(), "合同已提交管理员审核", "业务员已上传签约合同，等待管理员审核", MessageType.CONTRACT_PENDING_ADMIN_APPROVAL, contract.getId(), null, false);
        messageService.sendMessage(null, contract.getLandlord().getId(), "合同已提交管理员审核", "业务员已上传签约合同，等待管理员审核", MessageType.CONTRACT_PENDING_ADMIN_APPROVAL, contract.getId(), null, false);
    }

    @Transactional
    public void rejectContract(Long contractId, Long adminOperatorId, String reason) {
        OperatorAccount account = operatorAccountRepository.findById(adminOperatorId)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        if (account.getRole() != OperatorRole.ADMIN) {
            throw new RuntimeException("权限不足");
        }

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        if (contract.getStatus() != ContractStatus.PENDING_ADMIN_APPROVAL) {
            throw new RuntimeException("合同状态异常，无法审批");
        }

        contract.setStatus(ContractStatus.PENDING_STAFF_SIGNING);
        contract.setNotes(reason);
        contractRepository.save(contract);

        String rejectReason = reason == null || reason.isBlank() ? "无" : reason;
        if (contract.getAssignedStaffId() != null) {
            messageService.notifyStaff(
                    contract.getAssignedStaffId(),
                    "合同被驳回需重传",
                    "管理员驳回了合同审核，请补充/修正后重新上传。原因：" + rejectReason,
                    contract.getId(),
                    null,
                    true,
                    MessageType.CONTRACT_REJECTION_NOTICE
            );
        }

        messageService.sendMessage(null, contract.getTenant().getId(), "合同审核未通过", "管理员驳回合同，等待业务员补充重传", MessageType.CONTRACT_REJECTION_NOTICE, contract.getId(), null, false);
        messageService.sendMessage(null, contract.getLandlord().getId(), "合同审核未通过", "管理员驳回合同，等待业务员补充重传", MessageType.CONTRACT_REJECTION_NOTICE, contract.getId(), null, false);
    }

    private User resolveCounterparty(Contract contract, Long userId) {
        if (contract.getLandlord().getId().equals(userId)) {
            return contract.getTenant();
        }
        return contract.getLandlord();
    }

    private ContractDTO convertToDTO(Contract contract) {
        ContractDTO dto = new ContractDTO();
        dto.setId(contract.getId());
        dto.setHouseId(contract.getHouse().getId());
        dto.setHouseTitle(contract.getHouse().getTitle());
        dto.setHouseAddress(contract.getHouse().getAddress());
        dto.setLandlordId(contract.getLandlord().getId());
        dto.setLandlordName(contract.getLandlord().getRealName());
        dto.setLandlordPhone(contract.getLandlord().getPhone());
        dto.setTenantId(contract.getTenant().getId());
        dto.setTenantName(contract.getTenant().getRealName());
        dto.setTenantPhone(contract.getTenant().getPhone());
        dto.setRentPrice(contract.getRentPrice());
        dto.setDeposit(contract.getDeposit());
        dto.setStartDate(contract.getStartDate());
        dto.setEndDate(contract.getEndDate());
        dto.setStatus(contract.getStatus());
        dto.setSignedDate(contract.getSignedDate());
        dto.setNotes(contract.getNotes());
        dto.setCreatedAt(contract.getCreatedAt());
        dto.setAssignedStaffId(contract.getAssignedStaffId());
        dto.setSignedContractUrl(contract.getSignedContractUrl());
        dto.setSignedContractName(contract.getSignedContractName());
        dto.setSignedContractUploadedAt(contract.getSignedContractUploadedAt());

        terminationRequestRepository.findByContractIdAndStatus(contract.getId(), TerminationStatus.PENDING)
                .ifPresent(req -> {
                    dto.setTerminationStatus(req.getStatus());
                    dto.setTerminationRequestId(req.getId());
                });
        if (dto.getTerminationStatus() == null && (contract.getStatus() == ContractStatus.TERMINATION_PENDING || contract.getStatus() == ContractStatus.TERMINATION_PENDING_STAFF_REVIEW)) {
            dto.setTerminationStatus(TerminationStatus.PENDING);
        }
        return dto;
    }
}
