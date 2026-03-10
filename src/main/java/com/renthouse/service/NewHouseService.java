package com.renthouse.service;

import com.renthouse.domain.Contract;
import com.renthouse.domain.House;
import com.renthouse.domain.OperatorAccount;
import com.renthouse.domain.User;
import com.renthouse.dto.CreateHouseRequest;
import com.renthouse.dto.HouseDTO;
import com.renthouse.enums.ContractStatus;
import com.renthouse.enums.HouseStatus;
import com.renthouse.enums.MessageType;
import com.renthouse.enums.OperatorRole;
import com.renthouse.repository.ContractRepository;
import com.renthouse.repository.HouseRepository;
import com.renthouse.repository.OperatorAccountRepository;
import com.renthouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewHouseService {

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OperatorAccountService operatorAccountService;

    @Autowired
    private OperatorAccountRepository operatorAccountRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private MessageService messageService;

    public List<HouseDTO> getAvailableHouses() {
        return houseRepository.findByStatus(HouseStatus.AVAILABLE)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<HouseDTO> searchHouses(String district, BigDecimal minPrice, BigDecimal maxPrice) {
        return houseRepository.searchHouses(HouseStatus.AVAILABLE, district, minPrice, maxPrice)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public HouseDTO getHouseById(Long id) {
        House house = houseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("房源不存在"));

        house.setViewCount(house.getViewCount() + 1);
        houseRepository.save(house);

        return convertToDTO(house);
    }

    public List<HouseDTO> getMyHouses(Long userId) {
        return houseRepository.findByOwnerId(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<HouseDTO> getAllHouses() {
        return houseRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<HouseDTO> getStaffPendingHouses(Long operatorId) {
        OperatorAccount operator = operatorAccountRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("业务员不存在"));
        if (operator.getRole() != OperatorRole.STAFF && operator.getRole() != OperatorRole.ADMIN) {
            throw new RuntimeException("权限不足");
        }
        return houseRepository.findAll().stream()
                .filter(h -> h.getStatus() == HouseStatus.PENDING_STAFF_REVIEW)
                .filter(h -> operator.getRole() == OperatorRole.ADMIN || operatorId.equals(h.getAssignedStaffId()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public HouseDTO createHouse(CreateHouseRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getAccount() != null && Boolean.FALSE.equals(user.getAccount().getCanPublish())) {
            throw new RuntimeException("当前账号已被限制发布房源");
        }

        OperatorAccount assignedStaff = operatorAccountService.pickRandomEnabledStaff();

        House house = new House();
        house.setOwner(user);
        house.setTitle(request.getTitle());
        house.setAddress(request.getAddress());
        house.setDistrict(request.getDistrict());
        house.setHouseType(request.getHouseType());
        house.setArea(request.getArea());
        house.setFloor(request.getFloor());
        house.setRentPrice(request.getRentPrice());
        house.setDeposit(request.getDeposit());
        house.setDescription(request.getDescription());
        house.setImages(request.getImages());
        house.setFacilities(request.getFacilities());
        house.setStatus(HouseStatus.PENDING_STAFF_REVIEW);
        house.setAssignedStaffId(assignedStaff.getId());
        house.setViewCount(0);

        House saved = houseRepository.save(house);

        messageService.notifyStaff(
                assignedStaff.getId(),
                "房源待审核",
                String.format("房东 %s 发布了新房源《%s》，请审核", user.getRealName(), saved.getTitle()),
                null,
                saved.getId(),
                null,
                true,
                MessageType.HOUSE_PENDING_STAFF_REVIEW
        );

        return convertToDTO(saved);
    }

    @Transactional
    public HouseDTO approveHouseByStaff(Long houseId, Long operatorId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new RuntimeException("房源不存在"));
        checkStaffAssignment(house, operatorId);
        if (house.getStatus() != HouseStatus.PENDING_STAFF_REVIEW) {
            throw new RuntimeException("当前状态不支持审核");
        }
        house.setStatus(HouseStatus.AVAILABLE);
        house.setReviewedAt(LocalDateTime.now());
        house.setReviewComment("审核通过");
        House saved = houseRepository.save(house);

        messageService.sendMessage(
                null,
                house.getOwner().getId(),
                "房源审核通过",
                String.format("房源《%s》已通过业务员审核并上架", house.getTitle()),
                MessageType.HOUSE_REVIEW_RESULT,
                null,
                house.getId(),
                null,
                false
        );
        return convertToDTO(saved);
    }

    @Transactional
    public HouseDTO rejectHouseByStaff(Long houseId, Long operatorId, String reason) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new RuntimeException("房源不存在"));
        checkStaffAssignment(house, operatorId);
        if (house.getStatus() != HouseStatus.PENDING_STAFF_REVIEW) {
            throw new RuntimeException("当前状态不支持审核");
        }
        house.setStatus(HouseStatus.OFFLINE);
        house.setReviewedAt(LocalDateTime.now());
        house.setReviewComment(reason == null || reason.isBlank() ? "审核未通过" : reason);
        House saved = houseRepository.save(house);

        messageService.sendMessage(
                null,
                house.getOwner().getId(),
                "房源审核未通过",
                String.format("房源《%s》审核未通过，原因：%s", house.getTitle(), house.getReviewComment()),
                MessageType.HOUSE_REVIEW_RESULT,
                null,
                house.getId(),
                null,
                false
        );
        return convertToDTO(saved);
    }

    @Transactional
    public HouseDTO updateHouse(Long houseId, CreateHouseRequest request, Long userId, Long operatorId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new RuntimeException("房源不存在"));

        checkHousePermission(house, userId, operatorId);

        if (isHouseLocked(house.getId())) {
            throw new RuntimeException("该房源有待审批/进行中的合同，暂不支持编辑");
        }

        if (house.getStatus() == HouseStatus.RENTED) {
            throw new RuntimeException("房源已出租，暂不支持编辑");
        }

        house.setTitle(request.getTitle());
        house.setAddress(request.getAddress());
        house.setDistrict(request.getDistrict());
        house.setHouseType(request.getHouseType());
        house.setArea(request.getArea());
        house.setFloor(request.getFloor());
        house.setRentPrice(request.getRentPrice());
        house.setDeposit(request.getDeposit());
        house.setDescription(request.getDescription());
        house.setImages(request.getImages());
        house.setFacilities(request.getFacilities());
        house.setStatus(HouseStatus.PENDING_STAFF_REVIEW);

        OperatorAccount assignedStaff = operatorAccountService.pickRandomEnabledStaff();
        house.setAssignedStaffId(assignedStaff.getId());

        House updated = houseRepository.save(house);
        messageService.notifyStaff(
                assignedStaff.getId(),
                "房源待复审",
                String.format("房源《%s》已更新，请复审", house.getTitle()),
                null,
                updated.getId(),
                null,
                true,
                MessageType.HOUSE_PENDING_STAFF_REVIEW
        );
        return convertToDTO(updated);
    }

    @Transactional
    public void offlineHouse(Long houseId, Long userId, Long operatorId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new RuntimeException("房源不存在"));

        checkHousePermission(house, userId, operatorId);

        if (isHouseLocked(house.getId())) {
            throw new RuntimeException("该房源有待审批/进行中的合同，暂不可下架");
        }

        if (house.getStatus() == HouseStatus.RENTED) {
            throw new RuntimeException("房源已出租，无法下架");
        }

        house.setStatus(HouseStatus.OFFLINE);
        houseRepository.save(house);
    }

    @Transactional
    public void onlineHouse(Long houseId, Long userId, Long operatorId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new RuntimeException("房源不存在"));

        checkHousePermission(house, userId, operatorId);

        if (isHouseLocked(house.getId())) {
            throw new RuntimeException("该房源有待审批/进行中的合同，暂不可上架");
        }

        if (house.getStatus() == HouseStatus.RENTED) {
            throw new RuntimeException("房源已出租，无法上架");
        }

        if (house.getStatus() == HouseStatus.PENDING_STAFF_REVIEW) {
            throw new RuntimeException("房源审核中，无法直接上架");
        }

        house.setStatus(HouseStatus.AVAILABLE);
        houseRepository.save(house);
    }

    @Transactional
    public void deleteHouse(Long houseId, Long userId, Long operatorId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new RuntimeException("房源不存在"));

        checkHousePermission(house, userId, operatorId);

        if (isHouseLocked(house.getId())) {
            throw new RuntimeException("该房源有待审批/进行中的合同，暂不可删除");
        }

        if (house.getStatus() == HouseStatus.RENTED) {
            throw new RuntimeException("房源已出租，无法删除");
        }

        houseRepository.delete(house);
    }

    private void checkStaffAssignment(House house, Long operatorId) {
        OperatorAccount operator = operatorAccountRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("业务员不存在"));
        if (operator.getRole() == OperatorRole.ADMIN) {
            return;
        }
        if (operator.getRole() != OperatorRole.STAFF) {
            throw new RuntimeException("权限不足");
        }
        if (!operatorId.equals(house.getAssignedStaffId())) {
            throw new RuntimeException("非当前分配业务员，无法审核");
        }
    }

    private void checkHousePermission(House house, Long userId, Long operatorId) {
        if (userId != null && house.getOwner().getId().equals(userId)) {
            return;
        }
        if (operatorId != null) {
            OperatorAccount operator = operatorAccountRepository.findById(operatorId)
                    .orElseThrow(() -> new RuntimeException("操作员不存在"));
            if (operator.getRole() == OperatorRole.ADMIN) {
                return;
            }
        }
        throw new RuntimeException("权限不足：只能操作自己的房源");
    }

    private HouseDTO convertToDTO(House house) {
        HouseDTO dto = new HouseDTO();
        dto.setId(house.getId());
        dto.setOwnerId(house.getOwner().getId());
        dto.setOwnerName(house.getOwner().getRealName());
        dto.setTitle(house.getTitle());
        dto.setAddress(house.getAddress());
        dto.setDistrict(house.getDistrict());
        dto.setHouseType(house.getHouseType());
        dto.setArea(house.getArea());
        dto.setFloor(house.getFloor());
        dto.setRentPrice(house.getRentPrice());
        dto.setDeposit(house.getDeposit());
        dto.setStatus(house.getStatus());
        dto.setDescription(house.getDescription());
        dto.setImages(house.getImages());
        dto.setFacilities(house.getFacilities());
        dto.setViewCount(house.getViewCount());
        dto.setCreatedAt(house.getCreatedAt());
        dto.setUpdatedAt(house.getUpdatedAt());
        dto.setAssignedStaffId(house.getAssignedStaffId());
        dto.setReviewComment(house.getReviewComment());
        dto.setReviewedAt(house.getReviewedAt());

        contractRepository.findActiveContractByHouseId(house.getId())
                .ifPresent(contract -> {
                    dto.setCurrentTenantId(contract.getTenant().getId());
                    dto.setCurrentTenantName(contract.getTenant().getRealName());
                    dto.setCurrentTenantPhone(contract.getTenant().getPhone());
                });
        return dto;
    }

    public boolean isHouseLocked(Long houseId) {
        return contractRepository.existsByHouseIdAndStatusIn(houseId, List.of(
                ContractStatus.PENDING_LANDLORD_APPROVAL,
                ContractStatus.PENDING_STAFF_SIGNING,
                ContractStatus.PENDING_ADMIN_APPROVAL,
                ContractStatus.ACTIVE,
                ContractStatus.TERMINATION_PENDING,
                ContractStatus.TERMINATION_PENDING_COUNTERPARTY,
                ContractStatus.TERMINATION_PENDING_STAFF_REVIEW,
                ContractStatus.TERMINATION_FORCE_PENDING_JOINT_REVIEW
        ));
    }
}
