package com.example.nonogram.core;

import com.example.nonogram.core.model.Crossword;
import com.example.nonogram.core.model.Solution;

public interface AntiSolver {
    Crossword antiSolve(Solution solution);
}
