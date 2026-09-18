package com.example.assignment1.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import com.example.assignment1.service.StatisticsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@RestController
public class SpeechController {

    private final StatisticsService statsService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    public SpeechController(StatisticsService statsService) {
        this.statsService = statsService;
    }
    @PostMapping("/api/transcribe") //endpoint for uploading the audio and comvert to text
    public ResponseEntity<?> handleAudio(@RequestParam("audio") MultipartFile file) {
        try {
            String apiKey = System.getenv("OPENAI_API_KEY"); //gets the api keu
            MultiValueMap<String, Object> payload = new LinkedMultiValueMap<>();
            payload.add("file", file.getResource());
            payload.add("model", "gpt-4o-mini-transcribe");

            String response = restClient.post() //audio to openAI
                    .uri("https://api.openai.com/v1/audio/transcriptions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            JsonNode root = mapper.readTree(response);
            String transcribedText = root.get("text").asText();

            JsonNode usage = root.get("usage");
            long inputTokens = usage != null ? usage.get("input_tokens").asLong() : 0;
            long outputTokens = usage != null ? usage.get("output_tokens").asLong() : 0;
            statsService.addTokens(inputTokens, outputTokens); //update global token stats

            return ResponseEntity.ok(Map.of("message", transcribedText));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}