package com.xinki.portfolio.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinki.portfolio.common.Result;
import com.xinki.portfolio.dto.LoginDTO;
import com.xinki.portfolio.entity.User;
import com.xinki.portfolio.mapper.UserMapper;
import com.xinki.portfolio.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(15);
    private static final String LOGIN_FAIL_PREFIX = "login:fail:";

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String failKey = LOGIN_FAIL_PREFIX + clientIp;

        String failCountStr = redisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr != null ? Integer.parseInt(failCountStr) : 0;
        if (failCount >= LOGIN_MAX_ATTEMPTS) {
            Long ttl = redisTemplate.getExpire(failKey);
            long minutes = (ttl != null && ttl > 0) ? ttl / 60 : 15;
            return Result.error(429, "登录尝试次数过多，请 " + minutes + " 分钟后再试");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, loginDTO.getUsername()));
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            failCount++;
            redisTemplate.opsForValue().set(failKey, String.valueOf(failCount), LOGIN_LOCK_DURATION);
            int remaining = LOGIN_MAX_ATTEMPTS - failCount;
            return Result.error(401, "用户名或密码错误" + (remaining > 0 ? "，还剩 " + remaining + " 次尝试" : "，账号已锁定"));
        }

        redisTemplate.delete(failKey);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        data.put("username", user.getUsername());
        return Result.success(data);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}