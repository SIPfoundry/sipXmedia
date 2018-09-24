package org.sipfoundry.sipxivr.transcription;

public class RecognitionException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public RecognitionException() {
    }
    
    public RecognitionException(Throwable cause) {
        super(cause);
    }
    
    public RecognitionException(String error) {
        super(error);
    }
    
    public RecognitionException(String error, Throwable cause) {
        super(error, cause);
    }

}
