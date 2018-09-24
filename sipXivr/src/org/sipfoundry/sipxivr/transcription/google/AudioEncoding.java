package org.sipfoundry.sipxivr.transcription.google;

public enum AudioEncoding {
    LINEAR16("LINEAR16"),
    FLAC("FLAC"),
    MULAW("MULAW"),
    AMR("AMR"),
    AMR_WB("AMR_WB"),
    OGG_OPUS("OGG_OPUS"),
    SPEEX("SPEEX_WITH_HEADER_BYTE");
    
    private String code;
    
    private AudioEncoding(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
}
