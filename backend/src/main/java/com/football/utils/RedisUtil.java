package com.football.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存（默认不过期）
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            System.err.println("Redis SET 失败 [key=" + key + "]: " + e.getMessage());
        }
    }

    /**
     * 设置缓存并指定过期时间（秒）
     */
    public void set(String key, Object value, long timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Redis SET 失败 [key=" + key + ", timeout=" + timeout + "s]: " + e.getMessage());
        }
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            System.err.println("Redis GET 失败 [key=" + key + "]: " + e.getMessage());
            return null;
        }
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            System.err.println("Redis DELETE 失败 [key=" + key + "]: " + e.getMessage());
        }
    }

    /**
     * 批量删除缓存（支持通配符）
     */
    public void deleteByPattern(String pattern) {
        try {
            redisTemplate.delete(redisTemplate.keys(pattern));
        } catch (Exception e) {
            System.err.println("Redis DELETE BY PATTERN 失败 [pattern=" + pattern + "]: " + e.getMessage());
        }
    }

    /**
     * 判断 key 是否存在
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            System.err.println("Redis EXISTS 失败 [key=" + key + "]: " + e.getMessage());
            return false;
        }
    }

    /**
     * 递增
     */
    public void increment(String key) {
        try {
            redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            System.err.println("Redis INCREMENT 失败 [key=" + key + "]: " + e.getMessage());
        }
    }

    /**
     * 递增指定值
     */
    public void increment(String key, long delta) {
        try {
            redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            System.err.println("Redis INCREMENT 失败 [key=" + key + ", delta=" + delta + "]: " + e.getMessage());
        }
    }
}