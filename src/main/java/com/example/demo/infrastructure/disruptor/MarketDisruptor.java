package com.example.demo.infrastructure.disruptor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.service.PublishService;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

@Service
public class MarketDisruptor implements PublishService<Void, SymbolEntity> {
    private final int NUM_WORKERS = 16; // số lane / worker
    private final Disruptor<MarketEvent>[] disruptors = (Disruptor<MarketEvent>[]) new Disruptor[NUM_WORKERS];
    private final AtomicLong[] counters = new AtomicLong[NUM_WORKERS];

    public MarketDisruptor() throws Exception {
        for (int i = 0; i < NUM_WORKERS; i++) {
            final int lane = i;
            counters[i] = new AtomicLong(0);
            disruptors[i] = new Disruptor<>(
                    new MarketEvent.Factory(),
                    1024,
                    Thread.ofPlatform().name("lane-" + lane + "-").factory(),
                    ProducerType.SINGLE,
                    new BusySpinWaitStrategy());

            disruptors[i].handleEventsWith((event, seq, endOfBatch) -> {
                counters[lane].incrementAndGet();
                // format current time

                // if (seq == 1) {
                if (seq % 500 == 0) { // only log every 500th event
                    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                    System.out.println("[" + time + "] [Lane " + lane + "] " + event.getEntity());
                }
                // }
            });

            disruptors[i].start();

        }

        Thread reporter = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); // 1 second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                long total = 0;
                for (int i = 0; i < NUM_WORKERS; i++) {
                    long count = counters[i].getAndSet(0); // reset after reporting
                    total += count;
                    System.out.println("[Lane " + i + "] processed " + count + " msg/s");
                }
                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                System.out.println("Time" + time + "[Total] processed " + total + " msg/s\n");
            }
        }, "ThroughputReporter");

        reporter.setDaemon(true); // will not block JVM shutdown
        reporter.start();
    }

    // Publisher gọi: hash symbolId → lane
    public Void publish(String channel, SymbolEntity e) {
        int lane = (int) (channel.hashCode() % NUM_WORKERS);
        long seq = disruptors[lane].getRingBuffer().next();
        try {
            MarketEvent ev = disruptors[lane].getRingBuffer().get(seq);
            ev.setEntity(e);
        } finally {
            disruptors[lane].getRingBuffer().publish(seq);
        }
        return null;
    }

}
