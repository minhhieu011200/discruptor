package com.example.demo.infrastructure.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.example.demo.application.annotation.TraceLog;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class TraceLogAspect {

    @Around("@annotation(traceLog)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, TraceLog traceLog) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        String traceId = MDC.get("traceId");

        String operation = traceLog.value().isEmpty() ? methodName : traceLog.value();

        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (traceId != null) {
                log.info("[{}] traceId={} took {}ms", operation, traceId, duration);
            } else {
                log.info("[{}] took {}ms", operation, duration);
            }
        }
    }
}
