package com.example.nonogram.adapters;

import com.example.nonogram.core.io.JpnXmlWriter;
import com.example.nonogram.core.model.Crossword;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SaxJpnXmlWriter implements JpnXmlWriter {

    @Override
    public void write(Crossword cw, OutputStream out) {
        try {
            SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
            TransformerHandler th = tf.newTransformerHandler();
            Transformer tr = th.getTransformer();

            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            Result result = new StreamResult(out);
            th.setResult(result);

            AttributesImpl attrs = new AttributesImpl();

            th.startDocument();
            th.startElement("", "", "crossword", attrs);

            // <rows>
            th.startElement("", "", "rows", attrs);
            for (List<Integer> row : cw.getRows()) {
                th.startElement("", "", "row", attrs);
                if (!row.isEmpty()) {
                    writeChars(th, joinInts(row));
                }
                th.endElement("", "", "row");
            }
            th.endElement("", "", "rows");

            // <columns>
            th.startElement("", "", "columns", attrs);
            for (List<Integer> col : cw.getColumns()) {
                th.startElement("", "", "column", attrs);
                if (!col.isEmpty()) {
                    writeChars(th, joinInts(col));
                }
                th.endElement("", "", "column");
            }
            th.endElement("", "", "columns");

            th.endElement("", "", "crossword");
            th.endDocument();

            out.flush();
        } catch (Exception e) {
            throw new RuntimeException("SAX writer error: " + e.getMessage(), e);
        }
    }

    private static void writeChars(TransformerHandler th, String s) throws Exception {
        char[] data = s.toCharArray();
        th.characters(data, 0, data.length);
    }

    private static String joinInts(List<Integer> nums) {
        StringBuilder sb = new StringBuilder(nums.size() * 2);
        for (int i = 0; i < nums.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(nums.get(i));
        }
        return sb.toString();
    }
}
