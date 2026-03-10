package com.renthouse.config;

import com.renthouse.domain.Account;
import com.renthouse.domain.User;
import com.renthouse.enums.AccountType;
import com.renthouse.repository.AccountRepository;
import com.renthouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化配置：启动时自动初始化管理员账号
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=================================");
        System.out.println("开始初始化系统数据...");
        
        // 检查 admin 管理员是否已存在
        if (accountRepository.findByUsername("admin").isEmpty()) {
            // 创建账号
            Account account = new Account();
            account.setUsername("admin");
            account.setPassword(passwordEncoder.encode("123456"));
            account.setAccountType(AccountType.ADMIN);
            account.setEnabled(true);
            account.setCanPublish(true);
            account.setCanRent(true);
            Account savedAccount = accountRepository.save(account);

            // 创建用户信息
            User user = new User();
            user.setAccount(savedAccount);
            user.setRealName("系统管理员");
            user.setPhone("13800138000");
            user.setEmail("admin@renthouse.com");
            userRepository.save(user);

            System.out.println("=================================");
            System.out.println("管理员账号初始化成功！");
            System.out.println("用户名：admin");
            System.out.println("密码：123456");
            System.out.println("请及时修改默认密码！");
            System.out.println("=================================");
        } else {
            System.out.println("管理员账号已存在，跳过创建");
            System.out.println("=================================");
        }
    }
}
