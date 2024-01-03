package com.drrr.infra.notifications.kafka.config;


import com.drrr.infra.notifications.kafka.webpush.dto.NotificationDto;
import com.drrr.infra.push.entity.PushMessage;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class KafkaConsumerConfig {
    private final String consumerHost;
    private final KafkaConfig kafkaConfig;

    public KafkaConsumerConfig(@Value("${spring.kafka.consumer.bootstrap-servers}") final String consumerHost,
                               final KafkaConfig kafkaConfig) {
        this.consumerHost = consumerHost;
        this.kafkaConfig = kafkaConfig;
    }
//sdf
    private <T> JsonDeserializer<T> getDeserializer(final JsonDeserializer<T> deserializer) {
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);
        return deserializer;
    }

    private <T> Map<String, Object> getProp(final JsonDeserializer<T> deserializer) {
        final Map<String, Object> prop = new HashMap<>();

        prop.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerHost);
        prop.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        prop.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
        return prop;
    }

    @Bean
    public ConsumerFactory<String, PushMessage> consumerEmailFactory() {
        final JsonDeserializer<PushMessage> deserializer = getDeserializer(new JsonDeserializer<>(PushMessage.class));
        final Map<String, Object> prop = getProp(deserializer);

        return new DefaultKafkaConsumerFactory<>(
                prop,
                new StringDeserializer(),
                deserializer);
    }

    /**
     * AckMode.RECORD: In this after-processing mode, the consumer sends an acknowledgment for each message it
     * processes. AckMode.BATCH: In this manual mode, the consumer sends an acknowledgment for a batch of messages,
     * rather than for each message. AckMode.COUNT: In this manual mode, the consumer sends an acknowledgment after it
     * has processed a specific number of messages. AckMode.MANUAL: In this manual mode, the consumer doesn’t send an
     * acknowledgment for the messages it processes. AckMode.TIME: In this manual mode, the consumer sends an
     * acknowledgment after a certain amount of time has passed.
     * <p>
     * Blocking Retry를 사용하려면 RECORD 아니면 MANUAL를 사용해야 함 Blocking Retry는 메세지가 일시적인 에러로 인해서 메세지가 처음에 제대로 소비되지 않으면 다시 시도하는
     * 기능이다. consumer는 커스터마이즈를 통해서 backoff 시기나 "고정지연" 아니면 "지수 backoff" 전략을 사용할 수 있다. 그리고 Max 재시도 값을 설정함으로써 Error로 기록되기
     * 전에 그 재시도 값만큼 설정도 가능하다.
     * <p>
     * backoff는 주로 네트워크 통신이나 분산 시스템에서 재시도 간의 대기 시간을 관리하는 전략을 가리키는 용어 이는 일시적인 오류나 과부하 상황에서 시스템의 안정성을 유지하고, 자원을 효율적으로 사용하는
     * 데 도움이 됨
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PushMessage> kafkaEmailListenerContainerFactory() {
        final ConcurrentKafkaListenerContainerFactory<String, PushMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.getContainerProperties().setAckMode(AckMode.MANUAL);
        factory.setConsumerFactory(consumerEmailFactory());
        factory.setCommonErrorHandler(kafkaConfig.errorHandler());
        return factory;
    }


    @Bean
    public ConsumerFactory<String, NotificationDto> consumerWebPushFactory() {
        final JsonDeserializer<NotificationDto> deserializer = getDeserializer(
                new JsonDeserializer<>(NotificationDto.class));
        final Map<String, Object> prop = getProp(deserializer);

        return new DefaultKafkaConsumerFactory<>(
                prop,
                new StringDeserializer(),
                deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationDto> kafkaWebPushListenerContainerFactory() {
        final ConcurrentKafkaListenerContainerFactory<String, NotificationDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.getContainerProperties().setAckMode(AckMode.MANUAL);
        factory.setConsumerFactory(consumerWebPushFactory());
        factory.setCommonErrorHandler(kafkaConfig.errorHandler());
        return factory;
    }
}
