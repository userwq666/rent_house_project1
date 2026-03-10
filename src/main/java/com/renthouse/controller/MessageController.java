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

    private PrincipalContext resolvePrincipal() {
        try {
            return new PrincipalContext(AuthUtil.getCurrentUserId(), null);
        } catch (Exception ignored) {
            return new PrincipalContext(null, AuthUtil.getCurrentOperatorId());
        }
    }

    private static class PrincipalContext {
        private final Long userId;
        private final Long operatorId;

        private PrincipalContext(Long userId, Long operatorId) {
            this.userId = userId;
            this.operatorId = operatorId;
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyMessages() {
        try {
            PrincipalContext principal = resolvePrincipal();
            if (principal.userId != null) {
                return ResponseEntity.ok(messageService.getMessages(principal.userId));
            }
            return ResponseEntity.ok(messageService.getOperatorMessages(principal.operatorId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadMessageCount() {
        try {
            PrincipalContext principal = resolvePrincipal();
            if (principal.userId != null) {
                return ResponseEntity.ok(messageService.getUnreadMessageCount(principal.userId));
            }
            return ResponseEntity.ok(messageService.getOperatorUnreadMessageCount(principal.operatorId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/contacts")
    public ResponseEntity<?> getMessageContacts() {
        try {
            PrincipalContext principal = resolvePrincipal();
            if (principal.userId != null) {
                return ResponseEntity.ok(messageService.getMessageContacts(principal.userId));
            }
            return ResponseEntity.ok(messageService.getOperatorMessageContacts(principal.operatorId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/chat/{contactId}")
    public ResponseEntity<?> getChatMessages(@PathVariable Long contactId,
                                             @RequestParam(required = false) String contactType) {
        try {
            PrincipalContext principal = resolvePrincipal();
            if (principal.userId != null) {
                return ResponseEntity.ok(messageService.getChatMessages(principal.userId, contactId, contactType));
            }
            return ResponseEntity.ok(messageService.getOperatorChatMessages(principal.operatorId, contactId, contactType));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            PrincipalContext principal = resolvePrincipal();
            messageService.markAsRead(id, principal.userId, principal.operatorId);
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
            PrincipalContext principal = resolvePrincipal();
            messageService.updateMessageStatus(id, principal.userId, principal.operatorId, status);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        try {
            PrincipalContext principal = resolvePrincipal();
            if (principal.userId != null) {
                messageService.markAllAsRead(principal.userId);
            } else {
                messageService.markAllOperatorMessagesAsRead(principal.operatorId);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageRequest request) {
        try {
            PrincipalContext principal = resolvePrincipal();
            Long receiverId = request.getReceiverId();
            Long receiverOperatorId = request.getReceiverOperatorId();
            if (receiverId == null && receiverOperatorId == null) {
                return ResponseEntity.badRequest().body("接收方不能为空");
            }

            if (principal.userId != null) {
                if (receiverOperatorId != null) {
                    messageService.sendUserToOperatorMessage(
                            principal.userId,
                            receiverOperatorId,
                            request.getTitle(),
                            request.getContent(),
                            MessageType.USER_CHAT,
                            null,
                            null,
                            false
                    );
                } else {
                    if (SYSTEM_ID.equals(receiverId)) {
                        return ResponseEntity.badRequest().body("不能向系统发送消息");
                    }
                    messageService.sendMessage(
                            principal.userId,
                            receiverId,
                            request.getTitle(),
                            request.getContent(),
                            MessageType.USER_CHAT,
                            null,
                            null,
                            false
                    );
                }
                return ResponseEntity.ok().build();
            }

            if (receiverOperatorId != null) {
                if (SYSTEM_ID.equals(receiverOperatorId)) {
                    return ResponseEntity.badRequest().body("不能向系统发送消息");
                }
                messageService.sendOperatorMessage(
                        principal.operatorId,
                        receiverOperatorId,
                        request.getTitle(),
                        request.getContent(),
                        MessageType.USER_CHAT,
                        null,
                        null,
                        false
                );
            } else {
                if (SYSTEM_ID.equals(receiverId)) {
                    return ResponseEntity.badRequest().body("不能向系统发送消息");
                }
                messageService.sendOperatorToUserMessage(
                        principal.operatorId,
                        receiverId,
                        request.getTitle(),
                        request.getContent(),
                        MessageType.USER_CHAT,
                        null,
                        null,
                        false
                );
            }
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
    public ResponseEntity<?> archiveContactMessages(@PathVariable Long contactId,
                                                    @RequestParam(required = false) String contactType) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            messageService.archiveContactMessages(userId, contactId, contactType);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
