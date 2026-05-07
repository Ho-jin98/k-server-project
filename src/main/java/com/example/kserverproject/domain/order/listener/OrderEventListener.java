package com.example.kserverproject.domain.order.listener;

import com.example.kserverproject.domain.order.dto.event.OrderCreatedEvent;
import com.example.kserverproject.domain.order.producer.OrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderProducer orderProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishOrderCreated(OrderCreatedEvent orderCreatedEvent) {
        try{
            orderProducer.send(orderCreatedEvent.payload());
            log.info("[OrderEvent] 주문 생성 이벤트 발행 완료: orderId = {}",
                    orderCreatedEvent.payload().orderId());
        } catch (Exception e){
            // 발행 실패시 로그만 남김
            // 운영 환경에서는 알림/Outbox로 보강 가능여부 고려
            log.error("[OrderEvent] Kafka 이벤트 발행 실패: orderId = {}, error = {}",
                    orderCreatedEvent.payload().orderId(), e.getMessage(), e);
        }
    }
}
