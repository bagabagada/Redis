package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    /**
     * 开始时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1712361600L ;
    /**
     * 序列号位移位数
     */
    private static final int COUNT_BITS = 32 ;
    @Resource
    private StringRedisTemplate stringRedisTemplate ;

    public Long nextId(String keyPrefix){
        // 1 . 生成时间戳
        long nowSecond = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) ;
        long timestamp = nowSecond - BEGIN_TIMESTAMP ;

        // 2 . 生成序列号
        // 2 . 1 获取当前日期 ， 精确到天
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd")) ;
        // 2 . 2 自增长
        long count = stringRedisTemplate.opsForValue().increment("icr:"+keyPrefix+":"+date) ;

        // 3 . 拼接并返回(使用位运算)
        return timestamp <<  COUNT_BITS | count  ;
    }

    public static void main(String[] args) {
        LocalDateTime time = LocalDateTime.of(2024,4,6,0,0,0) ;
        long second = time.toEpochSecond(ZoneOffset.UTC) ;
        System.out.println("second : " + second) ;
    }

}
