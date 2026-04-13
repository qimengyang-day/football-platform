package com.football.utils;


import com.football.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 比赛状态管理工具类
 * 实现智能缓存策略和状态自动计算
 */
@Component
public class MatchStatusUtil {

    @Autowired
    private RedisUtil redisUtil;

    // 内存缓存：用于高并发降级
    private static final ConcurrentHashMap<String, Object> memoryCache = new ConcurrentHashMap<>();

    /**
     * 计算比赛状态
     * @param startTime 比赛开始时间
     * @return 状态值：REGISTERING(待比赛), ONGOING(进行中), ENDED(已结束)
     */
    public String calculateMatchStatus(Date startTime) {
        if (startTime == null) {
            return "REGISTERING";
        }

        Date now = new Date();
        long diff = now.getTime() - startTime.getTime();

        if (diff < 0) {
            // 比赛时间 > 当前时间：待比赛
            return "REGISTERING";
        } else if (diff < 90 * 60 * 1000) {
            // 比赛时间 <= 当前时间 且 比赛时间 > 当前时间-90分钟：进行中
            return "ONGOING";
        } else {
            // 比赛时间 <= 当前时间-90分钟：已结束
            return "ENDED";
        }
    }

    /**
     * 判断是否可以录入比分
     * @param startTime 比赛开始时间
     * @return true=可以录入, false=不可录入
     */
    public boolean canInputScore(Date startTime) {
        if (startTime == null) {
            return false;
        }
        String status = calculateMatchStatus(startTime);
        return !"REGISTERING".equals(status);
    }

    /**
     * 获取比赛状态（带缓存）
     * @param matchId 比赛ID
     * @param startTime 比赛开始时间
     * @return 状态值
     */
    public String getMatchStatusWithCache(Long matchId, Date startTime) {
        if (startTime == null) {
            return "REGISTERING";
        }

        // 计算场景类型
        ScenarioType scenario = getScenarioType(startTime);
        String cacheKey = "match:status:" + matchId;

        // 根据场景选择缓存策略
        try {
            // 比赛进行中：无缓存实时计算
            if (scenario == ScenarioType.ONGOING) {
                System.out.println("比赛进行中，实时计算状态: matchId=" + matchId);
                return calculateMatchStatus(startTime);
            }

            // 其他场景：使用缓存
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                System.out.println("从缓存获取比赛状态: matchId=" + matchId + ", scenario=" + scenario);
                return (String) cached;
            }
        } catch (Exception e) {
            System.err.println("Redis 缓存读取失败，降级到实时计算: " + e.getMessage());
        }

        // 缓存未命中或 Redis 失败：实时计算
        String status = calculateMatchStatus(startTime);

        // 写入缓存（根据场景设置不同 TTL）
        try {
            int ttl = getCacheTTL(scenario);
            redisUtil.set(cacheKey, status, ttl);
            System.out.println("比赛状态已缓存: matchId=" + matchId + ", status=" + status + ", TTL=" + ttl + "秒");
        } catch (Exception e) {
            System.err.println("Redis 缓存写入失败，不影响业务: " + e.getMessage());
            // 高并发降级：写入内存缓存
            memoryCache.put(cacheKey, status);
        }

        return status;
    }

    /**
     * 清除比赛状态缓存
     * @param matchId 比赛ID
     */
    public void clearMatchStatusCache(Long matchId) {
        try {
            String cacheKey = "match:status:" + matchId;
            redisUtil.delete(cacheKey);
            memoryCache.remove(cacheKey);
            System.out.println("已清除比赛状态缓存: matchId=" + matchId);
        } catch (Exception e) {
            System.err.println("清除缓存失败: " + e.getMessage());
        }
    }

    /**
     * 获取场景类型
     */
    private ScenarioType getScenarioType(Date startTime) {
        Date now = new Date();
        long diff = now.getTime() - startTime.getTime();

        if (diff < 0) {
            return ScenarioType.UPCOMING;
        } else if (diff < 90 * 60 * 1000) {
            return ScenarioType.ONGOING;
        } else if (diff < 2 * 60 * 60 * 1000) {
            return ScenarioType.JUST_ENDED;
        } else {
            return ScenarioType.LONG_ENDED;
        }
    }

    /**
     * 获取缓存 TTL（秒）
     */
    private int getCacheTTL(ScenarioType scenario) {
        switch (scenario) {
            case UPCOMING:
                return 1500; // 25分钟
            case ONGOING:
                return 0; // 无缓存，实时计算
            case JUST_ENDED:
                return 180; // 3分钟
            case LONG_ENDED:
                return 3300; // 55分钟
            default:
                return 300; // 默认5分钟
        }
    }

    /**
     * 枚举：场景类型
     */
    private enum ScenarioType {
        UPCOMING,      // 待比赛
        ONGOING,       // 进行中
        JUST_ENDED,    // 刚结束（2小时内）
        LONG_ENDED     // 已结束超过2小时
    }
}
