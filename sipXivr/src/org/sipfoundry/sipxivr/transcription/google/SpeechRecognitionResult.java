package org.sipfoundry.sipxivr.transcription.google;

import java.util.List;

public class SpeechRecognitionResult {
    private List<SpeechRecognitionAlternative> alternatives;

    public List<SpeechRecognitionAlternative> getAlternatives() {
        return alternatives;
    }
    
    public void setAlternatives(List<SpeechRecognitionAlternative> alternatives) {
        this.alternatives = alternatives;
    }
    
    public String getTranscript() {
        // Right now get the first alternative on the list and return the transcript
        if (alternatives != null && !alternatives.isEmpty()) {
            return alternatives.get(0).getTranscript();
        }
        
        return "";
    }
    
    public Double getConfidence() {
        if (alternatives != null && !alternatives.isEmpty()) {
            return alternatives.get(0).getConfidence();
        }
        
        return new Double(0.0);
    }
}
