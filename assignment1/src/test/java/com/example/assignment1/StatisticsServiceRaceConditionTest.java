package com.example.assignment1;

import com.example.assignment1.service.StatisticsService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
class StatisticsServiceRaceConditionTest {

    @Test
    void concurrentAddTokens_neverLosesUpdates() throws InterruptedException {
        StatisticsService stats = new StatisticsService();
        int threadCount = 100;
        int incrementsPerThread = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {for (int j= 0; j < incrementsPerThread; j++) {
                        stats.addTokens(1, 2);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        assertTrue(latch.await(10, TimeUnit.SECONDS), "all threads should finish in time");
        pool.shutdown();

        long expectedInput = (long) threadCount * incrementsPerThread;
        long expectedOutput = (long) threadCount * incrementsPerThread * 2;

        assertEquals(expectedInput, stats.getInputTokens(), "no input token updates should be lost");
        assertEquals(expectedOutput, stats.getOutputTokens(), "no output token updates should be lost");
    }
}