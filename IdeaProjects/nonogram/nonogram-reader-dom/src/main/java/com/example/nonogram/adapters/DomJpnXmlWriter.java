package com.example.nonogram.adapters;

import com.example.nonogram.core.io.JpnXmlWriter;
import com.example.nonogram.core.model.Crossword;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.util.List;

public class DomJpnXmlWriter implements JpnXmlWriter {

    @Override
    public void write(Crossword cw, OutputStream out) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = doc.createElement("crossword");
            doc.appendChild(root);

            Element rowsEl = doc.createElement("rows");
            root.appendChild(rowsEl);
            for (List<Integer> row : cw.getRows()) {
                Element rowEl = doc.createElement("row");
                if (!row.isEmpty()) {
                    rowEl.setTextContent(joinInts(row));
                }
                rowsEl.appendChild(rowEl);
            }

            Element colsEl = doc.createElement("columns");
            root.appendChild(colsEl);
            for (List<Integer> col : cw.getColumns()) {
                Element colEl = doc.createElement("column");
                if (!col.isEmpty()) {
                    colEl.setTextContent(joinInts(col));
                }
                colsEl.appendChild(colEl);
            }

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            t.transform(new DOMSource(doc), new StreamResult(out));
        } catch (Exception e) {
            throw new RuntimeException("Failed to write JPNXML: " + e.getMessage(), e);
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
