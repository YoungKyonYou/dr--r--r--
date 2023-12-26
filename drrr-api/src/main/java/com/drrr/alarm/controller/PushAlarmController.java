package com.drrr.alarm.controller;

import com.drrr.alarm.service.impl.ExternalMemberSubscriptionService;
import com.drrr.alarm.service.impl.ExternalNotificationEmailService;
import com.drrr.alarm.service.request.SubscriptionRequest;
import com.drrr.infra.notifications.kafka.webpush.WebPushProducer;
import com.drrr.infra.push.entity.PushMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PushAlarmController {
    private final ExternalNotificationEmailService externalNotificationEmailService;
    private final ExternalMemberSubscriptionService externalMemberSubscriptionService;
    private final WebPushProducer webPushProducer;

    @Operation(summary = "구독 요청 API",
            description = "호출 성공 시 사용자의 구독을 요청함")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용자의 구독 신청 완료", content = @Content(schema = @Schema(implementation = HttpStatus.class)))
    })
    @PostMapping("/subscription")
    public ResponseEntity<HttpStatus> addSubscription(@RequestBody final SubscriptionRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long memberId = Long.valueOf(authentication.getName());
        externalMemberSubscriptionService.executeSubscription(request, memberId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "구독 취소 API",
            description = "호출 성공 시 사용자의 구독을 취소함")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용자의 구독 취소 완료", content = @Content(schema = @Schema(implementation = HttpStatus.class)))
    })
    @DeleteMapping("/subscription/{memberId}")
    public ResponseEntity<HttpStatus> cancelSubscription(
            @NotNull @PathVariable(name = "memberId") final Long memberId) {
        externalMemberSubscriptionService.executeUnsubscription(memberId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "사용하지 말 것, 개인 테스트 용 Controller",
            description = "사용하지 말 것, 개인 테스트 용 Controller")
    @PostMapping("/notifications/email")
    public ResponseEntity<String> emailNotifications(@RequestBody PushMessage message) {
        webPushProducer.sendNotifications();
        // externalNotificationEmailService.execute(message);
        return ResponseEntity.ok("Email requests sent to Kafka");
    }
}
