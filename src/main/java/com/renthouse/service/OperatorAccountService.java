package com.renthouse.service;

import com.renthouse.domain.Account;
import com.renthouse.enums.AccountType;
import com.renthouse.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OperatorAccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<Account> findByUsername(String username) {
        return accountRepository.findByUsernameAndAccountTypeIn(username, List.of(AccountType.ADMIN, AccountType.STAFF));
    }

    public List<Account> getStaffList() {
        return accountRepository.findByAccountType(AccountType.STAFF);
    }

    public List<Account> getEnabledStaffList() {
        return accountRepository.findByAccountTypeAndEnabled(AccountType.STAFF, true);
    }

    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id)
                .filter(account -> account.getAccountType() == AccountType.ADMIN || account.getAccountType() == AccountType.STAFF);
    }

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Transactional
    public Account createStaff(String username, String password, String displayName, String phone) {
        if (accountRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        Account staff = new Account();
        staff.setUsername(username);
        staff.setPassword(passwordEncoder.encode(password));
        staff.setAccountType(AccountType.STAFF);
        staff.setRealName(displayName);
        staff.setDisplayName(displayName);
        staff.setPhone(phone);
        staff.setEnabled(true);
        staff.setCanPublish(false);
        staff.setCanRent(false);
        return accountRepository.save(staff);
    }

    @Transactional
    public Account updateEnabled(Long id, Boolean enabled) {
        if (enabled == null) {
            throw new RuntimeException("enabled 不能为空");
        }
        Account staff = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("业务员不存在"));
        if (staff.getAccountType() != AccountType.STAFF) {
            throw new RuntimeException("仅可修改业务员状态");
        }
        staff.setEnabled(enabled);
        return accountRepository.save(staff);
    }

    public Account requireAdmin(Long operatorId) {
        Account operator = accountRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("操作员不存在"));
        if (operator.getAccountType() != AccountType.ADMIN) {
            throw new RuntimeException("权限不足");
        }
        return operator;
    }

    public Account pickRandomEnabledStaff() {
        List<Account> staffs = getEnabledStaffList();
        if (staffs.isEmpty()) {
            throw new RuntimeException("当前没有可分配的业务员");
        }
        return staffs.get(ThreadLocalRandom.current().nextInt(staffs.size()));
    }
}
