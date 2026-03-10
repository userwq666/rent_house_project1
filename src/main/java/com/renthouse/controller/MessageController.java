package com.renthouse.controller;

import com.renthouse.dto.SendMessageRequest;
import com.renthouse.enums.MessageStatus;
import com.renthouse.enums.MessageType;
import com.renthouse.service.MessageService;
import com.renthouse.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            try {
                Long userId = AuthUtil.getCurrentUserId();
                return ResponseEntity.ok(messageService.getMessages(userId));
            } catch (Exception ignored) {
                Long operatorId = AuthUtil.getCurrentOperatorId();
                return ResponseEntity.ok(messageService.getOperatorMessages(operatorId));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadMessageCount() {
        try {
            try {
                Long userId = AuthUtil.getCurrentUserId();
                return ResponseEntity.ok(messageService.getUnreadMessageCount(userId));
            } catch (Exception ignored) {
                Long operatorId = AuthUtil.getCurrentOperatorId();
                return ResponseEntity.ok(messageService.getOperatorUnreadMessageCount(operatorId));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/contacts")
    public ResponseEntity<?> getMessageContacts() {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            return ResponseEntity.ok(messageService.getMessageContacts(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/chat/{contactId}")
    public ResponseEntity<?> getChatMessages(@PathVariable Long contactId) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            return ResponseEntity.ok(messageService.getChatMessages(userId, contactId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            try {
                Long userId = AuthUtil.getCurrentUserId();
                messageService.markAsRead(id, userId);
            } catch (Exception ignored) {
                Long operatorId = AuthUtil.getCurrentOperatorId();
                messageService.markAsRead(id, operatorId);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateMessageStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String statusStr = payload.get("status");
            MessageStatus status = MessageStatus.valueOf(statusStr);
            try {
                Long userId = AuthUtil.getCurrentUserId();
                messageService.updateMessageStatus(id, userId, status);
            } catch (Exception ignored) {
                Long operatorId = AuthUtil.getCurrentOperatorId();
                messageService.updateMessageStatus(id, operatorId, status);
            }
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
                return ResponseEntity.badRequest().body("接收方不能为空");
            }
            messageService.sendMessage(userId, request.getReceiverId(), request.getTitle(), request.getContent(),
                    MessageType.USER_CHAT, null, null, false);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<?> getAdminMessages() {
        try {
            Long operatorId = AuthUtil.getCurrentOperatorId();
            return ResponseEntity.ok(messageService.getAdminMessages(operatorId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/archive/{contactId}")
    public ResponseEntity<?> archiveContactMessages(@PathVariable Long contactId) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            messageService.archiveContactMessages(userId, contactId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
