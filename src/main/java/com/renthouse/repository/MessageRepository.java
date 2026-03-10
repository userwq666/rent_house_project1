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
    List<Message> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    @Query("SELECT m FROM Message m WHERE (m.sender.id = :userId OR m.receiver.id = :userId) ORDER BY m.createdAt DESC")
    List<Message> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE (m.senderOperatorId = :operatorId OR m.receiverOperatorId = :operatorId) ORDER BY m.createdAt DESC")
    List<Message> findBySenderOperatorIdOrReceiverOperatorIdOrderByCreatedAtDesc(@Param("operatorId") Long operatorId);

    long countByReceiverIdAndStatus(Long receiverId, MessageStatus status);

    List<Message> findByReceiverIdAndStatus(Long receiverId, MessageStatus status);

    List<Message> findByReceiverOperatorIdOrderByCreatedAtDesc(Long receiverOperatorId);

    long countByReceiverOperatorIdAndStatus(Long receiverOperatorId, MessageStatus status);

    List<Message> findByReceiverOperatorIdAndStatus(Long receiverOperatorId, MessageStatus status);
}
