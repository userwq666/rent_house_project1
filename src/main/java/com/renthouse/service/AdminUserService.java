package com.renthouse.service;

import com.renthouse.domain.Account;
import com.renthouse.domain.User;
import com.renthouse.dto.UpdateAccountStatusRequest;
import com.renthouse.dto.UpdateUserRestrictionRequest;
import com.renthouse.dto.UserAdminResponse;
import com.renthouse.repository.AccountRepository;
import com.renthouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理后台-用户管理服务
 */
@Service
public class AdminUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    public List<UserAdminResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    public UserAdminResponse updateRestrictions(Long userId, UpdateUserRestrictionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Account account = user.getAccount();
        if (request.getCanPublish() != null) {
            account.setCanPublish(request.getCanPublish());
        }
        if (request.getCanRent() != null) {
            account.setCanRent(request.getCanRent());
        }
        accountRepository.save(account);
        return convert(user);
    }

    public UserAdminResponse updateStatus(Long userId, UpdateAccountStatusRequest request) {
        if (request.getEnabled() == null) {
            throw new RuntimeException("状态不能为空");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Account account = user.getAccount();
        account.setEnabled(request.getEnabled());
        accountRepository.save(account);
        return convert(user);
    }

    private UserAdminResponse convert(User user) {
        Account account = user.getAccount();
        UserAdminResponse resp = new UserAdminResponse();
        resp.setUserId(user.getId());
        resp.setAccountId(account != null ? account.getId() : null);
        resp.setUsername(account != null ? account.getUsername() : null);
        resp.setRealName(user.getRealName());
        resp.setPhone(user.getPhone());
        resp.setEmail(user.getEmail());
        resp.setAccountType(account != null && account.getAccountType() != null
                ? account.getAccountType().name() : null);
        resp.setEnabled(account != null ? account.getEnabled() : null);
        resp.setCanPublish(account != null ? resolveFlag(account.getCanPublish()) : true);
        resp.setCanRent(account != null ? resolveFlag(account.getCanRent()) : true);
        resp.setCreatedAt(user.getCreatedAt());
        return resp;
    }

    private boolean resolveFlag(Boolean value) {
        return value == null || Boolean.TRUE.equals(value);
    }
}
