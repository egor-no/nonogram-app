package com.example.nonogram.adapters;

import com.example.nonogram.core.io.JpnXmlReader;
import com.example.nonogram.core.io.JpnXmlReaderException;
import com.example.nonogram.core.model.Crossword;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StaxJpnXmlReader implements JpnXmlReader {

    @Override
    public Crossword read(InputStream in) throws JpnXmlReaderException {
        if (in == null) throw new JpnXmlReaderException("InputStream is null");
        try {
            XMLInputFactory f = XMLInputFactory.newInstance();

            f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
            f.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            f.setProperty("javax.xml.stream.supportDTD", false);

            XMLStreamReader r = f.createXMLStreamReader(in, "UTF-8");

            boolean seenRoot = false;
            boolean inRows = false, inCols = false;
            boolean inRowItem = false, inColItem = false;
            StringBuilder text = new StringBuilder();

            List<List<Integer>> rows = new ArrayList<>();
            List<List<Integer>> cols = new ArrayList<>();

            while (r.hasNext()) {
                int ev = r.next();
                switch (ev) {
                    case XMLStreamConstants.START_ELEMENT -> {
                        String name = r.getLocalName();
                        switch (name) {
                            case "crossword" -> seenRoot = true;
                            case "rows" -> inRows = true;
                            case "columns" -> inCols = true;
                            case "row" -> {
                                if (inRows) {
                                    inRowItem = true;
                                    text.setLength(0);
                                }
                            }
                            case "column" -> {
                                if (inCols) {
                                    inColItem = true;
                                    text.setLength(0);
                                }
                            }
                            default -> {}
                        }
                    }
                    case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> {
                        if (inRowItem || inColItem) {
                            text.append(r.getText());
                        }
                    }
                    case XMLStreamConstants.END_ELEMENT -> {
                        String name = r.getLocalName();
                        switch (name) {
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
                            default -> {}
                        }
                    }
                    default -> {}
                }
            }
            r.close();

            if (!seenRoot)
                throw new JpnXmlReaderException("Root <crossword> not found");
            if (rows.isEmpty() || cols.isEmpty())
                throw new JpnXmlReaderException("<rows> or <columns> are empty/missing");

            return new Crossword(rows, cols);
        } catch (JpnXmlReaderException e) {
            throw e;
        } catch (Exception e) {
            throw new JpnXmlReaderException("StAX parse error: " + e.getMessage(), e);
        }
    }

    private static List<Integer> parseLine(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return List.of();
        String[] parts = s.split("\\s+");
        List<Integer> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            if (p.isEmpty()) continue;
            int v = Integer.parseInt(p);
            if (v <= 0) throw new IllegalArgumentException("Clue numbers must be > 0, got: " + v);
            out.add(v);
        }
        return List.copyOf(out);
    }
}
