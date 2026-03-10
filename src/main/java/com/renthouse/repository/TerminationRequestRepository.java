package com.renthouse.repository;

import com.renthouse.domain.TerminationRequest;
import com.renthouse.enums.TerminationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TerminationRequestRepository extends JpaRepository<TerminationRequest, Long> {
    Optional<TerminationRequest> findByContractIdAndStatus(Long contractId, TerminationStatus status);
    boolean existsByContractIdAndStatus(Long contractId, TerminationStatus status);
    Optional<TerminationRequest> findTopByContractIdOrderByCreatedAtDesc(Long contractId);
    List<TerminationRequest> findByResponderIdAndStatus(Long responderId, TerminationStatus status);
    List<TerminationRequest> findByReviewStaffIdAndStatus(Long reviewStaffId, TerminationStatus status);
}
