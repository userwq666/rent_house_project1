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
            Long accountId = AuthUtil.getCurrentAccountId();
            return ResponseEntity.ok(messageService.getMessages(accountId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadMessageCount() {
        try {
            Long accountId = AuthUtil.getCurrentAccountId();
            return ResponseEntity.ok(messageService.getUnreadMessageCount(accountId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/contacts")
    public ResponseEntity<?> getMessageContacts() {
        try {
            Long accountId = AuthUtil.getCurrentAccountId();
            return ResponseEntity.ok(messageService.getMessageContacts(accountId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/chat/{contactId}")
    public ResponseEntity<?> getChatMessages(@PathVariable Long contactId,
                                             @RequestParam(required = false) String contactType) {
        try {
            Long accountId = AuthUtil.getCurrentAccountId();
            return ResponseEntity.ok(messageService.getChatMessages(accountId, contactId, contactType));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            Long accountId = AuthUtil.getCurrentAccountId();
            messageService.markAsRead(id, accountId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateMessageStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String statusStr = payload.get("status");
            if (statusStr == null || statusStr.isBlank()) {
                return ResponseEntity.badRequest().body("状态不能为空");
            }
            MessageStatus status = MessageStatus.valueOf(statusStr);
            Long accountId = AuthUtil.getCurrentAccountId();
            messageService.updateMessageStatus(id, accountId, status);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        try {
            Long accountId = AuthUtil.getCurrentAccountId();
            messageService.markAllAsRead(accountId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageRequest request) {
        try {
            Long accountId = AuthUtil.getCurrentAccountId();
            Long receiverId = request.getReceiverId();
            if (receiverId == null) {
                return ResponseEntity.badRequest().body("接收方不能为空");
            }
            if (SYSTEM_ID.equals(receiverId)) {
                return ResponseEntity.badRequest().body("不能向系统发送消息");
            }

            messageService.sendMessage(
                    accountId,
                    receiverId,
                    request.getTitle(),
                    request.getContent(),
                    MessageType.USER_CHAT,
                    null,
                    null,
                    false
            );
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

    @PostMapping("/archive/{contactId}")
    public ResponseEntity<?> archiveContactMessages(@PathVariable Long contactId,
                                                    @RequestParam(required = false) String contactType) {
        try {
            Long accountId = AuthUtil.getCurrentAccountId();
            messageService.archiveContactMessages(accountId, contactId, contactType);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
