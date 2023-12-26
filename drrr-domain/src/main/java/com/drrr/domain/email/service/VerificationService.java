package com.drrr.domain.email.service;

import com.drrr.core.exception.email.EmailExceptionCode;
import com.drrr.domain.email.entity.Email;
import com.drrr.domain.email.repository.EmailRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
@Slf4j
@Service
public class VerificationService {
    private final EmailRepository emailRepository;

    public String createVerificationCode(final String providerId, final String email) {
        final Optional<Email> byProviderId = emailRepository.findByProviderId(providerId);

        if (byProviderId.isPresent()) {
            emailRepository.deleteByProviderId(providerId);
        }

        final UUID uuid = UUID.randomUUID();
        final String code = uuid.toString().replace("-", "").substring(0, 6);
        final Email emailEntity = Email.builder()
                .email(email)
                .verificationCode(code)
                .isVerified(false)
                .providerId(providerId)
                .build();
        emailRepository.save(emailEntity);
        return code;
    }

    public VerificationDto verifyCode(final String providerId, final String verificationCode) {
        final Email email = emailRepository.findByProviderId(providerId).orElseThrow(() -> {
            log.error("이메일 인증 정보를 찾을 수 없습니다.");
            log.error("providerId -> " + providerId);
            return EmailExceptionCode.EMAIL_VERIFICATION_INFORMATION_NOT_FOUND.newInstance();
        });

        final Duration duration = Duration.between(email.getCreatedAt(), LocalDateTime.now());
        final long differenceInSeconds = duration.toSeconds();
        boolean isVerified = true;

        if (differenceInSeconds > 180) {
            emailRepository.deleteByProviderId(providerId);

            log.error("이메일 인증 정보가 만료되었습니다.");
            log.error("providerId -> " + providerId);
            throw EmailExceptionCode.EMAIL_VERIFICATION_CODE_EXPIRED.newInstance();
        }

        //인증번호가 만들어진 이후부터 3분 이내에 입력
        if (!email.getVerificationCode().equals(verificationCode)) {
            isVerified = false;
        }

        final VerificationDto verificationDto = VerificationDto.builder()
                .isVerified(isVerified)
                .providerId(providerId)
                .build();

        if (isVerified) {
            emailRepository.deleteByProviderId(providerId);
        }

        return verificationDto;

    }

    @Builder
    public record VerificationDto(
            String providerId,
            boolean isVerified
    ) {

    }
}
