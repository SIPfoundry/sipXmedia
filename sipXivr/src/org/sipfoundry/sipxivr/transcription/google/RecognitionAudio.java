package org.sipfoundry.sipxivr.transcription.google;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.codec.binary.Base64;

public class RecognitionAudio {
    private String m_content;
    
    public static RecognitionAudio create(File audio) throws IOException {
        RecognitionAudio object = new RecognitionAudio();
        byte[] data = Files.readAllBytes(audio.toPath());
        object.setContent(new String(Base64.encodeBase64(data)));
        return object;
    }
    
    private RecognitionAudio() {
        
    }

    public String getContent() {
        return m_content;
    }

    private void setContent(String content) {
        this.m_content = content;
    }
}
