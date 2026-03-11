package com.renthouse.service;

import com.renthouse.domain.Account;
import com.renthouse.dto.UpdateAccountStatusRequest;
import com.renthouse.dto.UpdateUserRestrictionRequest;
import com.renthouse.dto.UserAdminResponse;
import com.renthouse.enums.AccountType;
import com.renthouse.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserService {

    @Autowired
    private AccountRepository accountRepository;

    public List<UserAdminResponse> getAllUsers() {
        return accountRepository.findByAccountType(AccountType.USER)
                .stream()
                .map(this::convert)
                .toList();
    }

    public UserAdminResponse updateRestrictions(Long userId, UpdateUserRestrictionRequest request) {
        Account account = requireUser(userId);
        if (request.getCanPublish() != null) {
            account.setCanPublish(request.getCanPublish());
        }
        if (request.getCanRent() != null) {
            account.setCanRent(request.getCanRent());
        }
        accountRepository.save(account);
        return convert(account);
    }

    public UserAdminResponse updateStatus(Long userId, UpdateAccountStatusRequest request) {
        if (request.getEnabled() == null) {
            throw new RuntimeException("状态不能为空");
        }
        Account account = requireUser(userId);
        account.setEnabled(request.getEnabled());
        accountRepository.save(account);
        return convert(account);
    }

    private Account requireUser(Long userId) {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (account.getAccountType() != AccountType.USER) {
            throw new RuntimeException("仅普通用户可在此接口管理");
        }
        return account;
    }

    private UserAdminResponse convert(Account account) {
        UserAdminResponse resp = new UserAdminResponse();
        resp.setUserId(account.getId());
        resp.setAccountId(account.getId());
        resp.setUsername(account.getUsername());
        resp.setRealName(account.getRealName());
        resp.setPhone(account.getPhone());
        resp.setEmail(account.getEmail());
        resp.setAccountType(account.getAccountType() == null ? null : account.getAccountType().name());
        resp.setEnabled(account.getEnabled());
        resp.setCanPublish(resolveFlag(account.getCanPublish()));
        resp.setCanRent(resolveFlag(account.getCanRent()));
        resp.setCreatedAt(account.getCreatedAt());
        return resp;
    }

    private boolean resolveFlag(Boolean value) {
        return value == null || Boolean.TRUE.equals(value);
    }
}
