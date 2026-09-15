package com.example.assignment1.controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class SpeechController {
    @GetMapping("/api/hello")
    public String testEndpoint() {
        return "Speech controller is working!";
    }
}