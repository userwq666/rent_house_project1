package com.renthouse.repository;

import com.renthouse.domain.Message;
import com.renthouse.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByReceiverPrincipalTypeAndReceiverPrincipalIdOrderByCreatedAtDesc(String receiverPrincipalType, Long receiverPrincipalId);

    @Query("SELECT m FROM Message m WHERE (m.senderPrincipalType = :principalType AND m.senderPrincipalId = :principalId) " +
            "OR (m.receiverPrincipalType = :principalType AND m.receiverPrincipalId = :principalId) " +
            "ORDER BY m.createdAt DESC")
    List<Message> findByPrincipalOrderByCreatedAtDesc(@Param("principalType") String principalType, @Param("principalId") Long principalId);

    long countByReceiverPrincipalTypeAndReceiverPrincipalIdAndStatus(String receiverPrincipalType, Long receiverPrincipalId, MessageStatus status);

    List<Message> findByReceiverPrincipalTypeAndReceiverPrincipalIdAndStatus(String receiverPrincipalType, Long receiverPrincipalId, MessageStatus status);
}
