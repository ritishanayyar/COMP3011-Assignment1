package com.example.assignment1.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

//Thread-safe counter forSTT token usage since server start.
@Service
public class StatisticsService {
    private final AtomicLong inputTokens = new AtomicLong(0);
    private final AtomicLong outputTokens = new AtomicLong(0);
    public void addTokens(long input, long output) {
        inputTokens.addAndGet(input);
        outputTokens.addAndGet(output);
    }
    
    public long getInputTokens() {
        return inputTokens.get();
    }

    public long getOutputTokens() {
        return outputTokens.get();
    }
}
