package com.renthouse.service;

import com.renthouse.domain.Account;
import com.renthouse.domain.User;
import com.renthouse.dto.UserRegisterRequest;
import com.renthouse.enums.AccountType;
import com.renthouse.repository.AccountRepository;
import com.renthouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 账号 Service
 */
@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户注册
     */
    @Transactional
    public User register(UserRegisterRequest request) {
        // 检查用户名是否已存在
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建账号
        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setAccountType(AccountType.USER);
        account.setEnabled(true);
        account = accountRepository.save(account);

        // 创建用户信息
        User user = new User();
        user.setAccount(account);
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setIdCard(request.getIdCard());
        // 维护双向关系，确保级联与外键一致
        account.setUser(user);

        return userRepository.save(user);
    }

    /**
     * 根据用户名查找账号
     */
    public Optional<Account> findByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

    /**
     * 根据账号ID查找用户信息
     */
    public Optional<User> findUserByAccountId(Long accountId) {
        return userRepository.findByAccountId(accountId);
    }

    public Optional<User> findUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * 更新用户基本信息
     */
    @Transactional
    public User updateUserProfile(Long userId, String realName, String phone, String email, String idCard) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setIdCard(idCard);
        return userRepository.save(user);
    }

    /**
     * 校验密码
     */
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 启用/禁用账号（仅管理员）
     */
    @Transactional
    public void toggleAccountStatus(Long accountId, Long operatorAccountId) {
        Account operator = accountRepository.findById(operatorAccountId)
                .orElseThrow(() -> new RuntimeException("操作者不存在"));

        if (operator.getAccountType() != AccountType.ADMIN) {
            throw new RuntimeException("权限不足");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        account.setEnabled(!account.getEnabled());
        accountRepository.save(account);
    }
}
