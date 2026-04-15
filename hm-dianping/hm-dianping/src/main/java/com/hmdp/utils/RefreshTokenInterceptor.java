package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    StringRedisTemplate stringRedisTemplate;
    public RefreshTokenInterceptor(StringRedisTemplate redisTemplate) {
        stringRedisTemplate = redisTemplate;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        return true;
    }
}
