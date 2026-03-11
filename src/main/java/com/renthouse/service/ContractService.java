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
import java.util.Objects;
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

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException("合同状态异常，无法终止");
        }

        if (terminationRequestRepository.existsByContractIdAndStatus(contractId, TerminationStatus.PENDING)) {
            throw new RuntimeException("已有待处理的终止申请");
        }

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        User counterparty = resolveCounterparty(contract, userId);

        Long staffId = contract.getAssignedStaffId();
        if (staffId == null) {
            staffId = operatorAccountService.pickRandomEnabledStaff().getId();
            contract.setAssignedStaffId(staffId);
        }

        TerminationRequest terminationRequest = new TerminationRequest();
        terminationRequest.setContract(contract);
        terminationRequest.setRequester(requester);
        terminationRequest.setResponder(counterparty);
        terminationRequest.setReviewStaffId(staffId);
        boolean force = Boolean.TRUE.equals(request.getForce());

        if (force) {
            String forceReason = request.getForceReason() == null ? "" : request.getForceReason().trim();
            String evidenceUrls = request.getEvidenceUrls() == null ? "" : request.getEvidenceUrls().trim();
            if (forceReason.isEmpty()) {
                throw new RuntimeException("强制终止必须填写原因");
            }
            if (evidenceUrls.isEmpty()) {
                throw new RuntimeException("强制终止必须上传证据附件");
            }
            int rejectCount = getRequesterRejectCount(contract, userId);
            if (rejectCount < 3) {
                throw new RuntimeException("普通终止被拒次数未达到3次，不能发起强制终止");
            }

            terminationRequest.setForceRequest(true);
            terminationRequest.setForceReason(forceReason);
            terminationRequest.setEvidenceUrls(evidenceUrls);
            terminationRequest.setReason(request.getReason());
            terminationRequestRepository.save(terminationRequest);

            contract.setStatus(ContractStatus.TERMINATION_FORCE_PENDING_JOINT_REVIEW);
            contractRepository.save(contract);

            messageService.notifyStaff(
                    staffId,
                    "强制终止待联合审核",
                    String.format("合同《%s》(ID:%d) 触发强制终止，请提交后续处理方案", contract.getHouse().getTitle(), contract.getId()),
                    contract.getId(),
                    terminationRequest.getId(),
                    true,
                    MessageType.FORCE_TERMINATION_NOTICE
            );
            messageService.notifyAdmins(
                    "强制终止待联合审核",
                    String.format("合同《%s》(ID:%d) 发起强制终止，请进行管理员裁决", contract.getHouse().getTitle(), contract.getId()),
                    contract.getId(),
                    null,
                    terminationRequest.getId(),
                    true,
                    MessageType.FORCE_TERMINATION_NOTICE
            );
            messageService.sendMessage(
                    null,
                    counterparty.getId(),
                    "强制终止申请已提交",
                    "对方已发起强制终止，业务员与管理员将联合审核",
                    MessageType.FORCE_TERMINATION_NOTICE,
                    contract.getId(),
                    terminationRequest.getId(),
                    false
            );
            return;
        }

        String reason = request.getReason() == null ? "" : request.getReason().trim();
        if (reason.isEmpty()) {
            throw new RuntimeException("终止原因不能为空");
        }
        terminationRequest.setForceRequest(false);
        terminationRequest.setReason(reason);
        terminationRequestRepository.save(terminationRequest);

        contract.setStatus(ContractStatus.TERMINATION_PENDING_COUNTERPARTY);
        contractRepository.save(contract);

        messageService.sendMessage(
                null,
                counterparty.getId(),
                "合同终止待你确认",
                String.format("对方申请终止合同《%s》，请同意或拒绝", contract.getHouse().getTitle()),
                MessageType.TERMINATION_REQUEST,
                contract.getId(),
                terminationRequest.getId(),
                true
        );
        messageService.sendMessage(
                null,
                requester.getId(),
                "终止申请已提交",
                "已提交给对方确认，待对方同意后进入业务员审核",
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
        boolean approve = Boolean.TRUE.equals(decision.getApprove());
        String comment = decision.getComment() == null ? "" : decision.getComment().trim();

        if (Boolean.TRUE.equals(terminationRequest.getForceRequest())) {
            handleForceTerminationDecision(terminationRequest, contract, operator, approve, comment);
            return;
        }

        if (contract.getStatus() != ContractStatus.TERMINATION_PENDING_STAFF_REVIEW) {
            throw new RuntimeException("当前不是业务员终止审核阶段");
        }
        if (operator.getRole() != OperatorRole.STAFF) {
            throw new RuntimeException("普通终止仅业务员可审核");
        }

        if (approve) {
            terminationRequest.setStatus(TerminationStatus.APPROVED);
            terminationRequestRepository.save(terminationRequest);
            finalizeTermination(contract);

            messageService.sendMessage(null, contract.getLandlord().getId(), "合同已终止", "业务员已审核通过，合同已终止", MessageType.TERMINATION_RESPONSE, contract.getId(), terminationRequest.getId(), false);
            messageService.sendMessage(null, contract.getTenant().getId(), "合同已终止", "业务员已审核通过，合同已终止", MessageType.TERMINATION_RESPONSE, contract.getId(), terminationRequest.getId(), false);
        } else {
            terminationRequest.setStatus(TerminationStatus.REJECTED);
            terminationRequest.setStaffFollowUpPlan(comment);
            terminationRequestRepository.save(terminationRequest);
            contract.setStatus(ContractStatus.ACTIVE);
            contractRepository.save(contract);

            String rejectReason = comment.isEmpty() ? "无" : comment;
            messageService.sendMessage(null, contract.getLandlord().getId(), "终止申请被驳回", "业务员驳回终止申请，说明：" + rejectReason, MessageType.TERMINATION_RESPONSE, contract.getId(), terminationRequest.getId(), false);
            messageService.sendMessage(null, contract.getTenant().getId(), "终止申请被驳回", "业务员驳回终止申请，说明：" + rejectReason, MessageType.TERMINATION_RESPONSE, contract.getId(), terminationRequest.getId(), false);
        }
    }

    @Transactional
    public void decideByCounterparty(Long requestId, Long userId, com.renthouse.dto.TerminationCounterpartyDecisionRequest decision) {
        TerminationRequest terminationRequest = terminationRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("终止申请不存在"));
        Contract contract = terminationRequest.getContract();

        if (terminationRequest.getStatus() != TerminationStatus.PENDING) {
            throw new RuntimeException("终止申请已处理");
        }
        if (Boolean.TRUE.equals(terminationRequest.getForceRequest())) {
            throw new RuntimeException("强制终止不走对方确认流程");
        }
        if (contract.getStatus() != ContractStatus.TERMINATION_PENDING_COUNTERPARTY) {
            throw new RuntimeException("当前不在对方确认阶段");
        }
        if (terminationRequest.getResponder() == null || !Objects.equals(terminationRequest.getResponder().getId(), userId)) {
            throw new RuntimeException("无权处理该终止申请");
        }

        boolean approve = Boolean.TRUE.equals(decision.getApprove());
        String comment = decision.getComment() == null ? "" : decision.getComment().trim();
        terminationRequest.setCounterpartyComment(comment);
        if (approve) {
            contract.setStatus(ContractStatus.TERMINATION_PENDING_STAFF_REVIEW);
            contractRepository.save(contract);
            terminationRequestRepository.save(terminationRequest);

            messageService.notifyStaff(
                    terminationRequest.getReviewStaffId(),
                    "合同终止待审核",
                    String.format("合同《%s》(ID:%d) 已经双方确认，请审核", contract.getHouse().getTitle(), contract.getId()),
                    contract.getId(),
                    terminationRequest.getId(),
                    true,
                    MessageType.TERMINATION_PENDING_STAFF_REVIEW
            );
            messageService.sendMessage(null, terminationRequest.getRequester().getId(), "对方已同意终止", "已进入业务员审核阶段", MessageType.TERMINATION_RESPONSE, contract.getId(), terminationRequest.getId(), false);
        } else {
            terminationRequest.setStatus(TerminationStatus.REJECTED);
            terminationRequestRepository.save(terminationRequest);
            contract.setStatus(ContractStatus.ACTIVE);
            increaseRejectCountForRequester(contract, terminationRequest.getRequester().getId());
            contractRepository.save(contract);

            int rejectCount = getRequesterRejectCount(contract, terminationRequest.getRequester().getId());
            String rejectReason = comment.isEmpty() ? "无" : comment;
            String extra = rejectCount >= 3 ? "（已达到3次，可发起强制终止）" : "";
            messageService.sendMessage(
                    null,
                    terminationRequest.getRequester().getId(),
                    "终止申请被对方拒绝",
                    "拒绝原因：" + rejectReason + "；累计被拒次数：" + rejectCount + extra,
                    MessageType.TERMINATION_RESPONSE,
                    contract.getId(),
                    terminationRequest.getId(),
                    false
            );
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
    public void rejectByLandlord(Long contractId, Long landlordId, String reason) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        if (!contract.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("无权审批该合同");
        }

        if (contract.getStatus() != ContractStatus.PENDING_LANDLORD_APPROVAL) {
            throw new RuntimeException("合同状态异常，无法拒绝");
        }

        String rejectReason = reason == null || reason.isBlank() ? "房东拒绝租房申请" : reason.trim();
        contract.setStatus(ContractStatus.REJECTED);
        contract.setNotes(rejectReason);
        contractRepository.save(contract);

        House house = contract.getHouse();
        house.setStatus(HouseStatus.AVAILABLE);
        houseRepository.save(house);

        messageService.sendMessage(
                null,
                contract.getTenant().getId(),
                "租房申请被拒绝",
                "房东拒绝了本次租房申请，原因：" + rejectReason,
                MessageType.CONTRACT_REJECTION_NOTICE,
                contract.getId(),
                null,
                false
        );
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
                null,
                true,
                MessageType.CONTRACT_PENDING_ADMIN_APPROVAL
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
        dto.setLandlordTerminationRejectCount(contract.getLandlordTerminationRejectCount() == null ? 0 : contract.getLandlordTerminationRejectCount());
        dto.setTenantTerminationRejectCount(contract.getTenantTerminationRejectCount() == null ? 0 : contract.getTenantTerminationRejectCount());

        terminationRequestRepository.findTopByContractIdOrderByCreatedAtDesc(contract.getId())
                .ifPresent(req -> {
                    dto.setTerminationStatus(req.getStatus());
                    dto.setTerminationRequestId(req.getId());
                    dto.setTerminationRequesterId(req.getRequester() == null ? null : req.getRequester().getId());
                    dto.setTerminationResponderId(req.getResponder() == null ? null : req.getResponder().getId());
                    dto.setForceTermination(req.getForceRequest());
                    dto.setTerminationEvidenceUrls(req.getEvidenceUrls());
                });
        if (dto.getTerminationStatus() == null && (contract.getStatus() == ContractStatus.TERMINATION_PENDING
                || contract.getStatus() == ContractStatus.TERMINATION_PENDING_COUNTERPARTY
                || contract.getStatus() == ContractStatus.TERMINATION_PENDING_STAFF_REVIEW
                || contract.getStatus() == ContractStatus.TERMINATION_FORCE_PENDING_JOINT_REVIEW)) {
            dto.setTerminationStatus(TerminationStatus.PENDING);
        }
        return dto;
    }

    private int getRequesterRejectCount(Contract contract, Long requesterId) {
        if (Objects.equals(contract.getLandlord().getId(), requesterId)) {
            return contract.getLandlordTerminationRejectCount() == null ? 0 : contract.getLandlordTerminationRejectCount();
        }
        return contract.getTenantTerminationRejectCount() == null ? 0 : contract.getTenantTerminationRejectCount();
    }

    private void increaseRejectCountForRequester(Contract contract, Long requesterId) {
        if (Objects.equals(contract.getLandlord().getId(), requesterId)) {
            int current = contract.getLandlordTerminationRejectCount() == null ? 0 : contract.getLandlordTerminationRejectCount();
            contract.setLandlordTerminationRejectCount(current + 1);
            return;
        }
        int current = contract.getTenantTerminationRejectCount() == null ? 0 : contract.getTenantTerminationRejectCount();
        contract.setTenantTerminationRejectCount(current + 1);
    }

    private void handleForceTerminationDecision(TerminationRequest request,
                                                Contract contract,
                                                OperatorAccount operator,
                                                boolean approve,
                                                String comment) {
        if (contract.getStatus() != ContractStatus.TERMINATION_FORCE_PENDING_JOINT_REVIEW) {
            throw new RuntimeException("当前不在强制终止联合审核阶段");
        }

        if (!approve) {
            request.setStatus(TerminationStatus.REJECTED);
            if (operator.getRole() == OperatorRole.STAFF) {
                request.setStaffFollowUpPlan(comment);
            } else {
                request.setAdminDecisionComment(comment);
            }
            terminationRequestRepository.save(request);
            contract.setStatus(ContractStatus.ACTIVE);
            contractRepository.save(contract);
            messageService.sendMessage(null, contract.getLandlord().getId(), "强制终止申请被驳回", "强制终止联合审核未通过", MessageType.FORCE_TERMINATION_NOTICE, contract.getId(), request.getId(), false);
            messageService.sendMessage(null, contract.getTenant().getId(), "强制终止申请被驳回", "强制终止联合审核未通过", MessageType.FORCE_TERMINATION_NOTICE, contract.getId(), request.getId(), false);
            return;
        }

        if (operator.getRole() == OperatorRole.STAFF) {
            if (comment.isEmpty()) {
                throw new RuntimeException("业务员通过强制终止时必须填写后续方案");
            }
            request.setStaffApproved(true);
            request.setStaffFollowUpPlan(comment);
        } else {
            if (comment.isEmpty()) {
                throw new RuntimeException("管理员通过强制终止时请填写裁决说明");
            }
            request.setAdminApproved(true);
            request.setAdminDecisionComment(comment);
        }
        terminationRequestRepository.save(request);

        if (Boolean.TRUE.equals(request.getStaffApproved()) && Boolean.TRUE.equals(request.getAdminApproved())) {
            request.setStatus(TerminationStatus.APPROVED);
            terminationRequestRepository.save(request);
            finalizeTermination(contract);
            messageService.sendMessage(null, contract.getLandlord().getId(), "强制终止已通过", "业务员与管理员联合审核通过，合同已终止", MessageType.FORCE_TERMINATION_NOTICE, contract.getId(), request.getId(), false);
            messageService.sendMessage(null, contract.getTenant().getId(), "强制终止已通过", "业务员与管理员联合审核通过，合同已终止", MessageType.FORCE_TERMINATION_NOTICE, contract.getId(), request.getId(), false);
            return;
        }

        if (operator.getRole() == OperatorRole.STAFF) {
            messageService.notifyAdmins(
                    "强制终止待管理员裁决",
                    String.format("合同《%s》(ID:%d) 业务员已提交方案，请管理员裁决", contract.getHouse().getTitle(), contract.getId()),
                    contract.getId(),
                    null,
                    request.getId(),
                    true,
                    MessageType.FORCE_TERMINATION_NOTICE
            );
        } else {
            messageService.notifyStaff(
                    request.getReviewStaffId(),
                    "强制终止待业务员方案",
                    String.format("合同《%s》(ID:%d) 管理员已裁决，请业务员补充后续方案", contract.getHouse().getTitle(), contract.getId()),
                    contract.getId(),
                    request.getId(),
                    true,
                    MessageType.FORCE_TERMINATION_NOTICE
            );
        }
    }
}
