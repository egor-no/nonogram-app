package com.example.nonogram.adapters;

import com.example.nonogram.core.io.JpnXmlWriter;
import com.example.nonogram.core.model.Crossword;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.util.List;

public class StaxJpnXmlWriter implements JpnXmlWriter {

    @Override
    public void write(Crossword cw, OutputStream out) {
        try {
            XMLOutputFactory f = XMLOutputFactory.newInstance();
            XMLStreamWriter w = f.createXMLStreamWriter(out, "UTF-8");

            w.writeStartDocument("UTF-8", "1.0");
            w.writeStartElement("crossword");

            w.writeStartElement("rows");
            for (List<Integer> row : cw.getRows()) {
                w.writeStartElement("row");
                if (!row.isEmpty()) {
                    w.writeCharacters(joinInts(row));
                }
                w.writeEndElement();
            }
            w.writeEndElement(); // </rows>

            w.writeStartElement("columns");
            for (List<Integer> col : cw.getColumns()) {
                w.writeStartElement("column");
                if (!col.isEmpty()) {
                    w.writeCharacters(joinInts(col));
                }
                w.writeEndElement();
            }
            w.writeEndElement(); // </columns>

            w.writeEndElement(); // </crossword>
            w.writeEndDocument();

            w.flush();
            w.close();
        } catch (Exception e) {
            throw new RuntimeException("StAX writer error: " + e.getMessage(), e);
        }
    }

    private static String joinInts(List<Integer> nums) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nums.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(nums.get(i));
        }
        return sb.toString();
    }
}
