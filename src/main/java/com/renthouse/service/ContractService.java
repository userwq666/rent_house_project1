package com.renthouse.service;

import com.renthouse.domain.Account;
import com.renthouse.domain.Contract;
import com.renthouse.domain.House;
import com.renthouse.domain.TerminationRequest;
import com.renthouse.domain.User;
import com.renthouse.dto.ContractDTO;
import com.renthouse.dto.CreateContractRequest;
import com.renthouse.dto.TerminateContractRequest;
import com.renthouse.dto.TerminationDecisionRequest;
import com.renthouse.enums.AccountType;
import com.renthouse.enums.ContractStatus;
import com.renthouse.enums.HouseStatus;
import com.renthouse.enums.MessageType;
import com.renthouse.enums.TerminationStatus;
import com.renthouse.repository.AccountRepository;
import com.renthouse.repository.ContractRepository;
import com.renthouse.repository.HouseRepository;
import com.renthouse.repository.TerminationRequestRepository;
import com.renthouse.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 合同 Service（含权限校验与终止流程）
 */
@Service
@Slf4j
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TerminationRequestRepository terminationRequestRepository;

    @Autowired
    private MessageService messageService;

    /**
     * 创建租房合同
     */
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
        if (tenant.getAccount() != null && Boolean.FALSE.equals(tenant.getAccount().getCanRent())) {
            throw new RuntimeException("当前账号已被限制租赁");
        }

        // 管理员不能租房
        if (tenant.getAccount() != null && tenant.getAccount().getAccountType() == AccountType.ADMIN) {
            throw new RuntimeException("管理员不能租房");
        }

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
        // 初始状态：待房主审批
        contract.setStatus(ContractStatus.PENDING_LANDLORD_APPROVAL);
        contract.setSignedDate(LocalDateTime.now());
        contract.setNotes(request.getNotes());

        Contract saved = contractRepository.save(contract);
        // 待审批期间房源进入等待状态，避免继续出租
        house.setStatus(HouseStatus.PENDING);
        houseRepository.save(house);

        // 发送消息给房主，需其审批
        messageService.sendMessage(
                tenant.getId(),
                house.getOwner().getId(),
                "租房申请",
                String.format("%s 申请租用您的房源《%s》，租金：%s元/月，押金：%s元，租期：%s 至 %s",
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
        List<ContractDTO> list = contractRepository.findByUserId(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.info("getMyContracts userId={}, size={}", userId, list.size());
        return list;
    }

    @Transactional(readOnly = true)
    public List<ContractDTO> getLandlordContracts(Long userId) {
        List<ContractDTO> list = contractRepository.findByLandlordId(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.info("getLandlordContracts userId={}, size={}", userId, list.size());
        return list;
    }

    @Transactional(readOnly = true)
    public List<ContractDTO> getTenantContracts(Long userId) {
        List<ContractDTO> list = contractRepository.findByTenantId(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.info("getTenantContracts userId={}, size={}", userId, list.size());
        return list;
    }

    public ContractDTO getContractById(Long contractId, Long userId, Long accountId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));
        checkContractPermission(contract, userId, accountId);
        return convertToDTO(contract);
    }

    /**
     * 发起终止申请或强制终止
     */
    @Transactional
    public void submitTerminationRequest(Long contractId, Long userId, Long accountId, TerminateContractRequest request) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        checkContractPermission(contract, userId, accountId);

        if (Boolean.TRUE.equals(request.getForce())) {
            forceTerminate(contract, userId, request.getForceReason());
            return;
        }

        if (contract.getStatus() != ContractStatus.ACTIVE && contract.getStatus() != ContractStatus.TERMINATION_PENDING) {
            throw new RuntimeException("合同状态异常，无法终止");
        }

        terminationRequestRepository.findByContractIdAndStatus(contractId, TerminationStatus.PENDING)
                .ifPresent(req -> {
                    throw new RuntimeException("已有待处理的终止申请");
                });

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        User responder = resolveCounterparty(contract, userId);

        TerminationRequest terminationRequest = new TerminationRequest();
        terminationRequest.setContract(contract);
        terminationRequest.setRequester(requester);
        terminationRequest.setResponder(responder);
        terminationRequest.setReason(request.getReason());
        terminationRequestRepository.save(terminationRequest);

        contract.setStatus(ContractStatus.TERMINATION_PENDING);
        contractRepository.save(contract);

        messageService.sendMessage(
                requester.getId(),
                responder.getId(),
                "合同终止确认",
                String.format("%s 请求终止房源《%s》的合同，原因：%s",
                        requester.getRealName(), contract.getHouse().getTitle(), request.getReason()),
                MessageType.TERMINATION_REQUEST,
                contract.getId(),
                terminationRequest.getId(),
                true
        );
    }

    /**
     * 对终止申请做出决策（同意/拒绝）
     */
    @Transactional
    public void decideTermination(Long requestId, Long userId, TerminationDecisionRequest decision) {
        TerminationRequest terminationRequest = terminationRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("终止申请不存在"));

        if (!terminationRequest.getResponder().getId().equals(userId)) {
            throw new RuntimeException("无权操作该终止申请");
        }

        if (terminationRequest.getStatus() != TerminationStatus.PENDING) {
            throw new RuntimeException("终止申请已处理");
        }

        Contract contract = terminationRequest.getContract();
        User responder = terminationRequest.getResponder();
        User requester = terminationRequest.getRequester();

        if (Boolean.TRUE.equals(decision.getApprove())) {
            terminationRequest.setStatus(TerminationStatus.APPROVED);
            terminationRequestRepository.save(terminationRequest);

            // 房主同意后，合同状态改为待管理员审批，而不是直接终止
            contract.setStatus(ContractStatus.PENDING_ADMIN_APPROVAL);
            contractRepository.save(contract);

            // 发送消息给管理员进行审批（需要操作）
            messageService.notifyAdmins(
                    "合同终止申请待审批",
                    String.format("房主已同意终止合同《%s》（合同 ID：%d），请审批", 
                            contract.getHouse().getTitle(), contract.getId()),
                    contract.getId(),
                    terminationRequest.getId(),
                    true
            );

            // 发送消息给租客，告知房主已同意，等待管理员审批
            messageService.sendMessage(
                    responder.getId(),
                    requester.getId(),
                    "合同终止已确认",
                    String.format("%s 同意终止房源《%s》的合同，已提交管理员审批", 
                            responder.getRealName(), contract.getHouse().getTitle()),
                    MessageType.TERMINATION_RESPONSE,
                    contract.getId(),
                    terminationRequest.getId(),
                    false
            );
        } else {
            terminationRequest.setStatus(TerminationStatus.REJECTED);
            terminationRequest.setForceReason(decision.getComment());
            terminationRequestRepository.save(terminationRequest);

            contract.setStatus(ContractStatus.ACTIVE);
            contractRepository.save(contract);

            messageService.sendMessage(
                    responder.getId(),
                    requester.getId(),
                    "合同终止被拒绝",
                    String.format("%s 拒绝终止合同，说明：%s",
                            responder.getRealName(),
                            decision.getComment() == null ? "无" : decision.getComment()),
                    MessageType.TERMINATION_RESPONSE,
                    contract.getId(),
                    terminationRequest.getId(),
                    false
            );
        }
    }

    /**
     * 管理员直接终止合同
     */
    @Transactional
    public void adminTerminate(Long contractId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        if (account.getAccountType() != AccountType.ADMIN) {
            throw new RuntimeException("权限不足");
        }
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));
        finalizeTermination(contract);
    }

    @Transactional(readOnly = true)
    public List<ContractDTO> getAllContracts(Long accountId) {
        AccountType accountType = accountRepository.findById(accountId)
                .map(Account::getAccountType)
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        if (accountType != AccountType.ADMIN) {
            throw new RuntimeException("权限不足");
        }

        return contractRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private void checkContractPermission(Contract contract, Long userId, Long accountId) {
        if (contract.getLandlord().getId().equals(userId) ||
                contract.getTenant().getId().equals(userId)) {
            return;
        }

        AccountType accountType = accountRepository.findById(accountId)
                .map(Account::getAccountType)
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        if (accountType != AccountType.ADMIN) {
            throw new RuntimeException("权限不足：只能操作自己相关的合同");
        }
    }

    private void finalizeTermination(Contract contract) {
        contract.setStatus(ContractStatus.TERMINATED);
        contractRepository.save(contract);

        House house = contract.getHouse();
        house.setStatus(HouseStatus.AVAILABLE);
        houseRepository.save(house);
    }

    /**
     * 管理员审批通过合同（支持新合同审批和终止申请审批）
     */
    @Transactional
    public void approveContract(Long contractId, Long adminAccountId) {
        Account account = accountRepository.findById(adminAccountId)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        if (account.getAccountType() != AccountType.ADMIN) {
            throw new RuntimeException("权限不足");
        }

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        if (contract.getStatus() != ContractStatus.PENDING_ADMIN_APPROVAL) {
            throw new RuntimeException("合同状态异常，无法审批");
        }

        // 检查是否有终止申请
        boolean hasTerminationRequest = terminationRequestRepository
                .findByContractIdAndStatus(contractId, TerminationStatus.APPROVED)
                .isPresent();

        if (hasTerminationRequest) {
            // 如果是终止申请，执行终止逻辑
            finalizeTermination(contract);

            // 发送系统消息通知租客和房主合同已终止
            messageService.sendMessage(
                    null,
                    contract.getTenant().getId(),
                    "合同终止已审批通过",
                    String.format("您的租房合同《%s》的终止申请已由管理员审批通过，合同正式终止。", 
                            contract.getHouse().getTitle()),
                    MessageType.CONTRACT_APPROVAL_NOTICE,
                    contract.getId(),
                    null,
                    false
            );

            messageService.sendMessage(
                    null,
                    contract.getLandlord().getId(),
                    "合同终止已审批通过",
                    String.format("租房合同《%s》的终止申请已由管理员审批通过，合同正式终止。", 
                            contract.getHouse().getTitle()),
                    MessageType.CONTRACT_APPROVAL_NOTICE,
                    contract.getId(),
                    null,
                    false
            );
        } else {
            // 如果是新合同，执行生效逻辑
            contract.setStatus(ContractStatus.ACTIVE);
            contractRepository.save(contract);

            // 合同生效后将房源标记为已出租，保证不再出现在可租列表
            House house = contract.getHouse();
            house.setStatus(HouseStatus.RENTED);
            houseRepository.save(house);

            // 发送系统消息通知租客和房主合同已生效
            messageService.sendMessage(
                    null,
                    contract.getTenant().getId(),
                    "合同审批通过通知",
                    String.format("您的租房合同《%s》已由管理员审批通过，正式生效。您可以与房主联系办理入住手续。", 
                            contract.getHouse().getTitle()),
                    MessageType.CONTRACT_APPROVAL_NOTICE,
                    contract.getId(),
                    null,
                    false
            );

            messageService.sendMessage(
                    null,
                    contract.getLandlord().getId(),
                    "合同审批通过通知",
                    String.format("您的租房合同《%s》已由管理员审批通过，正式生效。请协助租客办理入住手续。", 
                            contract.getHouse().getTitle()),
                    MessageType.CONTRACT_APPROVAL_NOTICE,
                    contract.getId(),
                    null,
                    false
            );
        }
    }

    /**
     * 房主审批合同 -> 提交管理员审批
     */
    @Transactional
    public void approveByLandlord(Long contractId, Long landlordId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));
    
        if (!contract.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("无权审批该合同");
        }
    
        if (contract.getStatus() != ContractStatus.PENDING_LANDLORD_APPROVAL) {
            throw new RuntimeException("合同状态异常，无法审批");
        }
    
        contract.setStatus(ContractStatus.PENDING_ADMIN_APPROVAL);
        contractRepository.save(contract);
    
        // 通知租客合同进入管理员审批（系统消息）
        messageService.sendMessage(
                null,
                contract.getTenant().getId(),
                "合同已提交管理员审批",
                String.format("房主已同意租用房源《%s》，合同 ID：%d，现已提交管理员审批", 
                        contract.getHouse().getTitle(), contract.getId()),
                MessageType.ADMIN_NOTIFICATION,
                contract.getId(),
                null,
                false
        );
    
        // 发送消息给管理员进行审批（需要操作）
        messageService.notifyAdmins(
                "新合同待审批",
                String.format("房主已同意合同《%s》（合同 ID：%d），请审批", contract.getHouse().getTitle(), contract.getId()),
                contract.getId(),
                null,
                true
        );
    }
    
    /**
     * 管理员拒绝合同
     */
    @Transactional
    public void rejectContract(Long contractId, Long adminAccountId, String reason) {
        Account account = accountRepository.findById(adminAccountId)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        if (account.getAccountType() != AccountType.ADMIN) {
            throw new RuntimeException("权限不足");
        }
    
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));
    
        if (contract.getStatus() != ContractStatus.PENDING_ADMIN_APPROVAL) {
            throw new RuntimeException("合同状态异常，无法审批");
        }
    
        // 设置合同状态为终止
        contract.setStatus(ContractStatus.TERMINATED);
        contract.setNotes(reason);
        contractRepository.save(contract);
    
        // 将房源恢复为可租状态
        House house = contract.getHouse();
        house.setStatus(HouseStatus.AVAILABLE);
        houseRepository.save(house);
    
        // 发送系统消息通知租客和房主合同被拒绝
        messageService.sendMessage(
                null,
                contract.getTenant().getId(),
                "合同审批未通过",
                String.format("很遗憾，您的租房合同《%s》未通过管理员审批。原因：%s", 
                        contract.getHouse().getTitle(), reason == null ? "无" : reason),
                MessageType.CONTRACT_REJECTION_NOTICE,
                contract.getId(),
                null,
                false
        );
    
        messageService.sendMessage(
                null,
                contract.getLandlord().getId(),
                "合同审批未通过",
                String.format("租房合同《%s》未通过管理员审批。原因：%s", 
                        contract.getHouse().getTitle(), reason == null ? "无" : reason),
                MessageType.CONTRACT_REJECTION_NOTICE,
                contract.getId(),
                null,
                false
        );
    }

    private void forceTerminate(Contract contract, Long userId, String forceReason) {
        if (forceReason == null || forceReason.isBlank()) {
            throw new RuntimeException("请填写强制终止原因");
        }

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        User responder = resolveCounterparty(contract, userId);

        finalizeTermination(contract);

        TerminationRequest terminationRequest = new TerminationRequest();
        terminationRequest.setContract(contract);
        terminationRequest.setRequester(requester);
        terminationRequest.setResponder(responder);
        terminationRequest.setForceReason(forceReason);
        terminationRequest.setStatus(TerminationStatus.FORCE_TERMINATED);
        terminationRequestRepository.save(terminationRequest);

        if (requester.getAccount() != null) {
            requester.getAccount().setCanPublish(false);
            requester.getAccount().setCanRent(false);
            accountRepository.save(requester.getAccount());
        }

        messageService.sendMessage(
            requester.getId(),
            responder.getId(),
            "合同被强制终止",
            String.format("%s 强制终止了房源《%s》的合同，原因：%s",
                    requester.getRealName(), contract.getHouse().getTitle(), forceReason),
            MessageType.FORCE_TERMINATION_NOTICE,
            contract.getId(),
            terminationRequest.getId(),
            false
        );

        messageService.notifyAdmins(
                "强制终止告警",
                String.format("%s 强制终止合同《%s》（合同ID：%d），原因：%s",
                        requester.getRealName(), contract.getHouse().getTitle(), contract.getId(), forceReason),
                contract.getId(),
                terminationRequest.getId(),
                false
        );
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

        terminationRequestRepository.findByContractIdAndStatus(contract.getId(), TerminationStatus.PENDING)
                .ifPresent(req -> {
                    dto.setTerminationStatus(req.getStatus());
                    dto.setTerminationRequestId(req.getId());
                });
        if (dto.getTerminationStatus() == null && contract.getStatus() == ContractStatus.TERMINATION_PENDING) {
            dto.setTerminationStatus(TerminationStatus.PENDING);
        }
        return dto;
    }
}
