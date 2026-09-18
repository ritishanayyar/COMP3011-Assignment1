package com.example.assignment1.controller;

import com.example.assignment1.service.StatisticsService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class AdminController {

    private final ApplicationContext springContext;
    private final Instant serverStartTime = Instant.now();
    private final StatisticsService statisticsService; //tracks if shutdown was already requested
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    public AdminController(ApplicationContext springContext, StatisticsService statisticsService) {
        this.springContext = springContext;
        this.statisticsService = statisticsService;
    }
    public record UptimeResponse(
            String utcServerStart,
            String utcNow,
            double serverUptimeSeconds
    ) {}
    public record GlobalStatsResponse(long inputTokens, long outputTokens) {}
    // uptime end point
    @GetMapping({"/api/v1/uptime","/api/v1/admin/uptime"})
    public UptimeResponse fetchUptime() {
        Instant now = Instant.now();
        double uptimeSeconds = Duration.between(serverStartTime, now).toNanos() / 1_000_000_000.0;
        return new UptimeResponse(
                serverStartTime.toString(),
                now.toString(),
                uptimeSeconds
        );
    } //easier to understand rather than usinf mapping.

    // runtime stats endpoint 
    @GetMapping({"/api/v1/stats", "/api/v1/global/stats"})
    public GlobalStatsResponse fetchStats() {
        return new GlobalStatsResponse(statisticsService.getInputTokens(), statisticsService.getOutputTokens());
    }
    // graceful shut down endpoint
    @PostMapping("/api/v1/admin/shutdown")
    public ResponseEntity<?> triggerShutdown() {
        // if shutdown was already requested, return 409 instead of starting another thread
        if (!shuttingDown.compareAndSet(false, true)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "timestamp", Instant.now().toString(),
                    "status", 409,
                    "error", "Conflict",
                    "message", "Graceful shutdown is already in progress.",
                    "path", "/api/v1/admin/shutdown"
            ));
        }
        Thread shutdownWorker =new Thread(() -> {
            try {Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); }
            SpringApplication.exit(springContext, () -> 0);
            System.exit(0);
        });
        shutdownWorker.setDaemon(false);
        shutdownWorker.start();
        return ResponseEntity.accepted().body(Map.of("message", "Graceful shutdown requested."));
    }
}