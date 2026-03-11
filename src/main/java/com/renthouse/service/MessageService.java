package com.renthouse.service;

import com.renthouse.domain.Account;
import com.renthouse.domain.Message;
import com.renthouse.dto.MessageDTO;
import com.renthouse.enums.AccountType;
import com.renthouse.enums.MessageStatus;
import com.renthouse.enums.MessageType;
import com.renthouse.repository.AccountRepository;
import com.renthouse.repository.MessageRepository;
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
    private static final String CONTACT_SYSTEM = "SYSTEM";

    public Message sendMessage(Long senderId,
                               Long receiverId,
                               String title,
                               String content,
                               MessageType type,
                               Long contractId,
                               Long requestId,
                               boolean requireAction) {
        return sendMessage(senderId, receiverId, title, content, type, contractId, null, requestId, requireAction);
    }

    public Message sendMessage(Long senderId,
                               Long receiverId,
                               String title,
                               String content,
                               MessageType type,
                               Long contractId,
                               Long houseId,
                               Long requestId,
                               boolean requireAction) {
        if (receiverId == null || receiverId.equals(SYSTEM_ID)) {
            throw new RuntimeException("接收方不能为空且不能是系统");
        }

        Account sender = null;
        if (senderId != null && !senderId.equals(SYSTEM_ID)) {
            sender = accountRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("发送方不存在"));
        }

        Account receiver = accountRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("接收方不存在"));

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

    public List<MessageDTO> getMessages(Long accountId) {
        return messageRepository.findByReceiverIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    public List<MessageDTO> getMessageContacts(Long accountId) {
        Account account = requireAccount(accountId);
        List<Message> messages = messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(accountId);
        if (account.getAccountType() == AccountType.USER) {
            messages = messages.stream().filter(msg -> !isArchivedForUser(msg, accountId)).toList();
        }

        Map<String, List<Message>> grouped = messages.stream()
                .collect(Collectors.groupingBy(msg -> buildContactKey(resolveContactRef(msg, accountId))));

        return grouped.entrySet().stream()
                .map(entry -> {
                    ContactRef ref = parseContactKey(entry.getKey());
                    List<Message> list = entry.getValue();
                    Message latest = list.stream().max(Comparator.comparing(Message::getCreatedAt)).orElse(null);
                    if (latest == null) {
                        return null;
                    }
                    long unreadCount = list.stream()
                            .filter(msg -> msg.getReceiver() != null && msg.getReceiver().getId().equals(accountId))
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

    public List<MessageDTO> getChatMessages(Long accountId, Long contactId) {
        return getChatMessages(accountId, contactId, null);
    }

    public List<MessageDTO> getChatMessages(Long accountId, Long contactId, String contactType) {
        Account account = requireAccount(accountId);
        List<Message> messages = messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(accountId);

        List<Message> chatMessages;
        if (isSystemContact(contactId, contactType)) {
            chatMessages = messages.stream()
                    .filter(msg -> msg.getSender() == null
                            && msg.getReceiver() != null
                            && msg.getReceiver().getId().equals(accountId))
                    .sorted(Comparator.comparing(Message::getCreatedAt))
                    .collect(Collectors.toList());
        } else {
            chatMessages = messages.stream()
                    .filter(msg -> isAccountPair(msg, accountId, contactId))
                    .filter(msg -> matchContactType(msg, accountId, contactType))
                    .sorted(Comparator.comparing(Message::getCreatedAt))
                    .collect(Collectors.toList());
        }

        if (account.getAccountType() == AccountType.USER) {
            chatMessages = chatMessages.stream().filter(msg -> !isArchivedForUser(msg, accountId)).toList();
        }

        List<Message> unreadMessages = chatMessages.stream()
                .filter(msg -> msg.getReceiver() != null
                        && msg.getReceiver().getId().equals(accountId)
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

    public long getUnreadMessageCount(Long accountId) {
        return messageRepository.countByReceiverIdAndStatus(accountId, MessageStatus.UNREAD);
    }

    public List<MessageDTO> getAdminMessages(Long accountId) {
        Account account = requireAccount(accountId);
        if (account.getAccountType() != AccountType.ADMIN) {
            throw new RuntimeException("权限不足");
        }
        return getMessages(accountId);
    }

    public void markAsRead(Long messageId, Long accountId) {
        Message message = requireMessage(messageId);
        if (!canOperateMessage(message, accountId)) {
            throw new RuntimeException("无权操作该消息");
        }
        message.setStatus(MessageStatus.READ);
        message.setReadAt(LocalDateTime.now());
        messageRepository.save(message);
    }

    public void updateMessageStatus(Long messageId, Long accountId, MessageStatus status) {
        Message message = requireMessage(messageId);
        if (!canOperateMessage(message, accountId)) {
            throw new RuntimeException("无权操作该消息");
        }
        message.setStatus(status);
        if (status == MessageStatus.READ && message.getReadAt() == null) {
            message.setReadAt(LocalDateTime.now());
        }
        messageRepository.save(message);
    }

    public void markAllAsRead(Long accountId) {
        List<Message> unreadMessages = messageRepository.findByReceiverIdAndStatus(accountId, MessageStatus.UNREAD);
        for (Message message : unreadMessages) {
            message.setStatus(MessageStatus.READ);
            message.setReadAt(LocalDateTime.now());
        }
        if (!unreadMessages.isEmpty()) {
            messageRepository.saveAll(unreadMessages);
        }
    }

    public void archiveContactMessages(Long userId, Long contactId) {
        archiveContactMessages(userId, contactId, null);
    }

    public void archiveContactMessages(Long userId, Long contactId, String contactType) {
        Account user = requireAccount(userId);
        if (user.getAccountType() != AccountType.USER) {
            throw new RuntimeException("仅普通用户支持会话归档");
        }
        if (contactId == null || contactId.equals(SYSTEM_ID)) {
            throw new RuntimeException("系统会话不可归档");
        }

        List<Message> messages = messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId);
        List<Message> contactMessages = messages.stream()
                .filter(msg -> isAccountPair(msg, userId, contactId))
                .filter(msg -> matchContactType(msg, userId, contactType))
                .collect(Collectors.toList());

        for (Message message : contactMessages) {
            addArchivedUserId(message, userId);
        }

        if (!contactMessages.isEmpty()) {
            messageRepository.saveAll(contactMessages);
        }
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
        accountRepository.findByAccountTypeAndEnabled(AccountType.ADMIN, true)
                .forEach(admin -> sendMessage(null, admin.getId(), title, content, type, contractId, houseId, requestId, requireAction));
    }

    public void notifyStaff(Long staffAccountId,
                            String title,
                            String content,
                            Long contractId,
                            Long requestId,
                            boolean requireAction,
                            MessageType type) {
        notifyStaff(staffAccountId, title, content, contractId, null, requestId, requireAction, type);
    }

    public void notifyStaff(Long staffAccountId,
                            String title,
                            String content,
                            Long contractId,
                            Long houseId,
                            Long requestId,
                            boolean requireAction,
                            MessageType type) {
        if (staffAccountId == null) {
            throw new RuntimeException("业务员账号不能为空");
        }
        Account staff = requireAccount(staffAccountId);
        if (staff.getAccountType() != AccountType.STAFF) {
            throw new RuntimeException("仅可向业务员发送此类消息");
        }
        sendMessage(null, staffAccountId, title, content, type, contractId, houseId, requestId, requireAction);
    }

    private Account requireAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
    }

    private Message requireMessage(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
    }

    private boolean isAccountPair(Message message, Long accountId, Long contactId) {
        Long senderId = message.getSender() != null ? message.getSender().getId() : null;
        Long receiverId = message.getReceiver() != null ? message.getReceiver().getId() : null;
        return (senderId != null && senderId.equals(accountId) && receiverId != null && receiverId.equals(contactId))
                || (senderId != null && senderId.equals(contactId) && receiverId != null && receiverId.equals(accountId));
    }

    private boolean matchContactType(Message message, Long accountId, String contactType) {
        if (contactType == null || contactType.isBlank()) {
            return true;
        }
        if (CONTACT_SYSTEM.equalsIgnoreCase(contactType)) {
            return isSystemContact(null, contactType);
        }
        Account contact = resolveContactAccount(message, accountId);
        if (contact == null || contact.getAccountType() == null) {
            return false;
        }
        return contact.getAccountType().name().equalsIgnoreCase(contactType);
    }

    private boolean canOperateMessage(Message message, Long accountId) {
        if (accountId == null) {
            throw new RuntimeException("未识别当前账号");
        }
        return message.getReceiver() != null && message.getReceiver().getId().equals(accountId);
    }

    private ContactRef resolveContactRef(Message message, Long currentAccountId) {
        if (message.getSender() == null && message.getReceiver() != null && message.getReceiver().getId().equals(currentAccountId)) {
            return new ContactRef(CONTACT_SYSTEM, SYSTEM_ID);
        }

        Account contact = resolveContactAccount(message, currentAccountId);
        if (contact == null) {
            return new ContactRef(CONTACT_SYSTEM, SYSTEM_ID);
        }
        String type = contact.getAccountType() == null ? CONTACT_SYSTEM : contact.getAccountType().name();
        return new ContactRef(type, contact.getId());
    }

    private Account resolveContactAccount(Message message, Long currentAccountId) {
        if (message.getSender() != null && message.getSender().getId().equals(currentAccountId)) {
            return message.getReceiver();
        }
        if (message.getReceiver() != null && message.getReceiver().getId().equals(currentAccountId)) {
            return message.getSender();
        }
        return null;
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
        } catch (NumberFormatException ex) {
            return new ContactRef(CONTACT_SYSTEM, SYSTEM_ID);
        }

        if (CONTACT_SYSTEM.equalsIgnoreCase(type)) {
            return new ContactRef(CONTACT_SYSTEM, SYSTEM_ID);
        }

        if (type == null || type.isBlank()) {
            return new ContactRef(CONTACT_SYSTEM, SYSTEM_ID);
        }
        return new ContactRef(type.toUpperCase(), id);
    }

    private boolean isSystemContact(Long contactId, String contactType) {
        if (contactId != null && contactId.equals(SYSTEM_ID)) {
            return true;
        }
        return CONTACT_SYSTEM.equalsIgnoreCase(contactType);
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

    private MessageDTO convert(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setTitle(message.getTitle());
        dto.setContent(message.getContent());
        dto.setType(message.getType());
        dto.setStatus(message.getStatus());

        if (message.getSender() != null) {
            dto.setSenderId(message.getSender().getId());
            dto.setSenderName(displayNameOf(message.getSender()));
            dto.setSenderType(message.getSender().getAccountType() == null ? null : message.getSender().getAccountType().name());
        } else {
            dto.setSenderId(SYSTEM_ID);
            dto.setSenderName(SYSTEM_NAME);
            dto.setSenderType(CONTACT_SYSTEM);
        }

        if (message.getReceiver() != null) {
            dto.setReceiverId(message.getReceiver().getId());
            dto.setReceiverName(displayNameOf(message.getReceiver()));
            dto.setReceiverType(message.getReceiver().getAccountType() == null ? null : message.getReceiver().getAccountType().name());
        }

        dto.setRelatedContractId(message.getRelatedContractId());
        dto.setRelatedHouseId(message.getRelatedHouseId());
        dto.setRelatedRequestId(message.getRelatedRequestId());
        dto.setRequireAction(message.getRequireAction());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setReadAt(message.getReadAt());
        return dto;
    }

    private String displayNameOf(Account account) {
        if (account.getDisplayName() != null && !account.getDisplayName().isBlank()) {
            return account.getDisplayName();
        }
        if (account.getRealName() != null && !account.getRealName().isBlank()) {
            return account.getRealName();
        }
        return account.getUsername();
    }

    private record ContactRef(String type, Long id) {
    }
}
