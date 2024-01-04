package com.drrr.infra.notifications.kafka.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaProducerConfig {
    private final String producerHost;
    //

    public KafkaProducerConfig(@Value("${spring.kafka.producer.bootstrap-servers}") final String producerHost) {
        this.producerHost = producerHost;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        final Map<String, Object> prop = new HashMap<>();
        //Kafka 서버의 위치를 설정
        prop.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerHost);
        //Kafka message key에 대한 직렬화 클래스로 JsonSerializer를 지정
        prop.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        //Kafka message value에 대한 직렬화 클래스로 JsonSerializer를 지정
        prop.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(prop);
    }


}