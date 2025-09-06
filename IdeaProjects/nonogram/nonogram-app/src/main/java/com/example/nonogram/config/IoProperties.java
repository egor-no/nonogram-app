package com.example.nonogram.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nonogram.io")
public class IoProperties {
    public enum Reader { DOM, SAX, STAX, JACKSON }
    public enum Writer { DOM, SAX, STAX }

    private Reader reader = Reader.SAX;
    private Writer writer = Writer.SAX;

    public Reader getReader() {
        return reader;
    }

    public void setReader(Reader reader) {
        this.reader = reader;
    }

    public Writer getWriter() {
        return writer;
    }

    public void setWriter(Writer writer) {
        this.writer = writer;
    }
}