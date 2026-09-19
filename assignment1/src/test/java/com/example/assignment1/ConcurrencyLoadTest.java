
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
    when(speechToText.transcribe(any(), any()))
            .thenAnswer(invocation ->{
                Thread.sleep(50);
                return new TranscriptionResult("test text", 10,2);
            });
    int numberOfRequests= 250; //handling 250 reqs concurrently
    ExecutorService executor =Executors.newFixedThreadPool(numberOfRequests);
            
    CountDownLatch start =new CountDownLatch(1);
    CountDownLatch finish = new CountDownLatch(numberOfRequests); //makes all requests start with each other so concurrency caqn be tested properly.
    AtomicInteger successful= new AtomicInteger();
    RestClient client = RestClient.builder()
            .baseUrl("http://localhost:"+ port)
            .build();
            
    for (int i= 0; i<numberOfRequests;i++) {
        executor.submit(()->{
            try {start.await();
            HttpStatusCode status = sendRequest(client);
             if (status.is2xxSuccessful()) {
                    successful.incrementAndGet();
                }

            } catch (Exception e) {
                // Request failed
            } finally {
                finish.countDown();
            }
        });
    }

    long startTime = System.currentTimeMillis();

    start.countDown();
    boolean completed =finish.await(20, TimeUnit.SECONDS);
    long timeTaken = System.currentTimeMillis()- startTime;

    executor.shutdown();
    assertTrue(completed);
    assertEquals(numberOfRequests, successful.get());
    assertTrue(timeTaken <10000);
}
}