package com.renthouse.domain;

import com.renthouse.enums.ContractStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 租房合同实体
 */
@Entity
@Table(name = "contracts")
@Data
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id", nullable = false)
    private House house;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @Column(name = "rent_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal rentPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deposit;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ContractStatus status = ContractStatus.ACTIVE;

    @Column(name = "signed_date")
    private LocalDateTime signedDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    @Column(name = "signed_contract_url", length = 500)
    private String signedContractUrl;

    @Column(name = "signed_contract_name", length = 255)
    private String signedContractName;

    @Column(name = "signed_contract_uploaded_at")
    private LocalDateTime signedContractUploadedAt;

    @Column(name = "signed_contract_uploaded_by")
    private Long signedContractUploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
