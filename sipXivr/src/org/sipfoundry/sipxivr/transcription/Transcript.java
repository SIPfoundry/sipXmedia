package org.sipfoundry.sipxivr.transcription;

public class Transcript {
    private String content;
    private int confidence;
    
    public Transcript() {
        
    }
    
    public Transcript(String content, int confidence) {
        this.content = content;
        this.confidence = confidence;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public int getConfidence() {
        return confidence;
    }
    
    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }
}
