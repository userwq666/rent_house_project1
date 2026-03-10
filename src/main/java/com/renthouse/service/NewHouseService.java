package com.renthouse.service;

import com.renthouse.domain.House;
import com.renthouse.domain.User;
import com.renthouse.dto.CreateHouseRequest;
import com.renthouse.dto.HouseDTO;
import com.renthouse.enums.AccountType;
import com.renthouse.enums.HouseStatus;
import com.renthouse.repository.AccountRepository;
import com.renthouse.repository.ContractRepository;
import com.renthouse.repository.HouseRepository;
import com.renthouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 房源 Service（新版带权限校验）
 */
@Service
public class NewHouseService {

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ContractRepository contractRepository;

    /**
     * 获取所有可租房源（公开）
     */
    public List<HouseDTO> getAvailableHouses() {
        return houseRepository.findByStatus(HouseStatus.AVAILABLE)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 搜索房源（公开）
     */
    public List<HouseDTO> searchHouses(String district, BigDecimal minPrice, BigDecimal maxPrice) {
        return houseRepository.searchHouses(HouseStatus.AVAILABLE, district, minPrice, maxPrice)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取房源详情（公开，浏览次数+1）
     */
    @Transactional
    public HouseDTO getHouseById(Long id) {
        House house = houseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("房源不存在"));

        house.setViewCount(house.getViewCount() + 1);
        houseRepository.save(house);

        return convertToDTO(house);
    }

    /**
     * 获取用户发布的房源
     */
    public List<HouseDTO> getMyHouses(Long userId) {
        return houseRepository.findByOwnerId(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 管理员获取全部房源
     */
    public List<HouseDTO> getAllHouses() {
        return houseRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 发布房源
     */
    @Transactional
    public HouseDTO createHouse(CreateHouseRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getAccount() != null && Boolean.FALSE.equals(user.getAccount().getCanPublish())) {
            throw new RuntimeException("当前账号已被限制发布房源");
        }

        // 管理员不能发布房源
        if (user.getAccount() != null && user.getAccount().getAccountType() == AccountType.ADMIN) {
            throw new RuntimeException("管理员不能发布房源");
        }

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
        house.setStatus(HouseStatus.AVAILABLE);
        house.setViewCount(0);

        House saved = houseRepository.save(house);
        return convertToDTO(saved);
    }

    /**
     * 更新房源（权限：房主或管理员）
     */
    @Transactional
    public HouseDTO updateHouse(Long houseId, CreateHouseRequest request, Long userId, Long accountId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new RuntimeException("房源不存在"));

        checkHousePermission(house, userId, accountId);

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

        House updated = houseRepository.save(house);
        return convertToDTO(updated);
    }

    /**
     * 下架房源（权限：房主或管理员）
     */
    @Transactional
    public void offlineHouse(Long houseId, Long userId, Long accountId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new RuntimeException("房源不存在"));

        checkHousePermission(house, userId, accountId);

        if (isHouseLocked(house.getId())) {
            throw new RuntimeException("该房源有待审批/进行中的合同，暂不可下架");
        }

        if (house.getStatus() == HouseStatus.RENTED) {
            throw new RuntimeException("房源已出租，无法下架");
        }

        house.setStatus(HouseStatus.OFFLINE);
        houseRepository.save(house);
    }

    /**
     * 上架房源（权限：房主或管理员）
     */
    @Transactional
    public void onlineHouse(Long houseId, Long userId, Long accountId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new RuntimeException("房源不存在"));

        checkHousePermission(house, userId, accountId);

        if (isHouseLocked(house.getId())) {
            throw new RuntimeException("该房源有待审批/进行中的合同，暂不可上架");
        }

        if (house.getStatus() == HouseStatus.RENTED) {
            throw new RuntimeException("房源已出租，无法上架");
        }

        house.setStatus(HouseStatus.AVAILABLE);
        houseRepository.save(house);
    }

    /**
     * 删除房源（权限：房主或管理员）
     */
    @Transactional
    public void deleteHouse(Long houseId, Long userId, Long accountId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new RuntimeException("房源不存在"));

        checkHousePermission(house, userId, accountId);

        if (isHouseLocked(house.getId())) {
            throw new RuntimeException("该房源有待审批/进行中的合同，暂不可删除");
        }

        if (house.getStatus() == HouseStatus.RENTED) {
            throw new RuntimeException("房源已出租，无法删除");
        }

        houseRepository.delete(house);
    }

    /**
     * 权限校验：是否为房主或管理员
     */
    private void checkHousePermission(House house, Long userId, Long accountId) {
        if (house.getOwner().getId().equals(userId)) {
            return;
        }

        AccountType accountType = accountRepository.findById(accountId)
                .map(account -> account.getAccountType())
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        if (accountType != AccountType.ADMIN) {
            throw new RuntimeException("权限不足：只能操作自己的房源");
        }
    }

    /**
     * 将 House 实体转为 DTO
     */
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

        contractRepository.findActiveContractByHouseId(house.getId())
                .ifPresent(contract -> {
                    dto.setCurrentTenantId(contract.getTenant().getId());
                    dto.setCurrentTenantName(contract.getTenant().getRealName());
                    dto.setCurrentTenantPhone(contract.getTenant().getPhone());
                });
        return dto;
    }

    /**
     * 是否存在未完结（待审批/进行中/终止待确认）的合同锁定房源
     */
    private boolean isHouseLockedByContract(Long houseId) {
        return contractRepository.existsByHouseIdAndStatusIn(houseId, java.util.List.of(
                com.renthouse.enums.ContractStatus.PENDING_LANDLORD_APPROVAL,
                com.renthouse.enums.ContractStatus.PENDING_ADMIN_APPROVAL,
                com.renthouse.enums.ContractStatus.ACTIVE,
                com.renthouse.enums.ContractStatus.TERMINATION_PENDING
        ));
    }

    /**
     * 检查房屋是否被合同锁定（包括待审批状态）
     */
    public boolean isHouseLocked(Long houseId) {
        return contractRepository.existsByHouseIdAndStatusIn(houseId, java.util.List.of(
                com.renthouse.enums.ContractStatus.PENDING_LANDLORD_APPROVAL,
                com.renthouse.enums.ContractStatus.PENDING_ADMIN_APPROVAL,
                com.renthouse.enums.ContractStatus.ACTIVE,
                com.renthouse.enums.ContractStatus.TERMINATION_PENDING
        ));
    }
}
