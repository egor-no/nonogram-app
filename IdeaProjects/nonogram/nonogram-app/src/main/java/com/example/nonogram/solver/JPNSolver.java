package com.example.nonogram.solver;

import com.example.nonogram.core.Solver;
import com.example.nonogram.core.model.Crossword;
import com.example.nonogram.core.model.Solution;
import org.springframework.stereotype.Component;

@Component
public class JPNSolver implements Solver {

    @Override public Solution solve(Crossword crossword) {
        // алгоритм
        throw new UnsupportedOperationException("Implement your solver here");
    }
}
