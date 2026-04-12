package com.example.demo.infrastructure.metrics;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.example.demo.application.annotation.Measured;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Aspect
@Component
public class MetricAspect {
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    public MetricAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(measured)")
    public Object aroundMeasured(ProceedingJoinPoint pjp, Measured measured)
            throws Throwable {

        // Tạo tên metric, mặc định lấy tên method
        String metricName = measured.value().isEmpty()
                ? pjp.getSignature().getDeclaringType().getSimpleName() + "." +
                        pjp.getSignature().getName()
                : measured.value();

        // Lấy hoặc tạo Timer
        Timer timer = timers.computeIfAbsent(metricName, k -> Timer.builder("queue_handler_duration_ms")
                .tag("queue_name", metricName)
                .description(measured.description())
                .distributionStatisticExpiry(Duration.ofMinutes(5)) // Giữ thống kê trong 5 phút
                .distributionStatisticBufferLength(5) // Buffer cho histogram
                .serviceLevelObjectives(
                        Duration.ofMillis(1),
                        Duration.ofMillis(5),
                        Duration.ofMillis(10),
                        Duration.ofMillis(50),
                        Duration.ofMillis(100),
                        Duration.ofMillis(200),
                        Duration.ofMillis(500),
                        Duration.ofSeconds(1))
                .register(meterRegistry));

        // Lấy hoặc tạo Counter
        Counter counter = counters.computeIfAbsent(metricName, k -> Counter.builder("queue_handler_total")
                .tag("queue_name", metricName)
                .description(measured.description())
                .register(meterRegistry));

        long start = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            long duration = System.nanoTime() - start;
            timer.record(duration, TimeUnit.NANOSECONDS);
            counter.increment();
        }
    }
}