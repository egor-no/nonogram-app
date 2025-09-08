package com.example.nonogram.solver;

import com.example.nonogram.core.AntiSolver;
import com.example.nonogram.core.model.Crossword;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JPNAntiSolver implements AntiSolver {

    @Override
    public Crossword fromFilled(boolean[][] filled) {
        validateRect(filled);

        List<List<Integer>> rows = new ArrayList<>();
        List<List<Integer>> columns = new ArrayList<>();

        int h = filled.length;
        int w = filled[0].length;

        int blockLength = 0;
        List<Integer> row;
        for (int r = 0; r < h; r++) {
            row = new ArrayList<>();
            for (int c = 0; c < w; c++) {
                if (filled[r][c]) {
                    blockLength += 1;
                } else {
                    if (blockLength > 0) {
                        row.add(blockLength);
                        blockLength = 0;
                    }
                }
            }
            if (blockLength > 0) {
                row.add(blockLength);
                blockLength = 0;
            }
            rows.add(row);
        }

        List<Integer> column;
        for (int c = 0; c < w; c++) {
            column = new ArrayList<>();
            for (int r = 0; r < h; r++) {
                if (filled[r][c]) {
                    blockLength += 1;
                } else {
                    if (blockLength > 0) {
                        column.add(blockLength);
                        blockLength = 0;
                    }
                }
            }
            if (blockLength > 0) {
                column.add(blockLength);
                blockLength = 0;
            }
            columns.add(column);
        }

        return new Crossword(rows, columns);
    }

    private static void validateRect(boolean[][] filled) {
        if (filled == null || filled.length == 0)
            throw new IllegalArgumentException("Grid must be non-empty");
        int w = filled[0].length;
        if (w == 0)
            throw new IllegalArgumentException("Grid width must be > 0");
        for (int r = 1; r < filled.length; r++) {
            if (filled[r] == null || filled[r].length != w)
                throw new IllegalArgumentException("Grid must be rectangular");
        }
    }
}
