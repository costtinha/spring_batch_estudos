package com.example.Spring.batch.Training.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.example.Spring.batch.Training.persistance")
@EnableRedisRepositories(basePackages = "com.example.Spring.batch.Training.cacheRepository")
@EntityScan(basePackages = "com.example.Spring.batch.Training.entity")
public class RepositoryConfig {
}
