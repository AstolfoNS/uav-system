package com.tf.backend.core.common.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TokenBucketRateLimiter implements AutoCloseable {

    private final int maxTokens;

    private final AtomicInteger tokens;

    private final ScheduledExecutorService scheduler;


    public TokenBucketRateLimiter(int maxTokens, long refillIntervalMs, String threadName) {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be > 0");
        }
        if (refillIntervalMs <= 0) {
            throw new IllegalArgumentException("refillIntervalMs must be > 0");
        }
        this.maxTokens = maxTokens;
        this.tokens = new AtomicInteger(maxTokens);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, threadName);
            t.setDaemon(true);
            return t;
        });
        this.scheduler.scheduleAtFixedRate(this::refillOne, 0, refillIntervalMs, TimeUnit.MILLISECONDS);
    }

    private void refillOne() {
        int old;
        do {
            old = tokens.get();
            if (old >= maxTokens) return;
        } while (!tokens.compareAndSet(old, old + 1));

        synchronized (tokens) {
            tokens.notifyAll();
        }
    }

    public void acquire() throws InterruptedException {
        while (true) {
            int cur = tokens.get();
            if (cur > 0 && tokens.compareAndSet(cur, cur - 1)) {
                return;
            }
            synchronized (tokens) {
                tokens.wait(200); // 避免永久等待
            }
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
