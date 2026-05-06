package com.example.demo.infrastructure.aop;

import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
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
        long start = System.nanoTime();
        String methodName = joinPoint.getSignature().toShortString();
        
        // 1. Try to get from MDC first
        String traceId = MDC.get("traceId");

        // 2. If not in MDC, try to find it in method arguments
        if (traceId == null && joinPoint.getSignature() instanceof MethodSignature) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    if (("traceId".equalsIgnoreCase(parameterNames[i]) || "traceid".equalsIgnoreCase(parameterNames[i])) 
                            && args[i] instanceof String) {
                        traceId = (String) args[i];
                        break;
                    }
                }
            }
        }

        String operation = traceLog.value().isEmpty() ? methodName : traceLog.value();

        try {
            return joinPoint.proceed();
        } finally {
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            if (log.isTraceEnabled()) {
                if (traceId != null) {
                    log.trace("[{}] traceId={} took {}ms", operation, traceId, duration);
                } else {
                    log.trace("[{}] took {}ms", operation, duration);
                }
            }
        }
    }
}
