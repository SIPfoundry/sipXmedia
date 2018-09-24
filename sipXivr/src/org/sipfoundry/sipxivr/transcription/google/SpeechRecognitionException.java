package org.sipfoundry.sipxivr.transcription.google;

import org.sipfoundry.sipxivr.transcription.RecognitionException;

public class SpeechRecognitionException extends RecognitionException {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public SpeechRecognitionException() {
    }
    
    public SpeechRecognitionException(Throwable cause) {
        super(cause);
    }
    
    public SpeechRecognitionException(SpeechRecognitionError error) {
        super(error.getMessage());
    }
    
    public SpeechRecognitionException(SpeechRecognitionError error, Throwable cause) {
        super(error.getMessage(), cause);
    }
}
