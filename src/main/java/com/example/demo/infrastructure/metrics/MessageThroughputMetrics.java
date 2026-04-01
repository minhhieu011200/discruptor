package com.example.demo.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MessageThroughputMetrics {
    // per-source counters for interval and per-second gauge
    private final Map<String, AtomicLong> processedInInterval = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> msgsPerSecond = new ConcurrentHashMap<>();
    private final Map<String, Counter> processedCounters = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "msg-throughput-metrics");
        t.setDaemon(true);
        return t;
    });
    private final Logger log = LoggerFactory.getLogger(MessageThroughputMetrics.class);

    public MessageThroughputMetrics(MeterRegistry registry) {
        // ensure a default 'total' metric exists
        ensureSourceRegistered("total", registry);

        // compute msgs/s every second and publish to gauges and logs
        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (Map.Entry<String, AtomicLong> e : processedInInterval.entrySet()) {
                    String source = e.getKey();
                    long count = e.getValue().getAndSet(0);
                    AtomicLong gauge = msgsPerSecond.get(source);
                    if (gauge != null) {
                        gauge.set(count);
                    }
                    log.info("messages/s (source={}) = {}", source, count);
                }
            } catch (Throwable t) {
                log.warn("Error computing messages/s", t);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void ensureSourceRegistered(String source, MeterRegistry registry) {
        processedInInterval.computeIfAbsent(source, s -> new AtomicLong());
        msgsPerSecond.computeIfAbsent(source, s -> {
            AtomicLong a = new AtomicLong();
            registry.gauge("messages.per.second", Tags.of("source", source), a);
            return a;
        });
        processedCounters.computeIfAbsent(source, s -> Counter.builder("messages.processed")
                .description("Total number of messages processed")
                .tags("source", source)
                .register(registry));
    }

    public void recordMessages(long n) {
        recordMessages(n, "total");
    }

    public void recordMessages(long n, String source) {
        if (n <= 0)
            return;
        // lazy-register meters for new sources
        processedInInterval.computeIfAbsent(source, s -> new AtomicLong()).addAndGet(n);
        Counter counter = processedCounters.get(source);
        if (counter != null) {
            counter.increment(n);
        }
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
    }
}
