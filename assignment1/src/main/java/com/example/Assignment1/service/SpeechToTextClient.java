package com.example.assignment1.service;

// This is something that can turn audio into text.
public interface SpeechToTextClient {
     // Takes audio bytes and a filename and returns the transcription and token usage.
    TranscriptionResult transcribe( byte[] audioBytes,String filename
    ) throws Exception;

}