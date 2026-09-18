package com.example.assignment1;

import com.example.assignment1.service.SpeechToTextClient;
import com.example.assignment1.service.TranscriptionResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ConcurrencyLoadTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private SpeechToTextClient speechToText;

    @Test
    void handlesTwoHundredPlusConcurrentTranscriptionRequests() throws Exception {

        when(speechToText.transcribe(any(), any())).thenAnswer(invocation -> {
            Thread.sleep(50);
            return new TranscriptionResult("stubbed transcript", 10, 2);
        });

        int requestCount = 250;

        ExecutorService pool = Executors.newFixedThreadPool(requestCount);
        AtomicInteger successCount = new AtomicInteger();
        List<Future<HttpStatusCode>> futures = new ArrayList<>();

        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        long start = System.currentTimeMillis();

        for (int i = 0; i < requestCount; i++) {
            futures.add(pool.submit(() -> sendOneRequest(restClient)));
        }

        for (Future<HttpStatusCode> future : futures) {
            HttpStatusCode status = future.get(20, TimeUnit.SECONDS);

            if (status.is2xxSuccessful()) {
                successCount.incrementAndGet();
            }
        }

        long elapsedMs = System.currentTimeMillis() - start;

        pool.shutdown();

        assertEquals(requestCount, successCount.get(),
                "All concurrent requests should succeed");

        assertTrue(elapsedMs < 10_000,
                "Requests took too long: " + elapsedMs + "ms");
    }

    private HttpStatusCode sendOneRequest(RestClient restClient) {

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("audio", new ByteArrayResource(
                "fake-audio-bytes".getBytes()
        ) {
            @Override
            public String getFilename() {
                return "recording.webm";
            }
        });

        ResponseEntity<String> response = restClient
                .post()
                .uri("/api/transcribe")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        return response.getStatusCode();
    }
}