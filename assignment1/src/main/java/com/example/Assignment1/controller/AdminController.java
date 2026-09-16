package com.example.assignment1.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class AdminController {

    private final ApplicationContext context;
    private static final AtomicLong requestCount = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();
    public AdminController(ApplicationContext context) {
        this.context = context;
    }

    //Uptime Endpoint
    @GetMapping({"/api/v1/uptime", "/api/v1/admin/uptime"})
    public Map<String, Object> getUptime() {
        long uptimeMillis = System.currentTimeMillis() - startTime;
        Map<String, Object> response = new HashMap<>();
        response.put("uptimeMillis", uptimeMillis);
        response.put("uptimeSeconds", uptimeMillis / 1000);
        return response;
    }
    // Statistics Endpoint
    @GetMapping({"/api/v1/stats", "/api/v1/global/stats"})
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", requestCount.get());
        stats.put("activeThreads", Thread.activeCount());
        return stats;
    }
    //Shutdown Endpoint
    @PostMapping("/api/v1/admin/shutdown")
    public String shutdown() {
        Thread shutdownThread = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
            SpringApplication.exit(context, () -> 0);
            System.exit(0);
        });
        shutdownThread.start();
        return "Shutting down application gracefully...";
    }
}