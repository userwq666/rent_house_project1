package com.renthouse.service;

import com.renthouse.domain.Account;
import com.renthouse.domain.Contract;
import com.renthouse.domain.House;
import com.renthouse.dto.CreateHouseRequest;
import com.renthouse.dto.HouseDTO;
import com.renthouse.enums.AccountType;
import com.renthouse.enums.ContractStatus;
import com.renthouse.enums.HouseStatus;
import com.renthouse.enums.MessageType;
import com.renthouse.repository.AccountRepository;
import com.renthouse.repository.ContractRepository;
import com.renthouse.repository.HouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NewHouseService {

    private static final int MAX_IMAGES = 4;
    private static final Pattern HOUSE_ID_KEYWORD_PATTERN =
            Pattern.compile("^(?:#|id\\s*:)\\s*(\\d+)$", Pattern.CASE_INSENSITIVE);

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private OperatorAccountService operatorAccountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private MessageService messageService;

    public List<HouseDTO> getAvailableHouses() {
        return houseRepository.findByStatus(HouseStatus.AVAILABLE)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<HouseDTO> searchHouses(String district,
                                       BigDecimal minPrice,
                                       BigDecimal maxPrice,
                                       String keyword,
                                       String houseType,
                                       BigDecimal minArea,
                                       BigDecimal maxArea,
                                       String sortBy) {
        KeywordCriteria keywordCriteria = parseKeywordCriteria(keyword);

        if (keywordCriteria.exactHouseId() != null) {
            return houseRepository.findById(keywordCriteria.exactHouseId())
                    .stream()
                    .filter(house -> house.getStatus() == HouseStatus.AVAILABLE)
                    .map(this::convertToDTO)
                    .toList();
        }

        List<House> houses = houseRepository.searchHouses(
                HouseStatus.AVAILABLE,
                normalize(district),
                minPrice,
                maxPrice,
                normalize(houseType),
                minArea,
                maxArea,
                keywordCriteria.textKeyword()
        );

        return sortHouses(houses, sortBy).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public HouseDTO getHouseById(Long id) {
        House house = houseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("房源不存在"));
        house.setViewCount((house.getViewCount() == null ? 0 : house.getViewCount()) + 1);
        houseRepository.save(house);
        return convertToDTO(house);
    }

    public List<HouseDTO> getMyHouses(Long userId) {
        return houseRepository.findByOwnerId(userId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<HouseDTO> getAllHouses() {
        return houseRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<HouseDTO> getStaffPendingHouses(Long operatorId) {
        Account operator = accountRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("业务员不存在"));
        if (operator.getAccountType() != AccountType.STAFF && operator.getAccountType() != AccountType.ADMIN) {
            throw new RuntimeException("权限不足");
        }
        return houseRepository.findAll().stream()
                .filter(h -> h.getStatus() == HouseStatus.PENDING_STAFF_REVIEW)
                .filter(h -> operator.getAccountType() == AccountType.ADMIN || operatorId.equals(h.getAssignedStaffId()))
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public HouseDTO createHouse(CreateHouseRequest request, Long userId) {
        Account owner = requireUserAccount(userId);
        if (Boolean.FALSE.equals(owner.getCanPublish())) {
            throw new RuntimeException("当前账号已被限制发布房源");
        }

        Account assignedStaff = operatorAccountService.pickRandomEnabledStaff();

        House house = new House();
        house.setOwner(owner);
        applyHousePayload(house, request);
        house.setImages(null);
        house.setStatus(HouseStatus.PENDING_STAFF_REVIEW);
        house.setAssignedStaffId(assignedStaff.getId());
        house.setViewCount(0);

        House saved = houseRepository.save(house);
        saved.setImages(persistHouseImages(saved.getId(), request.getImages()));
        saved = houseRepository.save(saved);

        messageService.notifyStaff(
                assignedStaff.getId(),
                "房源待审核",
                String.format("房东 %s 发布了新房源《%s》，请审核", displayName(owner), saved.getTitle()),
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
        house.setReviewComment(reason == null || reason.isBlank() ? "审核未通过" : reason.trim());
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
            throw new RuntimeException("该房源有关联合同处理中，暂不支持编辑");
        }
        if (house.getStatus() == HouseStatus.RENTED) {
            throw new RuntimeException("房源已出租，暂不支持编辑");
        }

        applyHousePayload(house, request);
        house.setImages(persistHouseImages(house.getId(), request.getImages()));
        house.setStatus(HouseStatus.PENDING_STAFF_REVIEW);

        Account assignedStaff = operatorAccountService.pickRandomEnabledStaff();
        house.setAssignedStaffId(assignedStaff.getId());

        House updated = houseRepository.save(house);
        messageService.notifyStaff(
                assignedStaff.getId(),
                "房源待复审",
                String.format("房源《%s》已更新，请复审", updated.getTitle()),
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
            throw new RuntimeException("该房源有关联合同处理中，暂不可下架");
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
            throw new RuntimeException("该房源有关联合同处理中，暂不可上架");
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
            throw new RuntimeException("该房源有关联合同处理中，暂不可删除");
        }
        if (house.getStatus() == HouseStatus.RENTED) {
            throw new RuntimeException("房源已出租，无法删除");
        }
        houseRepository.delete(house);
    }

    private void applyHousePayload(House house, CreateHouseRequest request) {
        house.setTitle(request.getTitle());
        house.setAddress(request.getAddress());
        house.setDistrict(request.getDistrict());
        house.setHouseType(request.getHouseType());
        house.setArea(request.getArea());
        house.setFloor(request.getFloor());
        house.setRentPrice(request.getRentPrice());
        house.setDeposit(request.getDeposit());
        house.setDescription(request.getDescription());
        house.setFacilities(request.getFacilities());
    }

    private Account requireUserAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (account.getAccountType() != AccountType.USER) {
            throw new RuntimeException("仅普通用户可操作");
        }
        return account;
    }

    private void checkStaffAssignment(House house, Long operatorId) {
        Account operator = accountRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("业务员不存在"));
        if (operator.getAccountType() == AccountType.ADMIN) {
            return;
        }
        if (operator.getAccountType() != AccountType.STAFF) {
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
            Account operator = accountRepository.findById(operatorId)
                    .orElseThrow(() -> new RuntimeException("操作员不存在"));
            if (operator.getAccountType() == AccountType.ADMIN) {
                return;
            }
        }
        throw new RuntimeException("权限不足：仅可操作自己的房源");
    }

    private HouseDTO convertToDTO(House house) {
        HouseDTO dto = new HouseDTO();
        dto.setId(house.getId());
        dto.setOwnerId(house.getOwner().getId());
        dto.setOwnerName(displayName(house.getOwner()));
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
                    dto.setCurrentTenantName(displayName(contract.getTenant()));
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

    private KeywordCriteria parseKeywordCriteria(String keyword) {
        String normalized = normalize(keyword);
        if (normalized == null) {
            return new KeywordCriteria(null, null);
        }

        Matcher matcher = HOUSE_ID_KEYWORD_PATTERN.matcher(normalized);
        if (matcher.matches()) {
            try {
                return new KeywordCriteria(Long.parseLong(matcher.group(1)), null);
            } catch (NumberFormatException ignored) {
                return new KeywordCriteria(null, normalized);
            }
        }
        return new KeywordCriteria(null, normalized);
    }

    private List<House> sortHouses(List<House> houses, String sortBy) {
        String normalizedSort = normalize(sortBy);
        Comparator<House> comparator;

        if ("price_asc".equalsIgnoreCase(normalizedSort)) {
            comparator = Comparator.comparing(House::getRentPrice, Comparator.nullsLast(BigDecimal::compareTo));
        } else if ("price_desc".equalsIgnoreCase(normalizedSort)) {
            comparator = Comparator.comparing(House::getRentPrice, Comparator.nullsLast(BigDecimal::compareTo)).reversed();
        } else if ("area_asc".equalsIgnoreCase(normalizedSort)) {
            comparator = Comparator.comparing(House::getArea, Comparator.nullsLast(BigDecimal::compareTo));
        } else if ("area_desc".equalsIgnoreCase(normalizedSort)) {
            comparator = Comparator.comparing(House::getArea, Comparator.nullsLast(BigDecimal::compareTo)).reversed();
        } else {
            comparator = Comparator.comparing(House::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed();
        }

        return houses.stream().sorted(comparator).toList();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String displayName(Account account) {
        if (account == null) {
            return "未知";
        }
        if (account.getRealName() != null && !account.getRealName().isBlank()) {
            return account.getRealName();
        }
        if (account.getDisplayName() != null && !account.getDisplayName().isBlank()) {
            return account.getDisplayName();
        }
        return account.getUsername();
    }

    private String persistHouseImages(Long houseId, String rawImages) {
        if (rawImages == null || rawImages.isBlank()) {
            return null;
        }

        List<String> values = parseImageValues(rawImages);
        if (values.isEmpty()) {
            return null;
        }
        if (values.size() > MAX_IMAGES) {
            throw new RuntimeException("房源图片最多4张");
        }

        List<String> urls = new ArrayList<>();
        for (String value : values) {
            if (value.startsWith("/uploads/houses/" + houseId + "/")) {
                urls.add(value);
                continue;
            }
            if (!value.startsWith("data:image/")) {
                throw new RuntimeException("图片格式非法");
            }
            urls.add(saveBase64ImageToHouseDir(houseId, value));
        }
        return String.join("|", urls);
    }

    private List<String> parseImageValues(String rawImages) {
        List<String> values = new ArrayList<>();
        for (String part : rawImages.split("\\|")) {
            String value = part == null ? "" : part.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private String saveBase64ImageToHouseDir(Long houseId, String dataUri) {
        int comma = dataUri.indexOf(',');
        if (comma <= 0) {
            throw new RuntimeException("图片数据损坏");
        }
        String meta = dataUri.substring(0, comma);
        String base64 = dataUri.substring(comma + 1);

        String ext = ".jpg";
        if (meta.contains("image/png")) {
            ext = ".png";
        } else if (meta.contains("image/webp")) {
            ext = ".webp";
        } else if (meta.contains("image/jpeg") || meta.contains("image/jpg")) {
            ext = ".jpg";
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("图片Base64解析失败");
        }
        if (bytes.length > 2L * 1024 * 1024) {
            throw new RuntimeException("图片不能超过2MB");
        }

        String fileName = UUID.randomUUID() + ext;
        Path dir = Paths.get("uploads", "houses", String.valueOf(houseId), "gallery");
        Path target = dir.resolve(fileName);
        try {
            Files.createDirectories(dir);
            Files.write(target, bytes);
        } catch (Exception e) {
            throw new RuntimeException("图片保存失败: " + e.getMessage());
        }
        return "/uploads/houses/" + houseId + "/gallery/" + fileName;
    }

    private record KeywordCriteria(Long exactHouseId, String textKeyword) {
    }
}
