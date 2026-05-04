package com.example.kserverproject.common.config.redis;

import com.example.kserverproject.common.exception.BusinessException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {

    private final RedissonClient redissonClient;

    private static final long WAIT_TIME_SECONDS = 3L; // 락 획득시간 3초
    private static final long LEASE_TIME_SECONDS = 5L; // 락 점유 최대시간 5초

    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(WAIT_TIME_SECONDS, LEASE_TIME_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }

            log.info("[Redisson Lock 획득] key: {}, thread: {}", lockKey, Thread.currentThread().getName());

            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[Redisson Lock 해제], key: {}", lockKey);
            }
        }
    }
}
