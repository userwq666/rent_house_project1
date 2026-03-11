package com.renthouse.service;

import com.renthouse.domain.Account;
import com.renthouse.dto.UserRegisterRequest;
import com.renthouse.enums.AccountType;
import com.renthouse.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public Account register(UserRegisterRequest request, AccountType targetType) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setAccountType(targetType);
        account.setRealName(request.getRealName());
        account.setDisplayName(request.getRealName());
        account.setPhone(request.getPhone());
        account.setEmail(request.getEmail());
        account.setIdCard(request.getIdCard());
        account.setEnabled(true);

        if (targetType == AccountType.USER) {
            account.setCanPublish(true);
            account.setCanRent(true);
        } else {
            account.setCanPublish(false);
            account.setCanRent(false);
        }

        return accountRepository.save(account);
    }

    public Optional<Account> findByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

    public Optional<Account> findById(Long accountId) {
        return accountRepository.findById(accountId);
    }

    public List<Account> findAllUsers() {
        return accountRepository.findByAccountType(AccountType.USER);
    }

    @Transactional
    public Account updateProfile(Long accountId, String realName, String phone, String email, String idCard) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (account.getAccountType() != AccountType.USER) {
            throw new RuntimeException("仅普通用户可编辑此资料");
        }
        account.setRealName(realName);
        account.setDisplayName(realName);
        account.setPhone(phone);
        account.setEmail(email);
        account.setIdCard(idCard);
        return accountRepository.save(account);
    }

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Transactional
    public void toggleAccountStatus(Long accountId, Long adminAccountId) {
        Account operator = accountRepository.findById(adminAccountId)
                .orElseThrow(() -> new RuntimeException("操作员不存在"));
        if (operator.getAccountType() != AccountType.ADMIN) {
            throw new RuntimeException("权限不足");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        account.setEnabled(!Boolean.TRUE.equals(account.getEnabled()));
        accountRepository.save(account);
    }
}
