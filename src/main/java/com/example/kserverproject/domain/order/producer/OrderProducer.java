package com.example.kserverproject.domain.order.producer;

import com.example.kserverproject.domain.order.dto.event.OrderEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.example.kserverproject.common.config.kafka.topic.KafkaTopic.TOPIC_ORDER;

@Service
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, OrderEventDto>  orderEventKafkaTemplate;

    public void send(OrderEventDto orderEventDto) {
        orderEventKafkaTemplate.send(TOPIC_ORDER,  orderEventDto);
    }
}
