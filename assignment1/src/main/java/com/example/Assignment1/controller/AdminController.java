package com.example.assignment1.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicLong;

@RestController
public class AdminController {

    private final ApplicationContext springContext;
    private static final AtomicLong totalHitCount = new AtomicLong(0L);
    private final long instanceStartTime = System.currentTimeMillis();

    public AdminController(ApplicationContext springContext) {
        this.springContext = springContext;
    }
    public record UptimeResponse(long uptimeMillis, long uptimeSeconds, double serverUptimeSeconds) {}
    public record StatsResponse(long totalRequests, long requests, int activeThreads, long serverUptimeSeconds) {}

    // uptime end point
    @GetMapping({"/api/v1/uptime", "/api/v1/admin/uptime"})
    public UptimeResponse fetchUptime(){
        long activeMillis= System.currentTimeMillis() -instanceStartTime;
        long activeSecs =activeMillis/ 1000;
        return new UptimeResponse(activeMillis, activeSecs, activeSecs);
    } //easier to understand rather than usinf mapping.
    //runtime stats endpoint
    @GetMapping({"/api/v1/stats", "/api/v1/global/stats"})
    public StatsResponse fetchStats() {
        long activeSecs =(System.currentTimeMillis()- instanceStartTime) / 1000;
        long hits = totalHitCount.get();
        return new StatsResponse(hits, hits, Thread.activeCount(), activeSecs);
    }
    //Graceful shut down endpoint
    @PostMapping("/api/v1/admin/shutdown")
    public String triggerShutdown() {
        Thread shutdownWorker = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            SpringApplication.exit(springContext, () -> 0);
            System.exit(0);
        });
        
        shutdownWorker.setDaemon(false);
        shutdownWorker.start();
        
        return "Gracefully shutting down..";
    }
}
