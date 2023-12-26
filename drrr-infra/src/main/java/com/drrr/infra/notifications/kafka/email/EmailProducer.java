package com.drrr.infra.notifications.kafka.email;

import com.drrr.core.exception.member.MemberExceptionCode;
import com.drrr.domain.category.service.RecommendPostService;
import com.drrr.domain.member.entity.Member;
import com.drrr.domain.member.repository.MemberRepository;
import com.drrr.domain.techblogpost.entity.TechBlogPost;
import com.drrr.domain.techblogpost.service.TechBlogPostService;
import com.drrr.infra.push.entity.PushMessage;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailProducer {
    private final String SUBJECT_CONTENT = "님이 관심 있을 만한 블로그를 추천해봤어요.";
    private final TemplateEngine templateEngine;
    private final RecommendPostService recommendPostService;
    private final TechBlogPostService techBlogPostService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MemberRepository memberRepository;
    private String htmlBody = "";

    @Transactional
    public void sendVerificationMessage(String email, String verificationCode) {
        final PushMessage emailMessage = PushMessage.builder()
                .to(email)
                .subject("이메일 인증 제목")
                .body(verificationCode)
                .build();
        this.kafkaTemplate.send("verification-email", emailMessage);
    }

    @Transactional
    public void sendRecommendationMessage() {
        final List<Member> members = memberRepository.findAll();
        if (members.isEmpty()) {
            log.error("사용자를 찾을 수 없습니다.");

            throw MemberExceptionCode.MEMBER_NOT_FOUND.newInstance();
        }
        final Context context = new Context();

        //limit은 테스트용 실제로는 제거해야 함
        members.stream().limit(1).forEach(member -> {
            final List<Long> recommendPostIds = recommendPostService.recommendPosts(member.getId());
            final List<TechBlogPost> posts = techBlogPostService.findTechBlogPostsByIds(recommendPostIds);

            context.setVariable("posts", posts);

            try {
                htmlBody = loadAndRenderIndexHtml(context);
            } catch (IOException e) {
                log.error("[HTML Render Error]");
                log.error("Occurred in EmailProducer.sendMessage");
                throw new RuntimeException(e);
            }

            final PushMessage emailMessage = PushMessage.builder()
                    .to(member.getEmail())
                    .subject(member.getNickname() + SUBJECT_CONTENT)
                    .body(htmlBody)
                    .build();

            this.kafkaTemplate.send("alarm-email", emailMessage);
        });

    }

    private String loadAndRenderIndexHtml(final Context context) throws IOException {
        return templateEngine.process("email_body", context); // Render the template with context
    }
}
