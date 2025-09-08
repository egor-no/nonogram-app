package com.example.nonogram.core;

import com.example.nonogram.core.model.Crossword;

public interface AntiSolver {
    Crossword fromFilled(boolean[][] filled);
}
