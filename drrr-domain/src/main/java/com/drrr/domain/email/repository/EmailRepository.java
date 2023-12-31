package com.drrr.domain.email.repository;

import com.drrr.domain.email.entity.Email;
import com.drrr.domain.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailRepository extends JpaRepository<Email, Long> {
    Optional<Email> findByProviderId(String providerId);
    void deleteByProviderId(String providerId);
}
