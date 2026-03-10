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

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByHouseId(Long houseId);

    List<Contract> findByLandlordId(Long landlordId);

    List<Contract> findByTenantId(Long tenantId);

    List<Contract> findByStatus(ContractStatus status);

    List<Contract> findByAssignedStaffId(Long assignedStaffId);

    @Query("SELECT c FROM Contract c WHERE c.house.id = :houseId AND c.status = 'ACTIVE'")
    Optional<Contract> findActiveContractByHouseId(@Param("houseId") Long houseId);

    boolean existsByHouseIdAndStatusIn(Long houseId, List<ContractStatus> statuses);

    @Query("SELECT c FROM Contract c WHERE c.status = 'ACTIVE' AND c.endDate BETWEEN :startDate AND :endDate")
    List<Contract> findExpiringContracts(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT c FROM Contract c WHERE c.landlord.id = :userId OR c.tenant.id = :userId")
    List<Contract> findByUserId(@Param("userId") Long userId);
}
