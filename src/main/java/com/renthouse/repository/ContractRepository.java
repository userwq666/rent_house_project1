package com.renthouse.repository;

import com.renthouse.domain.Contract;
import com.renthouse.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 合同Repository
 */
@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    
    // 根据房屋ID查询
    List<Contract> findByHouseId(Long houseId);
    
    // 根据房主ID查询
    List<Contract> findByLandlordId(Long landlordId);
    
    // 根据租客ID查询
    List<Contract> findByTenantId(Long tenantId);
    
    // 根据状态查询
    List<Contract> findByStatus(ContractStatus status);
    
    // 查询某个房屋的当前有效合同
    @Query("SELECT c FROM Contract c WHERE c.house.id = :houseId AND c.status = 'ACTIVE'")
    Optional<Contract> findActiveContractByHouseId(@Param("houseId") Long houseId);

    // 是否存在未完结的合同（包含待审批/进行中/终止待确认）
    boolean existsByHouseIdAndStatusIn(Long houseId, List<ContractStatus> statuses);
    
    // 查询即将到期的合同（30天内）
    @Query("SELECT c FROM Contract c WHERE c.status = 'ACTIVE' AND c.endDate BETWEEN :startDate AND :endDate")
    List<Contract> findExpiringContracts(@Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate);
    
    // 查询用户相关的所有合同（作为房主或租客）
    @Query("SELECT c FROM Contract c WHERE c.landlord.id = :userId OR c.tenant.id = :userId")
    List<Contract> findByUserId(@Param("userId") Long userId);
}
