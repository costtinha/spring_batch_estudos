package com.example.Spring.batch.Training.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "User", timeToLive = 3600)
public class UserCache {
    @Id
    private Integer userId;

    private String name;

    private int age;

    private String email;


    public UserCache(Integer userId, String name, int age, String email) {
        this.userId = userId;
        this.name = name;
        this.age = age;
        this.email = email;
    }

    public UserCache(String name, int age, String email) {
        this.name = name;
        this.age = age;
        this.email = email;
    }

    public UserCache() {
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
