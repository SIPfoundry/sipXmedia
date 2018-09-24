package org.sipfoundry.sipxivr.transcription.google;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.sipfoundry.sipxivr.transcription.LanguageCode;
import org.sipfoundry.sipxivr.transcription.RecognitionException;
import org.sipfoundry.sipxivr.transcription.Speech;
import org.sipfoundry.sipxivr.transcription.Transcript;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GSpeechAPI implements Speech {
    private static final Logger LOG = Logger.getLogger("org.sipfoundry.sipxivr");
    private static final String SPEECH_API_URL = "https://speech.googleapis.com/v1";
    private static final String SPEECH_RECOGNIZE_METHOD = "speech:recognize";
    
    private ObjectMapper m_jsonMapper;
    private String m_apiKey;
    
    public GSpeechAPI() {
        m_jsonMapper = new ObjectMapper();
        m_jsonMapper.setSerializationInclusion(Include.NON_NULL);
    }

    @Override
    public Transcript recognize(File audioFile, LanguageCode languageCode) throws RecognitionException {
        if (StringUtils.isBlank(m_apiKey)) {
            throw new RecognitionException("Google Speech API Key must be provided.");
        }
        
        try {
            RecognitionRequest request = RecognitionRequest.create(audioFile, languageCode);
            return recognize(request);
        } catch (IOException e) {
            throw new RecognitionException("Failed to create RecognitionRequest.", e);
        }
    }
    
    private Transcript recognize(RecognitionRequest request) throws RecognitionException {
        try {
            String url = SPEECH_API_URL 
                    + "/" + SPEECH_RECOGNIZE_METHOD 
                    + "?key=" + m_apiKey;
            
            URL speechUrl = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) speechUrl.openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            
            OutputStreamWriter outputStream = new OutputStreamWriter(con.getOutputStream());
            m_jsonMapper.writeValue(outputStream, request);
            
            int HttpResult = con.getResponseCode(); 
            if (HttpResult == HttpsURLConnection.HTTP_OK) {
                String result = readInputStream(con.getInputStream());
                RecognitionResponse response = m_jsonMapper.readValue(result, RecognitionResponse.class);
                return new Transcript(response.getTranscript(), response.getConfidence());
            } else {
                String result = readInputStream(con.getErrorStream());  
                ErrorResponse response = m_jsonMapper.readValue(result, ErrorResponse.class);
                throw new SpeechRecognitionException(response.getError());
            }  
        } catch (IOException e) {
            LOG.error("Error json error" + e, e);
        }
        
        return null;
    }
    
    private String readInputStream(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(
                new InputStreamReader(inputStream, "utf-8"));
        String line = null;  
        while ((line = br.readLine()) != null) {  
            sb.append(line + "\n");  
        }
        br.close();
        return sb.toString();
    }
    
    public void setApiKey(String authKey) {
        m_apiKey = authKey;
    }
}
