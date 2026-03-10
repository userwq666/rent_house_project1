package com.renthouse.service;

import com.renthouse.domain.Message;
import com.renthouse.domain.OperatorAccount;
import com.renthouse.domain.User;
import com.renthouse.dto.MessageDTO;
import com.renthouse.enums.MessageStatus;
import com.renthouse.enums.MessageType;
import com.renthouse.enums.OperatorRole;
import com.renthouse.repository.MessageRepository;
import com.renthouse.repository.OperatorAccountRepository;
import com.renthouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OperatorAccountRepository operatorAccountRepository;

    private static final Long SYSTEM_ID = -1L;
    private static final String SYSTEM_NAME = "系统消息";

    public Message sendMessage(Long senderId, Long receiverId, String title, String content,
                               MessageType type, Long contractId, Long requestId, boolean requireAction) {
        User sender = senderId != null && !senderId.equals(SYSTEM_ID)
                ? userRepository.findById(senderId).orElse(null)
                : null;
        if (receiverId != null && receiverId.equals(SYSTEM_ID)) {
            throw new RuntimeException("系统消息只允许系统向用户发送");
        }
        User receiver = receiverId != null ? userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("接收方不存在")) : null;

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setRelatedContractId(contractId);
        message.setRelatedRequestId(requestId);
        message.setRequireAction(requireAction);
        if (Boolean.TRUE.equals(requireAction)) {
            message.setStatus(MessageStatus.PENDING);
        }
        return messageRepository.save(message);
    }

    public Message sendOperatorMessage(Long senderOperatorId, Long receiverOperatorId, String title, String content,
                                       MessageType type, Long contractId, Long requestId, boolean requireAction) {
        OperatorAccount sender = senderOperatorId != null
                ? operatorAccountRepository.findById(senderOperatorId)
                .orElse(null)
                : null;
        OperatorAccount receiver = receiverOperatorId != null
                ? operatorAccountRepository.findById(receiverOperatorId)
                .orElseThrow(() -> new RuntimeException("接收业务账号不存在"))
                : null;

        Message message = new Message();
        message.setSenderOperatorId(sender != null ? sender.getId() : null);
        message.setSenderOperatorName(sender != null ? sender.getDisplayName() : SYSTEM_NAME);
        message.setReceiverOperatorId(receiver != null ? receiver.getId() : null);
        message.setReceiverOperatorName(receiver != null ? receiver.getDisplayName() : null);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setRelatedContractId(contractId);
        message.setRelatedRequestId(requestId);
        message.setRequireAction(requireAction);
        if (Boolean.TRUE.equals(requireAction)) {
            message.setStatus(MessageStatus.PENDING);
        }
        return messageRepository.save(message);
    }

    public List<MessageDTO> getMessages(Long userId) {
        return messageRepository.findByReceiverIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    public List<MessageDTO> getOperatorMessages(Long operatorId) {
        return messageRepository.findByReceiverOperatorIdOrderByCreatedAtDesc(operatorId)
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    public List<MessageDTO> getMessageContacts(Long userId) {
        List<Message> messages = messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId);
        Map<Long, List<Message>> groupedMessages = messages.stream()
                .filter(msg -> !isArchivedForUser(msg, userId))
                .collect(Collectors.groupingBy(msg -> {
                    if (msg.getSender() != null && msg.getSender().getId().equals(userId)) {
                        return msg.getReceiver() != null ? msg.getReceiver().getId() : SYSTEM_ID;
                    }
                    return msg.getSender() != null ? msg.getSender().getId() : SYSTEM_ID;
                }));

        return groupedMessages.entrySet().stream()
                .map(entry -> {
                    Message latestMessage = entry.getValue().stream()
                            .max(Comparator.comparing(Message::getCreatedAt))
                            .orElse(null);
                    if (latestMessage == null) {
                        return null;
                    }
                    long unreadCount = entry.getValue().stream()
                            .filter(msg -> msg.getReceiver() != null && msg.getReceiver().getId().equals(userId))
                            .filter(msg -> msg.getStatus() == MessageStatus.UNREAD)
                            .count();
                    MessageDTO dto = convert(latestMessage);
                    dto.setUnreadCount(unreadCount);
                    return dto;
                })
                .filter(dto -> dto != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<MessageDTO> getChatMessages(Long userId, Long contactId) {
        List<Message> messages = messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId);

        List<Message> chatMessages;
        if (contactId != null && contactId.equals(SYSTEM_ID)) {
            chatMessages = messages.stream()
                    .filter(msg -> msg.getSender() == null
                            && msg.getReceiver() != null
                            && msg.getReceiver().getId().equals(userId))
                    .filter(msg -> !isArchivedForUser(msg, userId))
                    .sorted(Comparator.comparing(Message::getCreatedAt))
                    .collect(Collectors.toList());
        } else {
            chatMessages = messages.stream()
                    .filter(msg ->
                            (msg.getSender() != null && msg.getSender().getId().equals(contactId) &&
                                    msg.getReceiver() != null && msg.getReceiver().getId().equals(userId)) ||
                                    (msg.getSender() != null && msg.getSender().getId().equals(userId) &&
                                            msg.getReceiver() != null && msg.getReceiver().getId().equals(contactId))
                    )
                    .filter(msg -> !isArchivedForUser(msg, userId))
                    .sorted(Comparator.comparing(Message::getCreatedAt))
                    .collect(Collectors.toList());
        }

        List<Message> unreadMessages = chatMessages.stream()
                .filter(msg -> msg.getReceiver() != null && msg.getReceiver().getId().equals(userId)
                        && msg.getStatus() == MessageStatus.UNREAD)
                .toList();

        for (Message message : unreadMessages) {
            message.setStatus(MessageStatus.READ);
            message.setReadAt(LocalDateTime.now());
        }
        if (!unreadMessages.isEmpty()) {
            messageRepository.saveAll(unreadMessages);
        }

        return chatMessages.stream().map(this::convert).collect(Collectors.toList());
    }

    public long getUnreadMessageCount(Long userId) {
        return messageRepository.countByReceiverIdAndStatus(userId, MessageStatus.UNREAD);
    }

    public long getOperatorUnreadMessageCount(Long operatorId) {
        return messageRepository.countByReceiverOperatorIdAndStatus(operatorId, MessageStatus.UNREAD);
    }

    public List<MessageDTO> getAdminMessages(Long operatorId) {
        OperatorAccount account = operatorAccountRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        if (account.getRole() != OperatorRole.ADMIN) {
            throw new RuntimeException("权限不足");
        }
        return getOperatorMessages(operatorId);
    }

    public void markAsRead(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        boolean userMatched = message.getReceiver() != null && message.getReceiver().getId().equals(userId);
        boolean operatorMatched = message.getReceiverOperatorId() != null && message.getReceiverOperatorId().equals(userId);
        if (!userMatched && !operatorMatched) {
            throw new RuntimeException("无权操作该消息");
        }
        message.setStatus(MessageStatus.READ);
        message.setReadAt(LocalDateTime.now());
        messageRepository.save(message);
    }

    public void updateMessageStatus(Long messageId, Long userId, MessageStatus status) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        boolean userMatched = message.getReceiver() != null && message.getReceiver().getId().equals(userId);
        boolean operatorMatched = message.getReceiverOperatorId() != null && message.getReceiverOperatorId().equals(userId);
        if (!userMatched && !operatorMatched) {
            throw new RuntimeException("无权操作该消息");
        }
        message.setStatus(status);
        messageRepository.save(message);
    }

    public void markAllAsRead(Long userId) {
        List<Message> unreadMessages = messageRepository.findByReceiverIdAndStatus(userId, MessageStatus.UNREAD);
        for (Message message : unreadMessages) {
            message.setStatus(MessageStatus.READ);
            message.setReadAt(LocalDateTime.now());
        }
        messageRepository.saveAll(unreadMessages);
    }

    public void archiveContactMessages(Long userId, Long contactId) {
        List<Message> messages = messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId);
        List<Message> contactMessages = messages.stream()
                .filter(msg -> {
                    Long senderId = msg.getSender() != null ? msg.getSender().getId() : null;
                    Long receiverId = msg.getReceiver() != null ? msg.getReceiver().getId() : null;
                    if (contactId.equals(SYSTEM_ID)) {
                        return false;
                    }
                    return (senderId != null && senderId.equals(contactId) && receiverId != null && receiverId.equals(userId)) ||
                            (senderId != null && senderId.equals(userId) && receiverId != null && receiverId.equals(contactId));
                })
                .collect(Collectors.toList());

        for (Message message : contactMessages) {
            addArchivedUserId(message, userId);
        }

        if (!contactMessages.isEmpty()) {
            messageRepository.saveAll(contactMessages);
        }
    }

    private void addArchivedUserId(Message message, Long userId) {
        String archivedIds = message.getArchivedByUserIds();
        if (archivedIds == null || archivedIds.isEmpty()) {
            message.setArchivedByUserIds(userId.toString());
        } else if (!archivedIds.contains(userId.toString())) {
            message.setArchivedByUserIds(archivedIds + "," + userId);
        }
    }

    private boolean isArchivedForUser(Message message, Long userId) {
        String archivedIds = message.getArchivedByUserIds();
        return archivedIds != null && archivedIds.contains(userId.toString());
    }

    private MessageDTO convert(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setTitle(message.getTitle());
        dto.setContent(message.getContent());
        dto.setType(message.getType());
        dto.setStatus(message.getStatus());
        if (message.getSender() != null) {
            dto.setSenderId(message.getSender().getId());
            dto.setSenderName(message.getSender().getRealName());
        } else {
            dto.setSenderId(SYSTEM_ID);
            dto.setSenderName(SYSTEM_NAME);
        }
        if (message.getReceiver() != null) {
            dto.setReceiverId(message.getReceiver().getId());
            dto.setReceiverName(message.getReceiver().getRealName());
        }
        dto.setSenderOperatorId(message.getSenderOperatorId());
        dto.setSenderOperatorName(message.getSenderOperatorName());
        dto.setReceiverOperatorId(message.getReceiverOperatorId());
        dto.setReceiverOperatorName(message.getReceiverOperatorName());
        dto.setRelatedContractId(message.getRelatedContractId());
        dto.setRelatedRequestId(message.getRelatedRequestId());
        dto.setRequireAction(message.getRequireAction());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setReadAt(message.getReadAt());
        return dto;
    }

    public void notifyAdmins(String title, String content, Long contractId, Long requestId, boolean requireAction) {
        operatorAccountRepository.findByRoleAndEnabled(OperatorRole.ADMIN, true).forEach(admin ->
                sendOperatorMessage(null, admin.getId(), title, content,
                        MessageType.ADMIN_NOTIFICATION, contractId, requestId, requireAction)
        );
    }

    public void notifyStaff(Long staffOperatorId, String title, String content, Long contractId, Long requestId, boolean requireAction, MessageType type) {
        sendOperatorMessage(null, staffOperatorId, title, content, type, contractId, requestId, requireAction);
    }
}
