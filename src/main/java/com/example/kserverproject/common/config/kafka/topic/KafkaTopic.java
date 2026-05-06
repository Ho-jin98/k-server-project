package com.example.kserverproject.common.config.kafka.topic;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopic {

    public static final String TOPIC_ORDER = "order";

    @Bean
    public NewTopic orderTopic() {
        return TopicBuilder.name(TOPIC_ORDER)
                .partitions(3)
                .replicas(3)
                .build();
    }
}
