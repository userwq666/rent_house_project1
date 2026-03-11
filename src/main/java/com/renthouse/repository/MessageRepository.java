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

    @Query("SELECT m FROM Message m WHERE (m.sender.id = :accountId OR m.receiver.id = :accountId) ORDER BY m.createdAt DESC")
    List<Message> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(@Param("accountId") Long accountId);

    long countByReceiverIdAndStatus(Long receiverId, MessageStatus status);

    List<Message> findByReceiverIdAndStatus(Long receiverId, MessageStatus status);
}
