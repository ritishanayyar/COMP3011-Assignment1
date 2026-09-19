
package com.example.assignment1;

import com.example.assignment1.service.SpeechToTextClient;
import com.example.assignment1.service.TranscriptionResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment =SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ConcurrencyLoadTest {
     @LocalServerPort
    private int port;
    @MockitoBean
    private SpeechToTextClient speechToText;
    @Test
    void handlesConcurrentRequests() throws Exception {
        when(speechToText.transcribe(any() ,any())) //faking the STT response
                .thenReturn(new TranscriptionResult("test text", 10,2));
            int numberOfRequests = 250;

        ExecutorService executor= Executors.newFixedThreadPool(50);

        CountDownLatch start =new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(numberOfRequests);
        AtomicInteger successful = new AtomicInteger();

        RestClient client =RestClient.builder()
                .baseUrl("http://localhost:" +port)
                .build();

        for (int i =0; i< numberOfRequests;i++) {
            executor.submit(()-> {
                try {
                    start.await();
                    HttpStatusCode status = sendRequest(client);
                    if (status.is2xxSuccessful()){
                        successful.incrementAndGet();}
                } catch (Exception e) {
                    //request failed hre
                } finally {
                    finish.countDown();
                }
            });}
        long startTime = System.currentTimeMillis();
        start.countDown();

        boolean completed = finish.await(20, TimeUnit.SECONDS);
        long timeTaken = System.currentTimeMillis()- startTime;
        executor.shutdown();

        assertTrue(completed);
        assertEquals(numberOfRequests, successful.get());
        assertTrue(timeTaken < 10000);
    }
    private HttpStatusCode sendRequest(RestClient client) {

        MultiValueMap<String,Object> body =new LinkedMultiValueMap<>();
        body.add("audio", new ByteArrayResource(
                "fake audio".getBytes()
        ) {
            @Override
            public String getFilename() {
                return "recording.webm";
            }
        });
        ResponseEntity<String> response = client
                .post()
                .uri("/api/transcribe")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        return response.getStatusCode();
    }
}
