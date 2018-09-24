package org.sipfoundry.sipxivr.transcription.google;

public class SpeechRecognitionError {
    private Integer code;
    private String message;
    private String status;
    
    public Integer getCode() {
        return code;
    }
    
    public void setCode(Integer code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getErrorMessage() {
        return message + " (code:" + code + ")";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    
}
