package com.example.nonogram.adapters;

import com.example.nonogram.core.io.JpnXmlReader;
import com.example.nonogram.core.io.JpnXmlReaderException;
import com.example.nonogram.core.model.Crossword;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DomJpnXmlReader implements JpnXmlReader {

    @Override
    public Crossword read(InputStream in) throws JpnXmlReaderException {
        try {
            if (in == null) throw new JpnXmlReaderException("InputStream is null");

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setIgnoringComments(true);
            dbf.setCoalescing(true);
            dbf.setExpandEntityReferences(false);

            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(in);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            if (root == null || !"crossword".equals(root.getNodeName())) {
                throw new JpnXmlReaderException("Root element <crossword> not found");
            }

            Element rowsEl = firstChildElementByName(root, "rows");
            Element colsEl = firstChildElementByName(root, "columns");
            if (rowsEl == null || colsEl == null) {
                throw new JpnXmlReaderException("<rows> or <columns> section missing");
            }

            List<List<Integer>> rows = readLines(rowsEl, "row");
            List<List<Integer>> cols = readLines(colsEl, "column");

            if (rows.isEmpty() || cols.isEmpty()) {
                throw new JpnXmlReaderException("Rows/columns must be non-empty");
            }

            return new Crossword(rows, cols);
        } catch (JpnXmlReaderException e) {
            throw e;
        } catch (Exception e) {
            throw new JpnXmlReaderException("Failed to parse JPNXML: " + e.getMessage(), e);
        }
    }

    private static Element firstChildElementByName(Element parent, String name) {
        NodeList nl = parent.getElementsByTagName(name);
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getParentNode() == parent && n.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) n;
            }
        }
        return null;
    }

    private static List<List<Integer>> readLines(Element section, String itemName) {
        NodeList nl = section.getElementsByTagName(itemName);
        List<List<Integer>> out = new ArrayList<>(nl.getLength());
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            String text = n.getTextContent();
            out.add(parseClueLine(text));
        }
        return out;
    }

    private static List<Integer> parseClueLine(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return List.of();
        String[] parts = s.split("\\s+");
        List<Integer> nums = new ArrayList<>(parts.length);
        for (String p : parts) {
            if (p.isEmpty()) continue;
            int v = Integer.parseInt(p);
            if (v <= 0) {
                throw new IllegalArgumentException("Clue numbers must be > 0, got: " + v);
            }
            nums.add(v);
        }
        return List.copyOf(nums);
    }
}
