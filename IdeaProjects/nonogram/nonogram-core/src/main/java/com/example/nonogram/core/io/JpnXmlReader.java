package com.example.nonogram.core.io;

import com.example.nonogram.core.model.Crossword;
import java.io.InputStream;

public interface JpnXmlReader {
    Crossword read(InputStream in) throws JpnXmlReaderException;
}