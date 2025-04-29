package com.example.Spring.batch.Training.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfigCheck {

    @Value("${SPRING_REDIS_HOST}")
    private String redisHost;

    @Value("${SPRING_REDIS_PORT}")
    private String redisPort;

    @Bean
    public CommandLineRunner checkRedisConfig() {
        return args -> {
            System.out.println("Redis Host configurado: " + redisHost);
            System.out.println("Redis Port configurado: " + redisPort);
        };
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(Integer.parseInt(redisPort));
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }
}
