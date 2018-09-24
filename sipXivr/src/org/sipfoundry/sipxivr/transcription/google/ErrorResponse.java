package org.sipfoundry.sipxivr.transcription.google;

public class ErrorResponse {
    private SpeechRecognitionError error;

    public SpeechRecognitionError getError() {
        return error;
    }

    public void setError(SpeechRecognitionError error) {
        this.error = error;
    }
    
}
