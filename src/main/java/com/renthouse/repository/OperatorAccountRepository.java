package com.renthouse.repository;

import com.renthouse.domain.OperatorAccount;
import com.renthouse.enums.OperatorRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorAccountRepository extends JpaRepository<OperatorAccount, Long> {
    Optional<OperatorAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    List<OperatorAccount> findByRoleAndEnabled(OperatorRole role, Boolean enabled);

    List<OperatorAccount> findByRole(OperatorRole role);
}

