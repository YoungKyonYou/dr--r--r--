package com.drrr.infra.notifications.kafka.webpush;

import com.drrr.infra.notifications.kafka.webpush.dto.NotificationDto;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@PropertySource(value = {"classpath:security-storage-api/notification/web-push/web-push-vapid.properties"})
public class WebPushConsumer {
    private final String publicKey;
    private final String privateKey;

    public WebPushConsumer(@Value("${vapid.public.pub}") final String publicKey,
                           @Value("${vapid.private.key}") final String privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    @KafkaListener(topics = "alarm-web-push", groupId = "group_web_push", containerFactory = "kafkaWebPushListenerContainerFactory")
    public void consume(final NotificationDto notificationDto) {
        PushService pushService;
        try {
            pushService = new PushService(publicKey, privateKey);

            final Notification notification = Notification.builder()
                    .endpoint(notificationDto.endPoint())
                    .userAuth(notificationDto.auth())
                    .userPublicKey(notificationDto.p245dh())
                    .payload(notificationDto.payload().getBytes())
                    .build();
            pushService.send(notification);
        } catch (GeneralSecurityException e) {
            log.error("[Web Push Key Error Failed Error]");
            log.error("Member p245dh: " + notificationDto.p245dh());
            throw new RuntimeException(e);
        } catch (IOException | JoseException | ExecutionException | InterruptedException e) {
            log.error("[Web Push Failed Error]");
            log.error("Web Push Send Error");
            log.error("Member EndPoint: " + notificationDto.endPoint());
            throw new RuntimeException(e);
        }

        log.info("Success");
    }
}
