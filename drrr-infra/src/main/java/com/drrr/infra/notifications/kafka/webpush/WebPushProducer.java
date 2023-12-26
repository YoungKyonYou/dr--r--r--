package com.drrr.infra.notifications.kafka.webpush;


import com.drrr.infra.notifications.kafka.webpush.dto.NotificationDto;
import com.drrr.infra.push.entity.Subscription;
import com.drrr.infra.push.repository.SubscriptionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebPushProducer {
    private final SubscriptionRepository subscriptionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendNotifications() {
        final List<Subscription> subscriptions = subscriptionRepository.findAll();
        if (subscriptions.isEmpty()) {
            log.error("[send Notifiction Failed]");
            log.error("웹 푸시가 실패하였습니다.");
            throw new IllegalArgumentException("subscription elements is null");
        }

        subscriptions.forEach(subscription -> {
            NotificationDto notification = NotificationDto.builder()
                    .endPoint(subscription.getEndpoint())
                    .p245dh(subscription.getP256dh())
                    .payload("웹 푸시 내용이 여기에 들어갑니다.")
                    .auth(subscription.getAuth())
                    .build();
            this.kafkaTemplate.send("alarm-web-push", notification);
        });

    }
}
