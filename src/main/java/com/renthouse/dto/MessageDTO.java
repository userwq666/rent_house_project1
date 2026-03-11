package com.renthouse.dto;

import com.renthouse.enums.MessageStatus;
import com.renthouse.enums.MessageType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDTO {
    private Long id;
    private String title;
    private String content;
    private MessageType type;
    private MessageStatus status;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private Long senderOperatorId;
    private String senderOperatorName;
    private Long receiverOperatorId;
    private String receiverOperatorName;
    private String senderPrincipalType;
    private Long senderPrincipalId;
    private String receiverPrincipalType;
    private Long receiverPrincipalId;
    private Long relatedContractId;
    private Long relatedHouseId;
    private Long relatedRequestId;
    private Boolean requireAction;
    private String contactType;
    private String contactKey;
    private Long contactId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private long unreadCount; // 未读消息数量
}
