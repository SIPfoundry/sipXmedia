package org.sipfoundry.sipxivr.transcription.google;

import java.io.File;
import java.io.IOException;

import org.sipfoundry.sipxivr.transcription.LanguageCode;

public class RecognitionRequest {
    private RecognitionAudio m_audio;
    private RecognitionConfig m_config;
    
    public static RecognitionRequest create(File audio, LanguageCode code) throws IOException {
        RecognitionRequest req = new RecognitionRequest();
        req.setAudio(RecognitionAudio.create(audio));
        
        String filename = audio.getName();
        if(filename.endsWith(".wav")) {
            req.setConfig(RecognitionConfig.createForWAV(code));
        } else if(filename.endsWith(".flac")) {
            req.setConfig(RecognitionConfig.createForFLAC(code));
        } else {
            throw new IllegalArgumentException("Unsupported Audio Type");
        }
        return req;
    }
    
    public RecognitionAudio getAudio() {
        return m_audio;
    }
    
    public void setAudio(RecognitionAudio audio) {
        this.m_audio = audio;
    }
    
    public RecognitionConfig getConfig() {
        return m_config;
    }
    
    public void setConfig(RecognitionConfig config) {
        this.m_config = config;
    }
    
}
