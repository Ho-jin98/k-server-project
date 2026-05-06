package com.example.kserverproject.common.config.kafka.consumer;

import com.example.kserverproject.domain.order.dto.event.OrderEventDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> baseConsumerProps(String groupId) {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        return props;
    }

    private ConsumerFactory<String, OrderEventDto> buildConsumerFactory(String groupId) {
        JsonDeserializer<OrderEventDto> deserializer = new JsonDeserializer<>(OrderEventDto.class);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProps(groupId),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public CommonErrorHandler commonErrorHandlerWithDLT(
            KafkaTemplate<String, OrderEventDto> orderEventKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(orderEventKafkaTemplate);

        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        return new DefaultErrorHandler(recoverer, backOff);
    }

    // 주문 기록 전용 컨슈머 그룹 생성
    @Bean
    public ConsumerFactory<String, OrderEventDto> orderConsumerFactory() {
        return buildConsumerFactory("order-group");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEventDto> orderKafkaListenerContainerFactory(
            CommonErrorHandler commonErrorHandlerWithDLT) {
        ConcurrentKafkaListenerContainerFactory<String, OrderEventDto> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(orderConsumerFactory());
        factory.setCommonErrorHandler(commonErrorHandlerWithDLT);

        return factory;
    }
}
