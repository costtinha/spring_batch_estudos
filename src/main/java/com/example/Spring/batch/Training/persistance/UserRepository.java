package com.example.Spring.batch.Training.persistance;

import com.example.Spring.batch.Training.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Integer> {
}
