package com.example.nonogram.core.model;

import java.util.List;
import java.util.Objects;

public final class Crossword {
    private final List<List<Integer>> rows;
    private final List<List<Integer>> columns;

    public Crossword(List<List<Integer>> rows, List<List<Integer>> columns) {
        this.rows = List.copyOf(rows);
        this.columns = List.copyOf(columns);
        validate();
    }

    private void validate() {
        if (rows == null || columns == null || rows.isEmpty() || columns.isEmpty())
            throw new IllegalArgumentException("Rows/columns must be non-empty.");
        if (rows.stream().anyMatch(Objects::isNull) || columns.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Row/column lists must not contain null.");
        if (rows.stream().flatMap(List::stream).anyMatch(n -> n == null || n < 0)
                || columns.stream().flatMap(List::stream).anyMatch(n -> n == null || n < 0))
            throw new IllegalArgumentException("Clue numbers must be >= 0.");
    }

    public List<List<Integer>> getRows() { return rows; }
    public List<List<Integer>> getColumns() { return columns; }
    public int height() { return rows.size(); }
    public int width() { return columns.size(); }
}