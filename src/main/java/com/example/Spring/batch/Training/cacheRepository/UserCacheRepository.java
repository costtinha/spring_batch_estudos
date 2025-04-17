package com.example.Spring.batch.Training.cacheRepository;

import com.example.Spring.batch.Training.entity.UserCache;
import org.springframework.data.repository.CrudRepository;

public interface UserCacheRepository extends CrudRepository<UserCache,Integer> {
}
