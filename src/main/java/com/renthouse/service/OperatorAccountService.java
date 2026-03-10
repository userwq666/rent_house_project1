package com.renthouse.service;

import com.renthouse.domain.OperatorAccount;
import com.renthouse.enums.OperatorRole;
import com.renthouse.repository.OperatorAccountRepository;
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
    private OperatorAccountRepository operatorAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<OperatorAccount> findByUsername(String username) {
        return operatorAccountRepository.findByUsername(username);
    }

    public List<OperatorAccount> getStaffList() {
        return operatorAccountRepository.findByRole(OperatorRole.STAFF);
    }

    public List<OperatorAccount> getEnabledStaffList() {
        return operatorAccountRepository.findByRoleAndEnabled(OperatorRole.STAFF, true);
    }

    public Optional<OperatorAccount> findById(Long id) {
        return operatorAccountRepository.findById(id);
    }

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Transactional
    public OperatorAccount createStaff(String username, String password, String displayName, String phone) {
        if (operatorAccountRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        OperatorAccount staff = new OperatorAccount();
        staff.setUsername(username);
        staff.setPassword(passwordEncoder.encode(password));
        staff.setRole(OperatorRole.STAFF);
        staff.setDisplayName(displayName);
        staff.setPhone(phone);
        staff.setEnabled(true);
        return operatorAccountRepository.save(staff);
    }

    @Transactional
    public OperatorAccount updateEnabled(Long id, Boolean enabled) {
        if (enabled == null) {
            throw new RuntimeException("enabled 不能为空");
        }
        OperatorAccount staff = operatorAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("业务员不存在"));
        staff.setEnabled(enabled);
        return operatorAccountRepository.save(staff);
    }

    public OperatorAccount requireAdmin(Long operatorId) {
        OperatorAccount operator = operatorAccountRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("操作员不存在"));
        if (operator.getRole() != OperatorRole.ADMIN) {
            throw new RuntimeException("权限不足");
        }
        return operator;
    }

    public OperatorAccount pickRandomEnabledStaff() {
        List<OperatorAccount> staffs = getEnabledStaffList();
        if (staffs.isEmpty()) {
            throw new RuntimeException("当前没有可分配的业务员");
        }
        return staffs.get(ThreadLocalRandom.current().nextInt(staffs.size()));
    }
}
