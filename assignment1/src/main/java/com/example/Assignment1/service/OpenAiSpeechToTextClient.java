package com.example.assignment1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

//actually calls openai's transcription endpoint
@Component
public class OpenAiSpeechToTextClient implements SpeechToTextClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiSpeechToTextClient.class);
    private static final String OPENAI_URL = "https://api.openai.com/v1/audio/transcriptions";
    private static final String MODEL= "gpt-4o-mini-transcribe";
    //timeouts so a slow call to openai cant hang a thread forever
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient restClient;

    public OpenAiSpeechToTextClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MS);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }
    @Override
    public TranscriptionResult transcribe(byte[] audioBytes, String filename) throws Exception {
        // read the key from the environment every call, never store it in a field
        String apiKey =System.getenv("OPENAI_API_KEY");
        if (apiKey == null|| apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is not set");
        }
        MultiValueMap<String, Object> payload = new LinkedMultiValueMap<>();
        payload.add("file", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        payload.add("model", MODEL);
        // only format this model supports, and its the one that includes token usage
        long start = System.nanoTime();
        String response = restClient.post()
                .uri(OPENAI_URL)
                .header("Authorization", "Bearer " + apiKey)
                .body(payload)
                .retrieve()
                .body(String.class);
        long elapsedMs = (System.nanoTime() - start) /1_000_000;
        JsonNode root = mapper.readTree(response);
        String text = root.path("text").asText("");

        JsonNode usage = root.path("usage");
        long inputTokens = usage.path("input_tokens").asLong(0);
        long outputTokens = usage.path("output_tokens").asLong(0);
        log.info("STT call took {}ms, inputTokens={}, outputTokens={}", elapsedMs, inputTokens, outputTokens);

        return new TranscriptionResult(text, inputTokens, outputTokens);
    }
}