package com.example.assignment1.controller;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SpeechController {
    @GetMapping("/api/hello")
    public String testEndpoint() {
        return "Speech controller is working!";
    }

    @PostMapping("/api/transcribe")
    public String transcribe(@RequestParam("audio") MultipartFile audio) {
        try {
            //read the API key from the termina;'s environment var'
            String apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                return "Error: OPENAI_API_KEY environment variable is not set.";
            }
            // Creating Spring's RestClient to talk to OpenAI
            RestClient restClient = RestClient.create();

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", audio.getResource());
            body.add("model", "gpt-4o-mini-transcribe"); 

            //Send POST request with multipart form data
            String response = restClient.post()
                .uri("https://api.openai.com/v1/audio/transcriptions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(String.class);
            return response; // Returns the JSON text response from OpenAI
            
        } catch (Exception e) {
            return "Transcription failed: " + e.getMessage();
        }
    }
}