package org.sipfoundry.sipxivr.transcription.google;

import org.sipfoundry.sipxivr.transcription.LanguageCode;

public class RecognitionConfig {
    private String m_encoding;
    private Integer m_sampleRateHertz;
    private String m_languageCode;
    private Integer m_maxAlternatives;
    private Boolean m_profanityFilter;
    private Boolean m_enableWordTimeOffsets;
    
    public static RecognitionConfig createForWAV(LanguageCode code) {
        RecognitionConfig config = new RecognitionConfig();
        config.setLanguageCode(code.getCode());
        //config.setEncoding(AudioEncoding.LINEAR16.getCode());
        //config.setSampleRateHertz(8000);
        return config;
    }
    
    public static RecognitionConfig createForFLAC(LanguageCode code) {
        RecognitionConfig config = new RecognitionConfig();
        config.setLanguageCode(code.getCode());
        config.setEncoding(AudioEncoding.FLAC.getCode());
        return config;
    }
    
    public static RecognitionConfig create(AudioEncoding encoding, LanguageCode code) {
        RecognitionConfig config = new RecognitionConfig();
        config.setEncoding(encoding.getCode());
        config.setLanguageCode(code.getCode());

        switch (encoding) {
        case AMR: config.setSampleRateHertz(8000); break;
        case AMR_WB: config.setSampleRateHertz(16000); break;
        case SPEEX: config.setSampleRateHertz(16000); break;
        default: break;
        }
        
        return config;
    }
    
    public RecognitionConfig() {
        
    }
    
    public String getEncoding() {
        return m_encoding;
    }
    
    public void setEncoding(String encoding) {
        this.m_encoding = encoding;
    }
    
    public Integer getSampleRateHertz() {
        return m_sampleRateHertz;
    }
    
    public void setSampleRateHertz(Integer sampleRateHertz) {
        this.m_sampleRateHertz = sampleRateHertz;
    }
    
    public String getLanguageCode() {
        return m_languageCode;
    }
    
    public void setLanguageCode(String languageCode) {
        this.m_languageCode = languageCode;
    }
    
    public Integer getMaxAlternatives() {
        return m_maxAlternatives;
    }
    
    public void setMaxAlternatives(Integer maxAlternatives) {
        this.m_maxAlternatives = maxAlternatives;
    }
    
    public Boolean getProfanityFilter() {
        return m_profanityFilter;
    }
    
    public void setProfanityFilter(Boolean profanityFilter) {
        this.m_profanityFilter = profanityFilter;
    }
    
    public Boolean getEnableWordTimeOffsets() {
        return m_enableWordTimeOffsets;
    }
    
    public void setEnableWordTimeOffsets(Boolean enableWordTimeOffsets) {
        this.m_enableWordTimeOffsets = enableWordTimeOffsets;
    }
    
    
}
