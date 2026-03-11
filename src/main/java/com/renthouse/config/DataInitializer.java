package com.renthouse.config;

import com.renthouse.domain.Account;
import com.renthouse.enums.AccountType;
import com.renthouse.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Account admin = accountRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = new Account();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setAccountType(AccountType.ADMIN);
            admin.setDisplayName("系统管理员");
            admin.setEnabled(true);
            admin.setCanPublish(false);
            admin.setCanRent(false);
            accountRepository.save(admin);
            System.out.println("默认管理员账号已初始化：admin / 123456");
            return;
        }

        if (admin.getAccountType() != AccountType.ADMIN) {
            admin.setAccountType(AccountType.ADMIN);
            admin.setCanPublish(false);
            admin.setCanRent(false);
            if (admin.getDisplayName() == null || admin.getDisplayName().isBlank()) {
                admin.setDisplayName("系统管理员");
            }
            accountRepository.save(admin);
        }
    }
}
