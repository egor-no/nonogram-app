package com.example.nonogram.core.model;

import java.util.Objects;

public final class Solution {
    private final Crossword crossword;
    private final boolean[][] filled;

    private Solution(Crossword crossword, boolean[][] filled) {
        this.crossword = Objects.requireNonNull(crossword);
        this.filled = Objects.requireNonNull(filled);
        int h = crossword.height(), w = crossword.width();
        if (filled.length != h) throw new IllegalArgumentException("Bad height");
        for (int r = 0; r < h; r++)
            if (filled[r] == null || filled[r].length != w) throw new IllegalArgumentException("Bad width at row "+r);
    }

    public static Solution empty(Crossword cw) {
        return new Solution(cw, new boolean[cw.height()][cw.width()]);
    }

    public static Solution of(Crossword cw, boolean[][] grid) {
        return new Solution(cw, grid);
    }

    public Crossword crossword() {
        return crossword;
    }

    public int height() {
        return crossword.height();
    }

    public int width()  {
        return crossword.width();
    }

    public boolean isFilled(int r, int c) {
        return filled[r][c];
    }

    public void setFilled(int r, int c, boolean v) {
        filled[r][c] = v;
    }
}