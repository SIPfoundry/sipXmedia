package org.sipfoundry.sipxivr.transcription.google;

import java.util.List;

public class RecognitionResponse {
    private List<SpeechRecognitionResult> results;

    public List<SpeechRecognitionResult> getResults() {
        return results;
    }

    public void setResults(List<SpeechRecognitionResult> results) {
        this.results = results;
    }
    
    public String getTranscript() {
        if (results != null) {
            StringBuilder sb = new StringBuilder();
            for(SpeechRecognitionResult result: results) {
                sb.append(result.getTranscript());
            }
            return sb.toString();
        }
        
        return "";
    }
    
    public int getConfidence() {
        if (results != null) {
            Double averageConfidence = new Double(0.0);
            for(SpeechRecognitionResult result: results) {
                averageConfidence += result.getConfidence();
            }
            return new Double((averageConfidence / results.size()) * 100).intValue();
        }
        
        return 0;
    }
}
