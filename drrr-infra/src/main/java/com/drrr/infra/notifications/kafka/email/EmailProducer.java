package com.drrr.infra.notifications.kafka.email;

import com.drrr.core.exception.member.MemberException;
import com.drrr.core.exception.member.MemberExceptionCode;
import com.drrr.domain.alert.push.entity.PushMessage;
import com.drrr.domain.category.service.RecommendPostService;
import com.drrr.domain.member.entity.Member;
import com.drrr.domain.member.repository.MemberRepository;
import com.drrr.domain.techblogpost.entity.TechBlogPost;
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
    private String htmlBody = "";
    private final TemplateEngine templateEngine;
    private final RecommendPostService recommendPostService;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MemberRepository memberRepository;

    @Transactional
    public void sendMessage() {
        final List<Member> members = memberRepository.findAll();
        if (members.size() == 0) {
            log.error("사용자를 찾을 수 없습니다.");

            throw new MemberException(MemberExceptionCode.MEMBER_NOT_FOUND.getCode(),
                    MemberExceptionCode.MEMBER_NOT_FOUND.getMessage());
        }
        final Context context = new Context();

        members.stream().forEach(member -> {
            final List<TechBlogPost> recommendPosts = recommendPostService.recommendPosts(member.getId());

            context.setVariable("posts", recommendPosts);

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
