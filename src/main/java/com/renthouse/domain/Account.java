package com.renthouse.domain;

import com.renthouse.enums.AccountType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 系统账号实体
 * 用于登录认证和权限控制
 */
@Entity
@Table(name = "accounts")
@Data
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType = AccountType.USER;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "can_publish", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean canPublish = true;

    @Column(name = "can_rent", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean canRent = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 一对一关联用户信息
    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private User user;
}
