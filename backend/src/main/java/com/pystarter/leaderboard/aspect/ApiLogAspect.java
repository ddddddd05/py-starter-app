package com.pystarter.leaderboard.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Spring AOP 切面：统一监控 Controller 接口耗时与异常，避免在业务代码里重复写日志。
 */
@Aspect
@Component
public class ApiLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ApiLogAspect.class);

    @Around("execution(* com.pystarter.leaderboard.controller..*(..))")
    public Object logApiTiming(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("[API] {} completed in {} ms", method, cost);
            return result;
        } catch (Throwable ex) {
            long cost = System.currentTimeMillis() - start;
            log.warn("[API] {} failed in {} ms: {}", method, cost, ex.getMessage());
            throw ex;
        }
    }
}
