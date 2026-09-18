package com.example.assignment1.controller;

import com.example.assignment1.service.SpeechToTextClient;
import com.example.assignment1.service.StatisticsService;
import com.example.assignment1.service.TranscriptionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;
import java.util.Map;
@RestController
public class SpeechController {
    private static final Logger log =
            LoggerFactory.getLogger(SpeechController.class);
    private final StatisticsService statsService;
    private final SpeechToTextClient speechToText;
    public SpeechController(
            StatisticsService statsService,
            SpeechToTextClient speechToText) {
        this.statsService = statsService;
        this.speechToText = speechToText;
    }
    @PostMapping("/api/transcribe")
    public ResponseEntity<?> handleAudio(
            @RequestParam("audio") MultipartFile file) {

        try {TranscriptionResult result =
                    speechToText.transcribe(
                            file.getBytes(),
                            "recording.webm"
                    );
                    statsService.addTokens(
                    result.inputTokens(),
                    result.outputTokens()
            );return ResponseEntity.ok(
                    Map.of("message", result.text())
            );

        } catch (Exception e) {
            log.warn(
                    "Transcription request failed: {}",e.getMessage()
            );
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("timestamp",Instant.now().toString(),"status",500,"error","Internal Server Error",
                    "message","Transcription failed. Please try again.","path","/api/transcribe"
                    ));
        }
    }
}