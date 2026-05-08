package com.example.kserverproject.common.config.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

//    private GenericJackson2JsonRedisSerializer jsonSerializer() {
//        ObjectMapper objectMapper = new ObjectMapper()
//                .registerModule(new JavaTimeModule())
//                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//
//        objectMapper.activateDefaultTyping(
//                LaissezFaireSubTypeValidator.instance,
//                ObjectMapper.DefaultTyping.NON_FINAL,
//                JsonTypeInfo.As.PROPERTY
//        );
//
//        return new GenericJackson2JsonRedisSerializer(objectMapper);
//    }

    // Redisson 클라이언트
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort);
        return Redisson.create(config);
    }

//    // RedisTemplate (Sorted Set)
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
//
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//
//        template.setConnectionFactory(redisConnectionFactory);
//
//        GenericJackson2JsonRedisSerializer serializer = jsonSerializer();
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setValueSerializer(serializer);
//        template.setHashKeySerializer(new StringRedisSerializer());
//        template.setHashValueSerializer(serializer);
//
//        return template;
//    }

    // CacheManager(@Cacheable용)
//    @Bean
//    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
//
//        GenericJackson2JsonRedisSerializer serializer = jsonSerializer();
//
//        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
//                .entryTtl(Duration.ofMinutes(30))
//                .serializeKeysWith(
//                        RedisSerializationContext.SerializationPair
//                                .fromSerializer(new StringRedisSerializer())
//                )
//                .serializeValuesWith(
//                        RedisSerializationContext.SerializationPair
//                                .fromSerializer(serializer)
//                )
//                .disableCachingNullValues();
//
//        return RedisCacheManager.builder(redisConnectionFactory)
//                .cacheDefaults(config)
//                .build();
//    }
}
