package com.example.nonogram.api;

import com.example.nonogram.api.dto.CrosswordDto;
import com.example.nonogram.api.dto.SolutionDto;
import com.example.nonogram.core.model.Crossword;
import com.example.nonogram.core.model.Solution;

public final class Mapper {
    private Mapper(){}

    public static Crossword toModel(CrosswordDto dto) {
        return new Crossword(dto.rows(), dto.columns());
    }

    public static CrosswordDto toDto(Crossword model) {
        return new CrosswordDto(model.getRows(), model.getColumns());
    }

    public static SolutionDto toDto(Solution sol) {
        return new SolutionDto(sol.height(), sol.width(), extract(sol));
    }

    private static boolean[][] extract(Solution s) {
        int h = s.height(), w = s.width();
        boolean[][] g = new boolean[h][w];
        for (int r = 0; r < h; r++)
            for (int c = 0; c < w; c++)
                g[r][c] = s.isFilled(r,c);
        return g;
    }
}