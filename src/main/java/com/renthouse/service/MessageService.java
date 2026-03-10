package com.renthouse.service;

import com.renthouse.domain.Account;
import com.renthouse.domain.Message;
import com.renthouse.domain.User;
import com.renthouse.dto.MessageDTO;
import com.renthouse.enums.AccountType;
import com.renthouse.enums.MessageStatus;
import com.renthouse.enums.MessageType;
import com.renthouse.repository.AccountRepository;
import com.renthouse.repository.MessageRepository;
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
    private AccountRepository accountRepository;

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

    public List<MessageDTO> getMessages(Long userId) {
        return messageRepository.findByReceiverIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    public List<MessageDTO> getMessageContacts(Long userId) {
        // 获取与当前用户相关的所有消息（作为发送方或接收方）
        List<Message> messages = messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId);
            
        // 按联系人分组，获取每个联系人的最新消息（排除已对当前用户归档的消息）
        Map<Long, List<Message>> groupedMessages = messages.stream()
                .filter(msg -> !isArchivedForUser(msg, userId)) // 过滤已对当前用户归档的消息
                .collect(Collectors.groupingBy(msg -> {
                    // 确定对方用户 ID
                    if (msg.getSender() != null && msg.getSender().getId().equals(userId)) {
                        return msg.getReceiver() != null ? msg.getReceiver().getId() : SYSTEM_ID; // 系统消息
                    } else {
                        return msg.getSender() != null ? msg.getSender().getId() : SYSTEM_ID;
                    }
                }));
            
        // 为每个联系人创建一条代表消息
        return groupedMessages.entrySet().stream()
                .map(entry -> {
                    // 获取最新的消息
                    Message latestMessage = entry.getValue().stream()
                            .max(Comparator.comparing(Message::getCreatedAt))
                            .orElse(null);
                        
                    if (latestMessage != null) {
                        // 统计未读消息数量（排除已归档）
                        long unreadCount = entry.getValue().stream()
                                .filter(msg -> msg.getReceiver() != null && msg.getReceiver().getId().equals(userId))
                                .filter(msg -> msg.getStatus() == MessageStatus.UNREAD)
                                .count();
                            
                        MessageDTO dto = convert(latestMessage);
                        // 设置未读消息数量
                        dto.setUnreadCount(unreadCount);
                        return dto;
                    }
                    return null;
                })
                .filter(dto -> dto != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // 按时间倒序排列
                .collect(Collectors.toList());
    }

    public List<MessageDTO> getChatMessages(Long userId, Long contactId) {
        // 获取当前用户相关的全部消息
        List<Message> messages = messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId);

        List<Message> chatMessages;

        if (contactId != null && contactId.equals(SYSTEM_ID)) {
            // 系统/管理员通知：sender 为空且接收者是当前用户
            chatMessages = messages.stream()
                    .filter(msg -> msg.getSender() == null
                            && msg.getReceiver() != null
                            && msg.getReceiver().getId().equals(userId))
                    .filter(msg -> !isArchivedForUser(msg, userId)) // 过滤已对当前用户归档的消息
                    .sorted(Comparator.comparing(Message::getCreatedAt))
                    .collect(Collectors.toList());
        } else {
            // 双方聊天记录
            chatMessages = messages.stream()
                    .filter(msg ->
                            (msg.getSender() != null && msg.getSender().getId().equals(contactId) &&
                                    msg.getReceiver() != null && msg.getReceiver().getId().equals(userId)) ||
                                    (msg.getSender() != null && msg.getSender().getId().equals(userId) &&
                                            msg.getReceiver() != null && msg.getReceiver().getId().equals(contactId))
                    )
                    .filter(msg -> !isArchivedForUser(msg, userId)) // 过滤已对当前用户归档的消息
                    .sorted(Comparator.comparing(Message::getCreatedAt)) // 按时间正序排列
                    .collect(Collectors.toList());
        }

        // 将当前用户收到的未读消息标记为已读
        List<Message> unreadMessages = chatMessages.stream()
                .filter(msg -> msg.getReceiver() != null && msg.getReceiver().getId().equals(userId)
                        && msg.getStatus() == MessageStatus.UNREAD)
                .collect(Collectors.toList());

        for (Message message : unreadMessages) {
            message.setStatus(MessageStatus.READ);
            message.setReadAt(LocalDateTime.now());
        }

        if (!unreadMessages.isEmpty()) {
            messageRepository.saveAll(unreadMessages);
        }

        return chatMessages.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    public long getUnreadMessageCount(Long userId) {
        return messageRepository.countByReceiverIdAndStatus(userId, MessageStatus.UNREAD);
    }

    public List<MessageDTO> getAdminMessages(Long adminAccountId) {
        Account account = accountRepository.findById(adminAccountId)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        if (account.getAccountType() != AccountType.ADMIN) {
            throw new RuntimeException("权限不足");
        }

        return messageRepository.findAll()
                .stream()
                .filter(msg -> msg.getType() == MessageType.ADMIN_NOTIFICATION || msg.getReceiver() == null)
                .map(this::convert)
                .collect(Collectors.toList());
    }

    public void markAsRead(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        if (message.getReceiver() == null || !message.getReceiver().getId().equals(userId)) {
            throw new RuntimeException("无权操作该消息");
        }
        message.setStatus(MessageStatus.READ);
        message.setReadAt(LocalDateTime.now());
        messageRepository.save(message);
    }

    public void updateMessageStatus(Long messageId, Long userId, MessageStatus status) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        if (message.getReceiver() == null || !message.getReceiver().getId().equals(userId)) {
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

    /**
     * 归档与特定联系人的所有消息（单方面隐藏，数据库保留记录）
     * 只隐藏当前用户视角的消息，不影响对方的视图
     */
    public void archiveContactMessages(Long userId, Long contactId) {
        System.out.println("开始归档：userId=" + userId + ", contactId=" + contactId);
        List<Message> messages = messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId);
        System.out.println("获取到 " + messages.size() + " 条消息");
        
        // 过滤出需要归档的消息（只归档当前用户视角的消息）
        List<Message> contactMessages = messages.stream()
                .filter(msg -> {
                    Long senderId = msg.getSender() != null ? msg.getSender().getId() : null;
                    Long receiverId = msg.getReceiver() != null ? msg.getReceiver().getId() : null;
                    
                    // 系统消息不参与归档
                    if (contactId.equals(SYSTEM_ID)) {
                        return false;
                    }
                    
                    // 只归档与特定联系人的消息，且当前用户是接收者或发送者
                    boolean isRelatedToContact = (senderId != null && senderId.equals(contactId) && receiverId != null && receiverId.equals(userId)) ||
                                                  (senderId != null && senderId.equals(userId) && receiverId != null && receiverId.equals(contactId));
                    
                    if (isRelatedToContact) {
                        System.out.println("匹配到消息：id=" + msg.getId() + ", sender=" + senderId + ", receiver=" + receiverId);
                    }
                    
                    return isRelatedToContact;
                })
                .collect(Collectors.toList());
        
        System.out.println("匹配到 " + contactMessages.size() + " 条消息需要归档");
        
        // 将当前用户 ID 添加到消息的 archivedByUserIds 字段
        for (Message message : contactMessages) {
            addArchivedUserId(message, userId);
        }
        
        if (!contactMessages.isEmpty()) {
            messageRepository.saveAll(contactMessages);
            System.out.println("归档完成");
        } else {
            System.out.println("没有找到需要归档的消息");
        }
    }

    /**
     * 添加用户 ID 到已归档列表
     */
    private void addArchivedUserId(Message message, Long userId) {
        String archivedIds = message.getArchivedByUserIds();
        if (archivedIds == null || archivedIds.isEmpty()) {
            message.setArchivedByUserIds(userId.toString());
        } else if (!archivedIds.contains(userId.toString())) {
            message.setArchivedByUserIds(archivedIds + "," + userId);
        }
    }

    /**
     * 检查消息是否对特定用户已归档
     */
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
        dto.setRelatedContractId(message.getRelatedContractId());
        dto.setRelatedRequestId(message.getRelatedRequestId());
        dto.setRequireAction(message.getRequireAction());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setReadAt(message.getReadAt());
        return dto;
    }

    public void notifyAdmins(String title, String content, Long contractId, Long requestId, boolean requireAction) {
        accountRepository.findByAccountType(AccountType.ADMIN).forEach(account -> {
            User adminUser = account.getUser();
            if (adminUser != null) {
                sendMessage(null, adminUser.getId(), title, content,
                        MessageType.ADMIN_NOTIFICATION, contractId, requestId, requireAction);
            }
        });
    }
}
