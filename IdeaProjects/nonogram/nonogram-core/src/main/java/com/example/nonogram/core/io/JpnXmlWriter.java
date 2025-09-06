package com.example.nonogram.core.io;

import com.example.nonogram.core.model.Crossword;

import java.io.OutputStream;

public interface JpnXmlWriter {
    void write(Crossword crossword, OutputStream out);
}