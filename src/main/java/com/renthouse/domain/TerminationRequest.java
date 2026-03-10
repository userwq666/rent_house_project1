package com.renthouse.domain;

import com.renthouse.enums.TerminationStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 合同终止申请
 */
@Entity
@Table(name = "termination_requests")
@Data
public class TerminationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responder_id")
    private User responder;

    @Column(name = "review_staff_id")
    private Long reviewStaffId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TerminationStatus status = TerminationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "force_reason", columnDefinition = "TEXT")
    private String forceReason;

    @Column(name = "admin_notified")
    private Boolean adminNotified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
