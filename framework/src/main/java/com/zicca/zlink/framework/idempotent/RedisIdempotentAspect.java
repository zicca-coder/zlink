package com.zicca.zlink.framework.idempotent;

import com.zicca.zlink.framework.execption.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RedisIdempotentAspect {

    private final StringRedisTemplate redisTemplate;

    // 原子操作脚本：检查+设置+超时处理
    private static final String ATOMIC_SCRIPT = """
            local key = KEYS[1]
            local expire_time = tonumber(ARGV[1])
            
            local status = redis.call('HMGET', key, 'status')
            
            -- 如果不存在，设置为处理中
            if not status then
                redis.call('HMSET', key, 'status', '0')
                redis.call('EXPIRE', key, expire_time)
                return '1' -- 首次处理
            end
            
            -- 如果已完成
            if status == '1' then
                return '2' -- 已处理
            end
            
            return '0' -- 默认返回处理中
            """;

    // 标记完成脚本
    private static final String MARK_DONE_SCRIPT = """
            local key = KEYS[1]
            local expire_time = tonumber(ARGV[1])
            
            redis.call('HSET', key, 'status', '1')
            redis.call('EXPIRE', key, expire_time)
            return 'OK'
            """;

    private final DefaultRedisScript<String> atomicScript =
            new DefaultRedisScript<>(ATOMIC_SCRIPT, String.class);

    private final DefaultRedisScript<String> markDoneScript =
            new DefaultRedisScript<>(MARK_DONE_SCRIPT, String.class);

    @Around("@annotation(com.zicca.zlink.framework.idempotent.RedisIdempotent)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        RedisIdempotent annotation = getAnnotation(joinPoint);
        String uniqueKey = buildUniqueKey(annotation, joinPoint);

        // 单次原子操作检查幂等性
        long currentTime = System.currentTimeMillis() / 1000;
        String result = redisTemplate.execute(
                atomicScript,
                Collections.singletonList(uniqueKey),
                String.valueOf(annotation.timeout())
        );
        // 如果消息已完成
        if ("2".equals(result)) {
            log.info("消息已处理过: {}", uniqueKey);
            return null;
        }
        // 如果消息正在处理中，延迟投递，稍后再次消费时判断是否已经执行完
        if ("0".equals(result)) {
            log.warn("消息正在处理中: {}", uniqueKey);
            throw new ServiceException("消息正在处理中，请稍后重试");
        }

        try {
            // 执行业务逻辑
            Object businessResult = joinPoint.proceed();

            // 标记为已完成
            redisTemplate.execute(
                    markDoneScript,
                    Collections.singletonList(uniqueKey),
                    String.valueOf(annotation.timeout())
            );

            log.info("消息处理成功: {}", uniqueKey);
            return businessResult;

        } catch (Throwable e) {
            // 处理失败，删除记录允许重试
            redisTemplate.delete(uniqueKey);
            log.error("消息处理失败，已清理状态: {}", uniqueKey, e);
            throw e;
        }
    }

    private String buildUniqueKey(RedisIdempotent annotation, ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String keyValue = SpELUtil.parseKey(annotation.key(), method, joinPoint.getArgs()).toString();
        return annotation.keyPrefix() + keyValue;
    }

    private RedisIdempotent getAnnotation(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = joinPoint.getTarget().getClass().getDeclaredMethod(
                signature.getName(),
                signature.getParameterTypes()
        );
        return targetMethod.getAnnotation(RedisIdempotent.class);
    }
}
