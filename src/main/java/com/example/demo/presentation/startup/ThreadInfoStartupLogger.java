package com.example.demo.presentation.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In thông tin virtual thread 1 lần duy nhất khi app khởi động xong.
 * Kiểm tra cả main thread và bounded-elastic scheduler (thread xử lý business).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ThreadInfoStartupLogger implements ApplicationRunner {

    @Value("${app.debug.dump-threads:false}")
    private boolean dumpThreads;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Thread main = Thread.currentThread();
        log.info("=== Thread Diagnostics ===");
        log.info("  [main] thread={}, virtual={}", main.getName(), main.isVirtual());

        AtomicBoolean isVirtual = new AtomicBoolean();
        String[] elasticThread = { "" };
        CountDownLatch latch = new CountDownLatch(1);

        Schedulers.boundedElastic().schedule(() -> {
            Thread t = Thread.currentThread();
            isVirtual.set(t.isVirtual());
            elasticThread[0] = t.getName();
            latch.countDown();
        });

        latch.await();

        log.info("  [boundedElastic] thread={}, virtual={}", elasticThread[0], isVirtual.get());
        log.info("  Virtual threads : {}", isVirtual.get() ? "ENABLED ✓" : "DISABLED");

        if (dumpThreads) {
            dumpAllThreads();
        }

        log.info("==========================");
    }

    private void dumpAllThreads() {
        Thread.getAllStackTraces().keySet().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .forEach(t -> log.info("thread={}, virtual={}, state={}",
                        t.getName(), t.isVirtual(), t.getState()));
    }
}