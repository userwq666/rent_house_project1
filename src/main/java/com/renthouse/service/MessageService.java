package com.renthouse.service;

import com.renthouse.domain.Message;
import com.renthouse.domain.Account;
import com.renthouse.dto.MessageDTO;
import com.renthouse.enums.AccountType;
import com.renthouse.enums.MessageStatus;
import com.renthouse.enums.MessageType;
import com.renthouse.repository.MessageRepository;
import com.renthouse.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AccountRepository accountRepository;

    private static final Long SYSTEM_ID = -1L;
    private static final String SYSTEM_NAME = "系统消息";
    private static final String CONTACT_USER = "USER";
    private static final String CONTACT_OPERATOR = "OPERATOR";
    private static final String CONTACT_SYSTEM = "SYSTEM";

    public Message sendMessage(Long senderId, Long receiverId, String title, String content,
                               MessageType type, Long contractId, Long requestId, boolean requireAction) {
        return sendMessage(senderId, receiverId, title, content, type, contractId, null, requestId, requireAction);
    }

    public Message sendMessage(Long senderId, Long receiverId, String title, String content,
                               MessageType type, Long contractId, Long houseId, Long requestId, boolean requireAction) {
        Account sender = senderId != null && !senderId.equals(SYSTEM_ID)
                ? accountRepository.findById(senderId)
                    .filter(account -> account.getAccountType() == AccountType.USER)
                    .orElse(null)
                : null;
        if (receiverId != null && receiverId.equals(SYSTEM_ID)) {
            throw new RuntimeException("系统消息只允许系统向用户发送");
        }
        Account receiver = receiverId != null ? accountRepository.findById(receiverId)
                .filter(account -> account.getAccountType() == AccountType.USER)
                .orElseThrow(() -> new RuntimeException("接收方不存在")) : null;

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setRelatedContractId(contractId);
        message.setRelatedHouseId(houseId);
        message.setRelatedRequestId(requestId);
        message.setRequireAction(requireAction);
        if (Boolean.TRUE.equals(requireAction)) {
            message.setStatus(MessageStatus.PENDING);
        }
        return messageRepository.save(message);
    }

    public Message sendOperatorMessage(Long senderOperatorId, Long receiverOperatorId, String title, String content,
                                       MessageType type, Long contractId, Long requestId, boolean requireAction) {
        return sendOperatorMessage(senderOperatorId, receiverOperatorId, title, content, type, contractId, null, requestId, requireAction);
    }

    public Message sendOperatorMessage(Long senderOperatorId, Long receiverOperatorId, String title, String content,
                                       MessageType type, Long contractId, Long houseId, Long requestId, boolean requireAction) {
        Account sender = senderOperatorId != null
                ? accountRepository.findById(senderOperatorId)
                .orElse(null)
                : null;
        Account receiver = receiverOperatorId != null
                ? accountRepository.findById(receiverOperatorId)
                .orElseThrow(() -> new RuntimeException("接收业务账号不存在"))
                : null;
        if (sender != null && sender.getAccountType() == AccountType.USER) {
            throw new RuntimeException("发送方必须是管理员/业务员");
        }
        if (receiver != null && receiver.getAccountType() == AccountType.USER) {
            throw new RuntimeException("接收方必须是管理员/业务员");
        }

        Message message = new Message();
        message.setSenderOperatorId(sender != null ? sender.getId() : null);
        message.setSenderOperatorName(sender != null ? sender.getDisplayName() : SYSTEM_NAME);
        message.setReceiverOperatorId(receiver != null ? receiver.getId() : null);
        message.setReceiverOperatorName(receiver != null ? receiver.getDisplayName() : null);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setRelatedContractId(contractId);
        message.setRelatedHouseId(houseId);
        message.setRelatedRequestId(requestId);
        message.setRequireAction(requireAction);
        if (Boolean.TRUE.equals(requireAction)) {
            message.setStatus(MessageStatus.PENDING);
        }
        return messageRepository.save(message);
    }

    public Message sendOperatorToUserMessage(Long senderOperatorId, Long receiverUserId, String title, String content,
                                             MessageType type, Long contractId, Long requestId, boolean requireAction) {
        return sendOperatorToUserMessage(senderOperatorId, receiverUserId, title, content, type, contractId, null, requestId, requireAction);
    }

    public Message sendOperatorToUserMessage(Long senderOperatorId, Long receiverUserId, String title, String content,
                                             MessageType type, Long contractId, Long houseId, Long requestId, boolean requireAction) {
        if (receiverUserId != null && receiverUserId.equals(SYSTEM_ID)) {
            throw new RuntimeException("不能向系统发送消息");
        }
        Account sender = senderOperatorId != null
                ? accountRepository.findById(senderOperatorId)
                .orElseThrow(() -> new RuntimeException("发送方业务账号不存在"))
                : null;
        if (sender != null && sender.getAccountType() == AccountType.USER) {
            throw new RuntimeException("发送方必须是管理员/业务员");
        }
        Account receiver = receiverUserId != null
                ? accountRepository.findById(receiverUserId)
                .filter(account -> account.getAccountType() == AccountType.USER)
                .orElseThrow(() -> new RuntimeException("接收方不存在"))
                : null;

        Message message = new Message();
        message.setSenderOperatorId(sender != null ? sender.getId() : null);
        message.setSenderOperatorName(sender != null ? sender.getDisplayName() : SYSTEM_NAME);
        message.setReceiver(receiver);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setRelatedContractId(contractId);
        message.setRelatedHouseId(houseId);
        message.setRelatedRequestId(requestId);
        message.setRequireAction(requireAction);
        if (Boolean.TRUE.equals(requireAction)) {
            message.setStatus(MessageStatus.PENDING);
        }
        return messageRepository.save(message);
    }

    public Message sendUserToOperatorMessage(Long senderUserId, Long receiverOperatorId, String title, String content,
                                             MessageType type, Long contractId, Long requestId, boolean requireAction) {
        return sendUserToOperatorMessage(senderUserId, receiverOperatorId, title, content, type, contractId, null, requestId, requireAction);
    }

    public Message sendUserToOperatorMessage(Long senderUserId, Long receiverOperatorId, String title, String content,
                                             MessageType type, Long contractId, Long houseId, Long requestId, boolean requireAction) {
        if (receiverOperatorId == null) {
            throw new RuntimeException("接收业务账号不能为空");
        }
        Account sender = senderUserId != null
                ? accountRepository.findById(senderUserId)
                .filter(account -> account.getAccountType() == AccountType.USER)
                .orElseThrow(() -> new RuntimeException("发送方用户不存在"))
                : null;
        Account receiver = accountRepository.findById(receiverOperatorId)
                .orElseThrow(() -> new RuntimeException("接收业务账号不存在"));
        if (receiver.getAccountType() == AccountType.USER) {
            throw new RuntimeException("接收方必须是管理员/业务员");
        }

        Message message = new Message();
        message.setSender(sender);
        message.setReceiverOperatorId(receiver.getId());
        message.setReceiverOperatorName(receiver.getDisplayName());
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setRelatedContractId(contractId);
        message.setRelatedHouseId(houseId);
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

    public List<MessageDTO> getOperatorMessageContacts(Long operatorId) {
        List<Message> messages = messageRepository.findBySenderOperatorIdOrReceiverOperatorIdOrderByCreatedAtDesc(operatorId);
        Map<String, List<Message>> grouped = messages.stream()
                .collect(Collectors.groupingBy(msg -> buildContactKey(resolveOperatorContactRef(msg, operatorId))));

        return grouped.entrySet().stream()
                .map(entry -> {
                    ContactRef ref = parseContactKey(entry.getKey());
                    List<Message> list = entry.getValue();
                    Message latest = list.stream().max(Comparator.comparing(Message::getCreatedAt)).orElse(null);
                    if (latest == null) {
                        return null;
                    }
                    long unreadCount = list.stream()
                            .filter(msg -> msg.getReceiverOperatorId() != null && msg.getReceiverOperatorId().equals(operatorId))
                            .filter(msg -> msg.getStatus() == MessageStatus.UNREAD)
                            .count();
                    MessageDTO dto = convert(latest);
                    dto.setUnreadCount(unreadCount);
                    dto.setContactType(ref.type());
                    dto.setContactId(ref.id());
                    dto.setContactKey(entry.getKey());
                    return dto;
                })
                .filter(dto -> dto != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<MessageDTO> getMessageContacts(Long userId) {
        List<Message> messages = messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId);
        Map<String, List<Message>> groupedMessages = messages.stream()
                .filter(msg -> !isArchivedForUser(msg, userId))
                .collect(Collectors.groupingBy(msg -> buildContactKey(resolveUserContactRef(msg, userId))));

        return groupedMessages.entrySet().stream()
                .map(entry -> {
                    ContactRef ref = parseContactKey(entry.getKey());
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
                    dto.setContactType(ref.type());
                    dto.setContactId(ref.id());
                    dto.setContactKey(entry.getKey());
                    return dto;
                })
                .filter(dto -> dto != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<MessageDTO> getChatMessages(Long userId, Long contactId) {
        return getChatMessages(userId, contactId, null);
    }

    public List<MessageDTO> getChatMessages(Long userId, Long contactId, String contactType) {
        List<Message> messages = messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId);

        List<Message> chatMessages;
        if (isSystemContact(contactId, contactType)) {
            chatMessages = messages.stream()
                    .filter(msg -> msg.getSender() == null
                            && msg.getSenderOperatorId() == null
                            && msg.getReceiver() != null
                            && msg.getReceiver().getId().equals(userId))
                    .filter(msg -> !isArchivedForUser(msg, userId))
                    .sorted(Comparator.comparing(Message::getCreatedAt))
                    .collect(Collectors.toList());
        } else if (CONTACT_USER.equalsIgnoreCase(contactType)) {
            chatMessages = messages.stream()
                    .filter(msg -> isUserUserPair(msg, userId, contactId))
                    .filter(msg -> !isArchivedForUser(msg, userId))
                    .sorted(Comparator.comparing(Message::getCreatedAt))
                    .collect(Collectors.toList());
        } else if (CONTACT_OPERATOR.equalsIgnoreCase(contactType)) {
            chatMessages = messages.stream()
                    .filter(msg -> isUserOperatorPair(msg, userId, contactId))
                    .filter(msg -> !isArchivedForUser(msg, userId))
                    .sorted(Comparator.comparing(Message::getCreatedAt))
                    .collect(Collectors.toList());
        } else {
            chatMessages = messages.stream()
                    .filter(msg -> isUserUserPair(msg, userId, contactId) || isUserOperatorPair(msg, userId, contactId))
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

    public List<MessageDTO> getOperatorChatMessages(Long operatorId, Long contactId) {
        return getOperatorChatMessages(operatorId, contactId, null);
    }

    public List<MessageDTO> getOperatorChatMessages(Long operatorId, Long contactId, String contactType) {
        List<Message> messages = messageRepository.findBySenderOperatorIdOrReceiverOperatorIdOrderByCreatedAtDesc(operatorId);

        List<Message> chatMessages;
        if (isSystemContact(contactId, contactType)) {
            chatMessages = messages.stream()
                    .filter(msg -> msg.getSenderOperatorId() == null
                            && msg.getSender() == null
                            && msg.getReceiverOperatorId() != null
                            && msg.getReceiverOperatorId().equals(operatorId))
                    .sorted(Comparator.comparing(Message::getCreatedAt))
                    .collect(Collectors.toList());
        } else if (CONTACT_OPERATOR.equalsIgnoreCase(contactType)) {
            chatMessages = messages.stream()
                    .filter(msg -> isOperatorOperatorPair(msg, operatorId, contactId))
                    .sorted(Comparator.comparing(Message::getCreatedAt))
                    .collect(Collectors.toList());
        } else if (CONTACT_USER.equalsIgnoreCase(contactType)) {
            chatMessages = messages.stream()
                    .filter(msg -> isOperatorUserPair(msg, operatorId, contactId))
                    .sorted(Comparator.comparing(Message::getCreatedAt))
                    .collect(Collectors.toList());
        } else {
            chatMessages = messages.stream()
                    .filter(msg -> isOperatorOperatorPair(msg, operatorId, contactId) || isOperatorUserPair(msg, operatorId, contactId))
                    .sorted(Comparator.comparing(Message::getCreatedAt))
                    .collect(Collectors.toList());
        }

        List<Message> unreadMessages = chatMessages.stream()
                .filter(msg -> msg.getReceiverOperatorId() != null && msg.getReceiverOperatorId().equals(operatorId)
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
        Account account = accountRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        if (account.getAccountType() != AccountType.ADMIN) {
            throw new RuntimeException("权限不足");
        }
        return getOperatorMessages(operatorId);
    }

    public void markAsRead(Long messageId, Long userId, Long operatorId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        if (!canOperateMessage(message, userId, operatorId)) {
            throw new RuntimeException("无权操作该消息");
        }
        message.setStatus(MessageStatus.READ);
        message.setReadAt(LocalDateTime.now());
        messageRepository.save(message);
    }

    public void updateMessageStatus(Long messageId, Long userId, Long operatorId, MessageStatus status) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        if (!canOperateMessage(message, userId, operatorId)) {
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

    public void markAllOperatorMessagesAsRead(Long operatorId) {
        List<Message> unreadMessages = messageRepository.findByReceiverOperatorIdAndStatus(operatorId, MessageStatus.UNREAD);
        for (Message message : unreadMessages) {
            message.setStatus(MessageStatus.READ);
            message.setReadAt(LocalDateTime.now());
        }
        messageRepository.saveAll(unreadMessages);
    }

    public void archiveContactMessages(Long userId, Long contactId) {
        archiveContactMessages(userId, contactId, null);
    }

    public void archiveContactMessages(Long userId, Long contactId, String contactType) {
        List<Message> messages = messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId);
        List<Message> contactMessages = messages.stream()
                .filter(msg -> {
                    Long senderId = msg.getSender() != null ? msg.getSender().getId() : null;
                    Long receiverId = msg.getReceiver() != null ? msg.getReceiver().getId() : null;
                    if (contactId.equals(SYSTEM_ID)) {
                        return false;
                    }
                    if (CONTACT_OPERATOR.equalsIgnoreCase(contactType)) {
                        return (msg.getSenderOperatorId() != null && msg.getSenderOperatorId().equals(contactId) &&
                                receiverId != null && receiverId.equals(userId)) ||
                                (senderId != null && senderId.equals(userId) &&
                                        msg.getReceiverOperatorId() != null && msg.getReceiverOperatorId().equals(contactId));
                    }
                    if (CONTACT_USER.equalsIgnoreCase(contactType)) {
                        return (senderId != null && senderId.equals(contactId) && receiverId != null && receiverId.equals(userId)) ||
                                (senderId != null && senderId.equals(userId) && receiverId != null && receiverId.equals(contactId));
                    }
                    return (senderId != null && senderId.equals(contactId) && receiverId != null && receiverId.equals(userId)) ||
                            (senderId != null && senderId.equals(userId) && receiverId != null && receiverId.equals(contactId)) ||
                            ((msg.getSenderOperatorId() != null && msg.getSenderOperatorId().equals(contactId) &&
                                    receiverId != null && receiverId.equals(userId)) ||
                                    (senderId != null && senderId.equals(userId) &&
                                            msg.getReceiverOperatorId() != null && msg.getReceiverOperatorId().equals(contactId)));
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
        Set<String> archivedIds = parseArchivedIds(message.getArchivedByUserIds());
        archivedIds.add(userId.toString());
        message.setArchivedByUserIds(String.join(",", archivedIds));
    }

    private boolean isArchivedForUser(Message message, Long userId) {
        Set<String> archivedIds = parseArchivedIds(message.getArchivedByUserIds());
        return archivedIds.contains(userId.toString());
    }

    private Set<String> parseArchivedIds(String archivedByUserIds) {
        Set<String> ids = new LinkedHashSet<>();
        if (archivedByUserIds == null || archivedByUserIds.isBlank()) {
            return ids;
        }
        for (String part : archivedByUserIds.split(",")) {
            String id = part.trim();
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids;
    }

    private boolean canOperateMessage(Message message, Long userId, Long operatorId) {
        if (userId == null && operatorId == null) {
            throw new RuntimeException("未识别当前账号");
        }
        if (userId != null) {
            return message.getReceiver() != null && message.getReceiver().getId().equals(userId);
        }
        return message.getReceiverOperatorId() != null && message.getReceiverOperatorId().equals(operatorId);
    }

    private ContactRef resolveUserContactRef(Message message, Long userId) {
        if (message.getSender() != null && message.getSender().getId().equals(userId)) {
            if (message.getReceiver() != null) {
                return new ContactRef(CONTACT_USER, message.getReceiver().getId());
            }
            if (message.getReceiverOperatorId() != null) {
                return new ContactRef(CONTACT_OPERATOR, message.getReceiverOperatorId());
            }
            return new ContactRef(CONTACT_SYSTEM, SYSTEM_ID);
        }
        if (message.getSender() != null) {
            return new ContactRef(CONTACT_USER, message.getSender().getId());
        }
        if (message.getSenderOperatorId() != null) {
            return new ContactRef(CONTACT_OPERATOR, message.getSenderOperatorId());
        }
        return new ContactRef(CONTACT_SYSTEM, SYSTEM_ID);
    }

    private ContactRef resolveOperatorContactRef(Message message, Long operatorId) {
        if (message.getSenderOperatorId() != null && message.getSenderOperatorId().equals(operatorId)) {
            if (message.getReceiver() != null) {
                return new ContactRef(CONTACT_USER, message.getReceiver().getId());
            }
            if (message.getReceiverOperatorId() != null) {
                return new ContactRef(CONTACT_OPERATOR, message.getReceiverOperatorId());
            }
            return new ContactRef(CONTACT_SYSTEM, SYSTEM_ID);
        }
        if (message.getReceiverOperatorId() != null && message.getReceiverOperatorId().equals(operatorId)) {
            if (message.getSender() != null) {
                return new ContactRef(CONTACT_USER, message.getSender().getId());
            }
            if (message.getSenderOperatorId() != null) {
                return new ContactRef(CONTACT_OPERATOR, message.getSenderOperatorId());
            }
            return new ContactRef(CONTACT_SYSTEM, SYSTEM_ID);
        }
        return new ContactRef(CONTACT_SYSTEM, SYSTEM_ID);
    }

    private String buildContactKey(ContactRef ref) {
        return ref.type() + ":" + ref.id();
    }

    private ContactRef parseContactKey(String contactKey) {
        if (contactKey == null || !contactKey.contains(":")) {
            return new ContactRef(CONTACT_SYSTEM, SYSTEM_ID);
        }
        String[] parts = contactKey.split(":", 2);
        String type = parts[0];
        Long id;
        try {
            id = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            id = SYSTEM_ID;
        }
        if (!CONTACT_USER.equals(type) && !CONTACT_OPERATOR.equals(type) && !CONTACT_SYSTEM.equals(type)) {
            type = CONTACT_SYSTEM;
            id = SYSTEM_ID;
        }
        return new ContactRef(type, id);
    }

    private boolean isSystemContact(Long contactId, String contactType) {
        if (contactId != null && contactId.equals(SYSTEM_ID)) {
            return true;
        }
        return CONTACT_SYSTEM.equalsIgnoreCase(contactType);
    }

    private boolean isUserUserPair(Message message, Long userId, Long contactId) {
        return (message.getSender() != null && message.getSender().getId().equals(contactId) &&
                message.getReceiver() != null && message.getReceiver().getId().equals(userId)) ||
                (message.getSender() != null && message.getSender().getId().equals(userId) &&
                        message.getReceiver() != null && message.getReceiver().getId().equals(contactId));
    }

    private boolean isUserOperatorPair(Message message, Long userId, Long contactId) {
        return (message.getSenderOperatorId() != null && message.getSenderOperatorId().equals(contactId) &&
                message.getReceiver() != null && message.getReceiver().getId().equals(userId)) ||
                (message.getSender() != null && message.getSender().getId().equals(userId) &&
                        message.getReceiverOperatorId() != null && message.getReceiverOperatorId().equals(contactId));
    }

    private boolean isOperatorOperatorPair(Message message, Long operatorId, Long contactId) {
        return (message.getSenderOperatorId() != null && message.getSenderOperatorId().equals(contactId) &&
                message.getReceiverOperatorId() != null && message.getReceiverOperatorId().equals(operatorId)) ||
                (message.getSenderOperatorId() != null && message.getSenderOperatorId().equals(operatorId) &&
                        message.getReceiverOperatorId() != null && message.getReceiverOperatorId().equals(contactId));
    }

    private boolean isOperatorUserPair(Message message, Long operatorId, Long contactId) {
        return (message.getSenderOperatorId() != null && message.getSenderOperatorId().equals(operatorId) &&
                message.getReceiver() != null && message.getReceiver().getId().equals(contactId)) ||
                (message.getSender() != null && message.getSender().getId().equals(contactId) &&
                        message.getReceiverOperatorId() != null && message.getReceiverOperatorId().equals(operatorId));
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
            if (message.getSenderOperatorId() != null) {
                dto.setSenderId(message.getSenderOperatorId());
                dto.setSenderName(message.getSenderOperatorName() == null ? SYSTEM_NAME : message.getSenderOperatorName());
            } else {
                dto.setSenderId(SYSTEM_ID);
                dto.setSenderName(SYSTEM_NAME);
            }
        }
        if (message.getReceiver() != null) {
            dto.setReceiverId(message.getReceiver().getId());
            dto.setReceiverName(message.getReceiver().getRealName());
        } else if (message.getReceiverOperatorId() != null) {
            dto.setReceiverId(message.getReceiverOperatorId());
            dto.setReceiverName(message.getReceiverOperatorName());
        }
        dto.setSenderOperatorId(message.getSenderOperatorId());
        dto.setSenderOperatorName(message.getSenderOperatorName());
        dto.setReceiverOperatorId(message.getReceiverOperatorId());
        dto.setReceiverOperatorName(message.getReceiverOperatorName());
        dto.setRelatedContractId(message.getRelatedContractId());
        dto.setRelatedHouseId(message.getRelatedHouseId());
        dto.setRelatedRequestId(message.getRelatedRequestId());
        dto.setRequireAction(message.getRequireAction());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setReadAt(message.getReadAt());
        return dto;
    }

    public void notifyAdmins(String title, String content, Long contractId, Long requestId, boolean requireAction) {
        notifyAdmins(title, content, contractId, null, requestId, requireAction, MessageType.ADMIN_NOTIFICATION);
    }

    public void notifyAdmins(String title, String content, Long contractId, Long houseId, Long requestId, boolean requireAction) {
        notifyAdmins(title, content, contractId, houseId, requestId, requireAction, MessageType.ADMIN_NOTIFICATION);
    }

    public void notifyAdmins(String title,
                             String content,
                             Long contractId,
                             Long houseId,
                             Long requestId,
                             boolean requireAction,
                             MessageType type) {
        accountRepository.findByAccountTypeAndEnabled(AccountType.ADMIN, true).forEach(admin ->
                sendOperatorMessage(null, admin.getId(), title, content,
                        type, contractId, houseId, requestId, requireAction)
        );
    }

    public void notifyStaff(Long staffOperatorId, String title, String content, Long contractId, Long requestId, boolean requireAction, MessageType type) {
        notifyStaff(staffOperatorId, title, content, contractId, null, requestId, requireAction, type);
    }

    public void notifyStaff(Long staffOperatorId, String title, String content, Long contractId, Long houseId, Long requestId, boolean requireAction, MessageType type) {
        sendOperatorMessage(null, staffOperatorId, title, content, type, contractId, houseId, requestId, requireAction);
    }

    private record ContactRef(String type, Long id) {}
}
