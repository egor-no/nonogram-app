package com.example.nonogram.config;

import com.example.nonogram.adapters.*;
import com.example.nonogram.core.io.JpnXmlReader;
import com.example.nonogram.core.io.JpnXmlWriter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IoProperties.class)
public class ReaderWriterConfig {

    @Bean
    public JpnXmlReader jpnXmlReader(IoProperties props) {
        return switch (props.getReader()) {
            case SAX    -> new SaxJpnXmlReader();
            case STAX   -> new StaxJpnXmlReader();
            case DOM    -> new DomJpnXmlReader();
            case JACKSON/* когда реализуешь */ -> throw new IllegalStateException("Jackson reader not implemented");
        };
    }

    @Bean
    public JpnXmlWriter jpnXmlWriter(IoProperties props) {
        return switch (props.getWriter()) {
            case SAX   -> new SaxJpnXmlWriter();
            case STAX  -> new StaxJpnXmlWriter();
            case DOM   -> new DomJpnXmlWriter();
        };
    }
}
