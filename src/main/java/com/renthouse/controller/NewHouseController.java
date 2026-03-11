package com.renthouse.controller;

import com.renthouse.dto.CreateHouseRequest;
import com.renthouse.dto.HouseDTO;
import com.renthouse.service.NewHouseService;
import com.renthouse.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/houses")
@CrossOrigin(origins = "*")
public class NewHouseController {

    @Autowired
    private NewHouseService houseService;

    @GetMapping("/available")
    public ResponseEntity<List<HouseDTO>> getAvailableHouses() {
        return ResponseEntity.ok(houseService.getAvailableHouses());
    }

    @GetMapping("/search")
    public ResponseEntity<List<HouseDTO>> searchHouses(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String houseType,
            @RequestParam(required = false) BigDecimal minArea,
            @RequestParam(required = false) BigDecimal maxArea,
            @RequestParam(required = false) String sortBy) {
        return ResponseEntity.ok(houseService.searchHouses(
                district, minPrice, maxPrice, keyword, houseType, minArea, maxArea, sortBy
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HouseDTO> getHouseById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(houseService.getHouseById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllHouses() {
        try {
            AuthUtil.getCurrentOperatorId();
            return ResponseEntity.ok(houseService.getAllHouses());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyHouses() {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            return ResponseEntity.ok(houseService.getMyHouses(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/staff/pending")
    public ResponseEntity<?> getStaffPendingHouses() {
        try {
            Long operatorId = AuthUtil.getCurrentOperatorId();
            return ResponseEntity.ok(houseService.getStaffPendingHouses(operatorId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/staff/approve")
    public ResponseEntity<?> approveByStaff(@PathVariable Long id) {
        try {
            Long operatorId = AuthUtil.getCurrentOperatorId();
            return ResponseEntity.ok(houseService.approveHouseByStaff(id, operatorId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("审核失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/staff/reject")
    public ResponseEntity<?> rejectByStaff(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            Long operatorId = AuthUtil.getCurrentOperatorId();
            String reason = body == null ? null : body.get("reason");
            return ResponseEntity.ok(houseService.rejectHouseByStaff(id, operatorId, reason));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("审核失败: " + e.getMessage());
        }
    }

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

    @PutMapping("/{id}")
    public ResponseEntity<?> updateHouse(@PathVariable Long id, @RequestBody CreateHouseRequest request) {
        try {
            Long userId = null;
            Long operatorId = null;
            try {
                userId = AuthUtil.getCurrentUserId();
            } catch (Exception ignored) {
                operatorId = AuthUtil.getCurrentOperatorId();
            }
            HouseDTO house = houseService.updateHouse(id, request, userId, operatorId);
            return ResponseEntity.ok(house);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/offline")
    public ResponseEntity<?> offlineHouse(@PathVariable Long id) {
        try {
            Long userId = null;
            Long operatorId = null;
            try {
                userId = AuthUtil.getCurrentUserId();
            } catch (Exception ignored) {
                operatorId = AuthUtil.getCurrentOperatorId();
            }
            houseService.offlineHouse(id, userId, operatorId);
            return ResponseEntity.ok("下架成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("下架失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/online")
    public ResponseEntity<?> onlineHouse(@PathVariable Long id) {
        try {
            Long userId = null;
            Long operatorId = null;
            try {
                userId = AuthUtil.getCurrentUserId();
            } catch (Exception ignored) {
                operatorId = AuthUtil.getCurrentOperatorId();
            }
            houseService.onlineHouse(id, userId, operatorId);
            return ResponseEntity.ok("上架成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("上架失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHouse(@PathVariable Long id) {
        try {
            Long userId = null;
            Long operatorId = null;
            try {
                userId = AuthUtil.getCurrentUserId();
            } catch (Exception ignored) {
                operatorId = AuthUtil.getCurrentOperatorId();
            }
            houseService.deleteHouse(id, userId, operatorId);
            return ResponseEntity.ok("删除成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除失败: " + e.getMessage());
        }
    }
}
