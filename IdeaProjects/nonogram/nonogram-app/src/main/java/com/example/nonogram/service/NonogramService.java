package com.example.nonogram.service;
import com.example.nonogram.core.AntiSolver;
import com.example.nonogram.core.io.JpnXmlReader;
import com.example.nonogram.core.io.JpnXmlWriter;
import com.example.nonogram.core.model.Crossword;
import com.example.nonogram.core.model.Solution;
import com.example.nonogram.core.Solver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NonogramService {
    private final JpnXmlReader reader;
    private final Solver solver;
    private final AntiSolver antiSolver;
    private final JpnXmlWriter writer;

    public NonogramService(JpnXmlReader reader, Solver solver,
                           AntiSolver antiSolver, JpnXmlWriter writer) {
        this.reader = reader;
        this.solver = solver;
        this.antiSolver = antiSolver;
        this.writer = writer;
    }

    public Result readAndSolve(InputStream in) {
        Crossword cw = reader.read(in);
        Solution sol = solver.solve(cw);
        return new Result(cw, sol);
    }

    public Result readAndSolve(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return readAndSolve(in);
        }
    }

    public void saveAsJpnXml(Solution solution, OutputStream out) {
        boolean[][] grid = toGrid(solution);
        Crossword cw = antiSolver.antiSolve(grid);
        writer.write(cw, out);
    }

    private static boolean[][] toGrid(Solution s) {
        int h = s.height(), w = s.width();
        boolean[][] g = new boolean[h][w];
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                g[r][c] = s.isFilled(r, c);
            }
        }
        return g;
    }

    public void saveAsJpnXml(Solution solution, Path path) throws IOException {
        try (OutputStream out = Files.newOutputStream(path)) {
            saveAsJpnXml(solution, out);
        }
    }

    public record Result(Crossword crossword, Solution solution) {}
}