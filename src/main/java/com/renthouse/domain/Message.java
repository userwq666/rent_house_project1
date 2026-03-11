package com.renthouse.domain;

import com.renthouse.enums.MessageStatus;
import com.renthouse.enums.MessageType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 系统消息/对话记录
 */
@Entity
@Table(name = "messages")
@Data
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Column(name = "sender_operator_id")
    private Long senderOperatorId;

    @Column(name = "receiver_operator_id")
    private Long receiverOperatorId;

    @Column(name = "sender_operator_name", length = 80)
    private String senderOperatorName;

    @Column(name = "receiver_operator_name", length = 80)
    private String receiverOperatorName;

    @Column(name = "sender_principal_type", length = 20)
    private String senderPrincipalType;

    @Column(name = "sender_principal_id")
    private Long senderPrincipalId;

    @Column(name = "receiver_principal_type", length = 20)
    private String receiverPrincipalType;

    @Column(name = "receiver_principal_id")
    private Long receiverPrincipalId;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 50)
    private MessageType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private MessageStatus status = MessageStatus.UNREAD;

    @Column(name = "related_contract_id")
    private Long relatedContractId;

    @Column(name = "related_house_id")
    private Long relatedHouseId;

    @Column(name = "related_request_id")
    private Long relatedRequestId;

    @Column(name = "require_action")
    private Boolean requireAction = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 已归档此消息的用户 ID 列表（逗号分隔）
     * 用于实现单方面隐藏聊天记录
     */
    @Column(name = "archived_by_user_ids", length = 500)
    private String archivedByUserIds;
}
