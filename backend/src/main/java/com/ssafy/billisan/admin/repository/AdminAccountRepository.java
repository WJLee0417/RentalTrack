package com.ssafy.billisan.admin.repository;

import com.ssafy.billisan.admin.domain.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, UUID> {
    Optional<AdminAccount> findByLoginId(String loginId);
}