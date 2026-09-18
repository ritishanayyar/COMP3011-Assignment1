package com.example.assignment1.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.assignment1.service.StatisticsService;

import java.util.concurrent.atomic.AtomicLong;
import java.time.Duration;
import java.time.Instant;

@RestController
public class AdminController {

    private final ApplicationContext springContext;
    private final Instant serverStartTime=Instant.now();
    private final StatisticsService statisticsService;//getting our inputs and output tokens

    public AdminController(ApplicationContext springContext, StatisticsService statisticsService) {
        this.springContext = springContext;
        this.statisticsService = statisticsService;
    }
    public record UptimeResponse(
        String utcServerStart,
        String utcNow,
        double serverUptimeSeconds
    ){}
    public record GlobalStatsResponse(long inputTokens, long outputTokens) {}

    // uptime end point
    @GetMapping({"/api/v1/uptime", "/api/v1/admin/uptime"})
    public UptimeResponse fetchUptime(){
        Instant now = Instant.now();
        double uptimeSeconds= Duration.between (serverStartTime, now).toNanos()/1_000_000_000.0;
        return new UptimeResponse(
            serverStartTime.toString(),
            now.toString(),
            uptimeSeconds

        );  } //easier to understand rather than usinf mapping.
    //runtime stats endpoint
    @GetMapping({"/api/v1/stats", "/api/v1/global/stats"})
    public GlobalStatsResponse fetchStats() {
        return new GlobalStatsResponse(statisticsService.getInputTokens(), statisticsService.getOutputTokens());
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
