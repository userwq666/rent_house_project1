package com.renthouse.controller;

import com.renthouse.dto.CreateHouseRequest;
import com.renthouse.dto.HouseDTO;
import com.renthouse.enums.AccountType;
import com.renthouse.repository.AccountRepository;
import com.renthouse.service.NewHouseService;
import com.renthouse.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 房源 Controller（新版）
 */
@RestController
@RequestMapping("/api/houses")
@CrossOrigin(origins = "*")
public class NewHouseController {

    @Autowired
    private NewHouseService houseService;

    @Autowired
    private AccountRepository accountRepository;

    /**
     * 获取所有可租房源（公开接口）
     */
    @GetMapping("/available")
    public ResponseEntity<List<HouseDTO>> getAvailableHouses() {
        return ResponseEntity.ok(houseService.getAvailableHouses());
    }

    /**
     * 搜索房源（公开接口）
     */
    @GetMapping("/search")
    public ResponseEntity<List<HouseDTO>> searchHouses(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return ResponseEntity.ok(houseService.searchHouses(district, minPrice, maxPrice));
    }

    /**
     * 根据 ID 获取房源详情（公开接口，浏览次数+1）
     */
    @GetMapping("/{id}")
    public ResponseEntity<HouseDTO> getHouseById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(houseService.getHouseById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 管理员获取全部房源
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllHouses() {
        try {
            Long accountId = AuthUtil.getCurrentAccountId();
            AccountType type = accountRepository.findById(accountId)
                    .map(acc -> acc.getAccountType())
                    .orElse(AccountType.USER);
            if (type != AccountType.ADMIN) {
                throw new RuntimeException("权限不足");
            }
            return ResponseEntity.ok(houseService.getAllHouses());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取我发布的房源（需登录）
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyHouses() {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            return ResponseEntity.ok(houseService.getMyHouses(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    /**
     * 发布房源（需登录）
     */
    @PostMapping
    public ResponseEntity<?> createHouse(@RequestBody CreateHouseRequest request) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            HouseDTO house = houseService.createHouse(request, userId);
            return ResponseEntity.ok(house);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("发布失败: " + e.getMessage());
        }
    }

    /**
     * 更新房源（需登录，权限：房主或管理员）
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateHouse(
            @PathVariable Long id,
            @RequestBody CreateHouseRequest request) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            Long accountId = AuthUtil.getCurrentAccountId();
            HouseDTO house = houseService.updateHouse(id, request, userId, accountId);
            return ResponseEntity.ok(house);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新失败: " + e.getMessage());
        }
    }

    /**
     * 下架房源（需登录，权限：房主或管理员）
     */
    @PutMapping("/{id}/offline")
    public ResponseEntity<?> offlineHouse(@PathVariable Long id) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            Long accountId = AuthUtil.getCurrentAccountId();
            houseService.offlineHouse(id, userId, accountId);
            return ResponseEntity.ok("下架成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("下架失败: " + e.getMessage());
        }
    }

    /**
     * 上架房源（需登录，权限：房主或管理员）
     */
    @PutMapping("/{id}/online")
    public ResponseEntity<?> onlineHouse(@PathVariable Long id) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            Long accountId = AuthUtil.getCurrentAccountId();
            houseService.onlineHouse(id, userId, accountId);
            return ResponseEntity.ok("上架成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("上架失败: " + e.getMessage());
        }
    }

    /**
     * 删除房源（需登录，权限：房主或管理员）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHouse(@PathVariable Long id) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            Long accountId = AuthUtil.getCurrentAccountId();
            houseService.deleteHouse(id, userId, accountId);
            return ResponseEntity.ok("删除成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除失败: " + e.getMessage());
        }
    }
}
