package com.example.nonogram.adapters;

import com.example.nonogram.core.io.JpnXmlReader;
import com.example.nonogram.core.io.JpnXmlReaderException;
import com.example.nonogram.core.model.Crossword;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SaxJpnXmlReader implements JpnXmlReader {

    @Override
    public Crossword read(InputStream in) throws JpnXmlReaderException {
        if (in == null) throw new JpnXmlReaderException("InputStream is null");
        try {
            SAXParserFactory f = SAXParserFactory.newInstance();
            f.setNamespaceAware(false);
            f.setValidating(false);

            // Безопасность (XXE и т.п.)
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            SAXParser parser = f.newSAXParser();
            CrosswordHandler handler = new CrosswordHandler();
            parser.parse(in, handler);

            if (!handler.seenRoot) {
                throw new JpnXmlReaderException("Root <crossword> not found");
            }
            if (handler.rows.isEmpty() || handler.cols.isEmpty()) {
                throw new JpnXmlReaderException("<rows> or <columns> are empty/missing");
            }
            return new Crossword(handler.rows, handler.cols);
        } catch (JpnXmlReaderException e) {
            throw e;
        } catch (Exception e) {
            throw new JpnXmlReaderException("SAX parse error: " + e.getMessage(), e);
        }
    }

    private static final class CrosswordHandler extends DefaultHandler {
        boolean inRows = false;
        boolean inCols = false;
        boolean inRowItem = false;
        boolean inColItem = false;
        boolean seenRoot = false;

        final List<List<Integer>> rows = new ArrayList<>();
        final List<List<Integer>> cols = new ArrayList<>();

        StringBuilder text = new StringBuilder();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            switch (qName) {
                case "crossword" -> seenRoot = true;
                case "rows" -> inRows = true;
                case "columns" -> inCols = true;
                case "row" -> {
                    if (!inRows) break;
                    inRowItem = true;
                    text.setLength(0);
                }
                case "column" -> {
                    if (!inCols) break;
                    inColItem = true;
                    text.setLength(0);
                }
                default -> { /* игнорируем прочее */ }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (inRowItem || inColItem) {
                text.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (qName) {
                case "rows" -> inRows = false;
                case "columns" -> inCols = false;

                case "row" -> {
                    if (inRowItem) {
                        rows.add(parseLine(text.toString()));
                        inRowItem = false;
                        text.setLength(0);
                    }
                }
                case "column" -> {
                    if (inColItem) {
                        cols.add(parseLine(text.toString()));
                        inColItem = false;
                        text.setLength(0);
                    }
                }
                default -> { /* игнорируем прочее */ }
            }
        }

        private static List<Integer> parseLine(String raw) throws SAXException {
            String s = raw == null ? "" : raw.trim();
            if (s.isEmpty()) return List.of();
            String[] parts = s.split("\\s+");
            List<Integer> out = new ArrayList<>(parts.length);
            try {
                for (String p : parts) {
                    if (p.isEmpty()) continue;
                    int v = Integer.parseInt(p);
                    if (v <= 0) throw new NumberFormatException("non-positive: " + v);
                    out.add(v);
                }
            } catch (NumberFormatException e) {
                throw new SAXException("Bad clue number in line: \"" + raw + "\" (" + e.getMessage() + ")", e);
            }
            return List.copyOf(out);
        }
    }
}
