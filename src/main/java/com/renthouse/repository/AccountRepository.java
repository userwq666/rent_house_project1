package com.renthouse.repository;

import com.renthouse.domain.Account;
import com.renthouse.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 账号Repository
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    Optional<Account> findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    List<Account> findByAccountType(AccountType accountType);
    
    List<Account> findByEnabled(Boolean enabled);
}
