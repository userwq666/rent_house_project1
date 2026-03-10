package com.renthouse.config;

import com.renthouse.domain.OperatorAccount;
import com.renthouse.enums.OperatorRole;
import com.renthouse.repository.OperatorAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private OperatorAccountRepository operatorAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (operatorAccountRepository.findByUsername("admin").isEmpty()) {
            OperatorAccount admin = new OperatorAccount();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setRole(OperatorRole.ADMIN);
            admin.setDisplayName("系统管理员");
            admin.setEnabled(true);
            operatorAccountRepository.save(admin);
            System.out.println("默认管理员账号已初始化：admin / 123456");
        }
    }
}
