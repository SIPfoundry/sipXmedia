package org.sipfoundry.sipxivr.transcription;

import java.io.File;

public interface Speech {
    Transcript recognize(File audioFile, LanguageCode languageCode) throws RecognitionException;
}
