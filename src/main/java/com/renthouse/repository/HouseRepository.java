package com.renthouse.repository;

import com.renthouse.domain.House;
import com.renthouse.enums.HouseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 房屋Repository
 */
@Repository
public interface HouseRepository extends JpaRepository<House, Long> {
    
    // 根据状态查询
    List<House> findByStatus(HouseStatus status);
    
    // 根据房主ID查询
    List<House> findByOwnerId(Long ownerId);
    
    // 根据区域查询
    List<House> findByDistrictContaining(String district);
    
    // 根据价格区间查询
    List<House> findByRentPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    // 根据户型查询
    List<House> findByHouseType(String houseType);
    
    // 综合查询：状态+区域+价格区间
    @Query("SELECT h FROM House h WHERE h.status = :status " +
           "AND (:district IS NULL OR h.district LIKE %:district%) " +
           "AND (:minPrice IS NULL OR h.rentPrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR h.rentPrice <= :maxPrice)")
    List<House> searchHouses(@Param("status") HouseStatus status,
                            @Param("district") String district,
                            @Param("minPrice") BigDecimal minPrice,
                            @Param("maxPrice") BigDecimal maxPrice);
}
