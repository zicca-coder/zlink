package com.zicca.zlink.framework.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
public class TimeCostAspect {

    @Around("@annotation(timeCost)")
    public Object around(ProceedingJoinPoint joinPoint, TimeCost timeCost) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long end = System.currentTimeMillis();
            String methodName = joinPoint.getSignature().toShortString();
            log.info("方法[{}]执行耗时: {} ms", methodName, (end - start));
        }
    }


}
