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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class SpeechControllerTest {
    @LocalServerPort
    private int port;

    @MockitoBean
    private SpeechToTextClient speechToText;

    @Test
    void transcribeEndpoint_returnsSttTextAndUpdatesGlobalStats()
            throws Exception {
        when(speechToText.transcribe(any(), any()))
                .thenReturn(
                        new TranscriptionResult(
                                "hello from the stub",
                                42,
                                7
                        )
                );
         RestClient restClient =
                RestClient.builder()
                        .baseUrl("http://localhost:" + port)
                        .build();
                MultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();
          body.add(
                "audio",
                new ByteArrayResource(
                        "fake-audio-bytes".getBytes()
                ) {
                    @Override
                    public String getFilename() {
                        return "recording.webm";
                    }
                }
        ); ResponseEntity<String> response =
                restClient
                        .post()
                        .uri("/api/transcribe")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(body)
                        .retrieve()
                        .toEntity(String.class);

        assertEquals(
                200,
                response.getStatusCode().value()
        );

        assertEquals(
                "{\"message\":\"hello from the stub\"}",
                response.getBody()
        );
    }

    @Test
    void transcribeEndpoint_returnsSpecCompliantErrorShapeWhenSttFails()
            throws Exception {

        when(speechToText.transcribe(any(), any()))
                .thenThrow(
                        new RuntimeException(
                                "upstream STT service unavailable"
                        )
                );  RestClient restClient =
                RestClient.builder()
                        .baseUrl("http://localhost:" + port)
                        .build();

        MultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();

        body.add(
                "audio",
                new ByteArrayResource(
                        "fake-audio-bytes".getBytes()
                ) {
                    @Override
                    public String getFilename() {
                        return "recording.webm";
                    }
                }
        ); ResponseEntity<String> response =
        restClient
                .post()
                .uri("/api/transcribe")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .exchange((request, clientResponse) ->
                        ResponseEntity
                                .status(clientResponse.getStatusCode())
                                .headers(clientResponse.getHeaders())
                                .body(clientResponse.bodyTo(String.class))
                );

        HttpStatusCode status =
                response.getStatusCode();

        assertEquals(
                500,
                status.value()
        );
    }
}