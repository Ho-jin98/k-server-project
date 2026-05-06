package com.example.kserverproject.domain.order.listener;

import com.example.kserverproject.common.config.kafka.topic.KafkaTopic;
import com.example.kserverproject.domain.order.dto.event.OrderEventDto;
import com.example.kserverproject.domain.order.service.OrderKafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderListener {

    private final OrderKafkaService orderKafkaService;

    @KafkaListener(
            topics = KafkaTopic.TOPIC_ORDER,
            groupId = "order-group",
            containerFactory = "orderKafkaListenerContainerFactory"
    )
    public void consume(OrderEventDto orderEventDto) {

        log.info("[Order-Consumer] 주문 이벤트 수신 : orderId{}, userId{}",
                orderEventDto.orderId(), orderEventDto.userId());

        // DB작업 먼저 진행
        orderKafkaService.processOrderCompleted(orderEventDto);
        // Redis 작업은 트랜잭션 밖에서 별도로 진행
        orderKafkaService.updatePopularMenus(orderEventDto);
    }
}
