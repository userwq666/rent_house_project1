package com.renthouse.domain;

import com.renthouse.enums.AccountType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(name = "display_name", length = 80)
    private String displayName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "id_card", length = 20)
    private String idCard;

    @Column(length = 255)
    private String avatar;

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
}
