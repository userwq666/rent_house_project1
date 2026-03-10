package com.renthouse.controller;

import com.renthouse.dto.MessageDTO;
import com.renthouse.dto.SendMessageRequest;
import com.renthouse.enums.MessageStatus;
import com.renthouse.enums.MessageType;
import com.renthouse.service.MessageService;
import com.renthouse.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {

    @Autowired
    private MessageService messageService;

    private static final Long SYSTEM_ID = -1L;

    @GetMapping
    public ResponseEntity<?> getMyMessages() {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            List<MessageDTO> messages = messageService.getMessages(userId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadMessageCount() {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            long count = messageService.getUnreadMessageCount(userId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/contacts")
    public ResponseEntity<?> getMessageContacts() {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            List<MessageDTO> contacts = messageService.getMessageContacts(userId);
            return ResponseEntity.ok(contacts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/chat/{contactId}")
    public ResponseEntity<?> getChatMessages(@PathVariable Long contactId) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            List<MessageDTO> messages = messageService.getChatMessages(userId, contactId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            messageService.markAsRead(id, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateMessageStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            String statusStr = payload.get("status");
            MessageStatus status = MessageStatus.valueOf(statusStr);
            messageService.updateMessageStatus(id, userId, status);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            messageService.markAllAsRead(userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageRequest request) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            if (SYSTEM_ID.equals(request.getReceiverId())) {
                return ResponseEntity.badRequest().body("不能向系统发送消息");
            }
            if (request.getReceiverId() == null) {
                messageService.notifyAdmins(request.getTitle(), request.getContent(), null, null, false);
            } else {
                messageService.sendMessage(userId, request.getReceiverId(), request.getTitle(), request.getContent(),
                        MessageType.USER_CHAT, null, null, false);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<?> getAdminMessages() {
        try {
            Long accountId = AuthUtil.getCurrentAccountId();
            return ResponseEntity.ok(messageService.getAdminMessages(accountId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 归档与特定联系人的所有消息（前端隐藏，数据库保留）
     */
    @PostMapping("/archive/{contactId}")
    public ResponseEntity<?> archiveContactMessages(@PathVariable Long contactId) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            System.out.println("归档消息：userId=" + userId + ", contactId=" + contactId);
            messageService.archiveContactMessages(userId, contactId);
            System.out.println("归档成功");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("归档失败：" + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
