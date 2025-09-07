package com.example.nonogram.solver;

import com.example.nonogram.core.Solver;
import com.example.nonogram.core.model.Crossword;
import com.example.nonogram.core.model.Solution;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JPNSolver implements Solver {

    private int height;
    private int width;
    private List<List<Integer>> rows;
    private List<List<Integer>> columns;

    private CellState[][] process;
    private List<CellState[]> variants;
    private ArrayList<Integer> solved;

    private int[] amounts;

    private void initNewSolutionProcess(Crossword crossword) {
        height = crossword.height();
        width = crossword.width();
        rows = crossword.getRows();
        columns = crossword.getColumns();
        solved = new ArrayList<>();
        amounts = new int[3];

        process = new CellState[height][width];
        for (int  i = 0; i < height; i++) {
            for (int  j = 0; j < width; j++) {
                process[i][j] = CellState.UNKNOWN;
            }
        }
    }

    @Override public Solution solve(Crossword crossword) {
        initNewSolutionProcess(crossword);

        int a = 0;
        while(!isSolved()) {
            if (isGuessAndCheckNeeded()) {
                if (nonfilledCells() < 20) {
                    resolveByBruteForce();
                } else {
                    backtrackSolve();
                }
                continue;
            }

            for (int i = 0; i < height; i++)
                makeVariations(i, true, a);
            for (int i = 0; i < width; i++)
                makeVariations(i, false, a);
            a++;
        }

        return Solution.of(crossword, toBooleanGrid(process));
    }

    private boolean[][] toBooleanGrid(CellState[][] process) {
        int h = process.length;
        int w = process[0].length;
        boolean[][] grid = new boolean[h][w];

        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                grid[r][c] = (process[r][c] == CellState.FILLED);
            }
        }

        return grid;
    }

    public boolean isSolved() {
        int kol = width * height;
        int kolSolved = 0;

        for (int i = 0; i < height ; i++)
            for (int j = 0; j < width; j++)
                if (process[i][j] != CellState.UNKNOWN)
                    kolSolved++;
        solved.add(kolSolved);

        if (kolSolved == kol)
            return true;
        else
            return false;
    }

    private boolean isGuessAndCheckNeeded() {
        if (solved.size() > 3)
            if (solved.get(solved.size()-1).equals(solved.get(solved.size()-2))
                    && solved.get(solved.size()-2).equals(solved.get(solved.size()-3)))
                    return true;
        return false;
    }

    /**
     * Решает одну линию (строку или столбец) по подсказкам:
     * @param index  номер строки/столбца
     * @param isRow  true — работаем со строкой, false — со столбцом
     * @param phase  0 — быстрые эвристики; 1 — генерация вариантов и консенсус
     */
    private void makeVariations(int index, boolean isRow, int phase) {
        final int lineLen = isRow ? width : height;
        final List<Integer> clues = isRow ? rows.get(index) : columns.get(index);

        // Снимок текущей линии из process
        CellState[] line = new CellState[lineLen];
        for (int i = 0; i < lineLen; i++) {
            int r = isRow ? index : i;
            int c = isRow ? i     : index;
            line[i] = process[r][c];
        }

        if (phase == 0) {
            // Эвристика перекрытий (гарантированные FILLED)
            filledHeuristics(line, clues, isRow, index);

            // Если в линии нет блоков, вся линия BLANK
            if (clues.isEmpty()) {
                for (int i = 0; i < lineLen; i++) {
                    int r = isRow ? index : i;
                    int c = isRow ? i     : index;
                    process[r][c] = CellState.BLANK;
                }
            }
        } else {
            // Генерация всех допустимых вариантов + консенсус по позициям
            if (!lineFilled(line)) {
                variants = new ArrayList<>();
                // рабочий буфер для сборки варианта
                CellState[] work = new CellState[lineLen];
                // инициализируем UNKNOWN
                for (int i = 0; i < lineLen; i++) work[i] = CellState.UNKNOWN;

                generateLineVariants(0, 0, line, work, clues);

                // Фиксируем клетки, одинаковые во всех вариантах
                for (int i = 0; i < lineLen; i++) {
                    CellState consensus = checkConsensusAt(i);
                    if (consensus != CellState.UNKNOWN) {
                        int r = isRow ? index : i;
                        int c = isRow ? i     : index;
                        process[r][c] = consensus;
                    }
                }
            }
        }

        // Вызов эвристики на «краях»
        // если это первая линия ИЛИ последняя по своему измерению
        if (index == 0 || (isRow && index == height - 1) || (!isRow && index == width - 1)) {
            edgeHeuristics();
        }
    }

    /**
     * Рекурсивно генерируем все допустимые варианты линии,
     * соблюдая подсказки и уже известные клетки (line).
     *
     * @param posStart  позиция, с которой можно ставить следующий блок или BLANK
     * @param clueIdx   индекс текущего блока в clues
     * @param baseLine  «база» из process (может содержать FIX/BLANK/UNKNOWN)
     * @param work      формируемый вариант (в конце без UNKNOWN)
     * @param clues     список длин блоков
     */
    private void generateLineVariants(int posStart, int clueIdx,
                                      CellState[] baseLine,
                                      CellState[] work,
                                      List<Integer> clues) {
        final int n = baseLine.length;

        // Все блоки размещены — остаток заполняем BLANK (если не противоречит baseLine)
        if (clueIdx == clues.size()) {
            // Заполнить хвост BLANK
            for (int i = posStart; i < n; i++) {
                if (baseLine[i] == CellState.FILLED) return; // конфликт
                work[i] = CellState.BLANK;
            }
            // Также убедимся, что все ранние позиции определены
            for (int i = 0; i < posStart; i++) {
                if (work[i] == CellState.UNKNOWN) {
                    // если baseLine фиксирует — поставим её значение,
                    // иначе это ранее выставленные значения, здесь не должно быть UNKNOWN
                    work[i] = (baseLine[i] != CellState.UNKNOWN) ? baseLine[i] : CellState.BLANK;
                }
            }
            variants.add(work.clone());
            return;
        }

        // Вычисляем максимально допустимую стартовую позицию для текущего блока
        int blocksLeft = clues.size() - clueIdx;
        int sumRemain = 0;
        for (int t = clueIdx; t < clues.size(); t++) sumRemain += clues.get(t);
        int latestStart = n - (sumRemain + (blocksLeft - 1)); // учёт минимальных 1 BLANK между блоками

        // Попробуем разместить текущий блок длины len начиная с каждой позиции s
        int len = clues.get(clueIdx);

        // Перед началом размещения "дотянем" work до posStart, учитывая baseLine
        for (int i = 0; i < posStart; i++) {
            if (work[i] == CellState.UNKNOWN) {
                if (baseLine[i] == CellState.FILLED) {
                    work[i] = CellState.FILLED; // уважим фиксированное
                } else {
                    work[i] = CellState.BLANK;  // безопасно заполнить
                }
            }
        }

        for (int s = posStart; s <= latestStart; s++) {
            // 1) промежуток перед блоком [posStart..s-1] должен быть BLANK
            boolean ok = true;
            for (int i = posStart; i < s; i++) {
                if (baseLine[i] == CellState.FILLED) { ok = false; break; }
            }
            if (!ok) continue;

            // Проставим BLANK в work на этом участке (с бэктрекингом)
            List<Integer> touched = new ArrayList<>();
            for (int i = posStart; i < s; i++) {
                if (work[i] == CellState.UNKNOWN) {
                    work[i] = CellState.BLANK;
                    touched.add(i);
                } else if (work[i] != CellState.BLANK) {
                    ok = false; break;
                }
            }
            if (!ok) { // откат
                for (int i : touched) work[i] = CellState.UNKNOWN;
                continue;
            }

            // 2) блок [s..s+len-1] должен быть FILLED и совместим с baseLine
            if (s + len > n) {
                for (int i : touched) work[i] = CellState.UNKNOWN;
                continue;
            }
            List<Integer> blockTouched = new ArrayList<>();
            for (int i = s; i < s + len; i++) {
                if (baseLine[i] == CellState.BLANK) { ok = false; break; }
                if (work[i] == CellState.UNKNOWN) {
                    work[i] = CellState.FILLED;
                    blockTouched.add(i);
                } else if (work[i] != CellState.FILLED) {
                    ok = false; break;
                }
            }
            if (!ok) {
                for (int i : blockTouched) work[i] = CellState.UNKNOWN;
                for (int i : touched)      work[i] = CellState.UNKNOWN;
                continue;
            }

            // 3) разделительный BLANK после блока (если есть ещё блоки)
            int nextPos = s + len;
            Integer sepTouched = null;
            if (clueIdx < clues.size() - 1) {
                if (nextPos >= n || baseLine[nextPos] == CellState.FILLED) {
                    // нужен BLANK, но нельзя
                    ok = false;
                } else {
                    if (work[nextPos] == CellState.UNKNOWN) {
                        work[nextPos] = CellState.BLANK;
                        sepTouched = nextPos;
                    } else if (work[nextPos] != CellState.BLANK) {
                        ok = false;
                    }
                }
                nextPos++; // следующий блок начинается после разделителя
            }

            if (ok) {
                generateLineVariants(nextPos, clueIdx + 1, baseLine, work, clues);
            }

            // Откаты
            if (sepTouched != null) work[sepTouched] = CellState.UNKNOWN;
            for (int i : blockTouched) work[i] = CellState.UNKNOWN;
            for (int i : touched)      work[i] = CellState.UNKNOWN;
        }
    }

    /**
     * Эвристика перекрытий: если блок длины L влезает в оставшийся интервал только частично,
     * центральная «перекрывающаяся» часть гарантированно FILLED.
     */
    private void filledHeuristics(CellState[] line, List<Integer> clues, boolean isRow, int index) {
        final int n = line.length;

        int totalClues = clues.size();
        for (int i = 0; i < totalClues; i++) {
            int before = 0;
            for (int j = 0; j < i; j++) before += clues.get(j) + 1; // блок + разделитель
            int after = 0;
            for (int j = i + 1; j < totalClues; j++) after += clues.get(j) + 1;

            int remain = n - after - before;        // сколько места «под текущий блок»
            int L = clues.get(i);
            if (remain < 2 * L) {
                int diff = 2 * L - remain;          // ширина перекрытия
                int start = before + (remain - diff) / 2;
                for (int k = 0; k < diff; k++) {
                    int pos = start + k;
                    int r = isRow ? index : pos;
                    int c = isRow ? pos   : index;
                    process[r][c] = CellState.FILLED;
                    line[pos] = CellState.FILLED; // синхронизируем локальный снимок
                }
            }
        }
    }

    /**
     * Возвращает «консенсус» по позиции k среди всех сгенерированных variants:
     * FILLED/BLANK, если во всех вариантах одинаково; иначе UNKNOWN.
     */
    private CellState checkConsensusAt(int k) {
        if (variants == null || variants.isEmpty()) return CellState.UNKNOWN;
        CellState first = variants.get(0)[k];
        for (int i = 1; i < variants.size(); i++) {
            if (variants.get(i)[k] != first) return CellState.UNKNOWN;
        }
        return first;
    }

    private boolean lineFilled(CellState[] line) {
        for (CellState cs : line) if (cs == CellState.UNKNOWN) return false;
        return true;
    }

    private void edgeHeuristics() {
        int blockLength;

        for (int i = 0; i < width; i++) {
            if (!columns.get(i).isEmpty()) {
                if (process[0][i] == CellState.FILLED) {
                    blockLength = columns.get(i).get(0);
                    for (int j = 1; j < blockLength; j++)
                        process[j][i] = CellState.FILLED;
                    if (blockLength < height)
                        process[blockLength][i] = CellState.BLANK;
                }
                if (process[height-1][i] == CellState.FILLED) {
                    blockLength = columns.get(i).get(columns.get(i).size() - 1);
                    for (int j = 1; j < blockLength; j++)
                        process[height - j - 1][i] = CellState.FILLED;
                    if (blockLength < height)
                        process[height - blockLength - 1][i] = CellState.BLANK;
                }
            }
        }

        for (int i = 0; i < height; i++) {
            if (!rows.get(i).isEmpty()) {
                if (process[i][0] == CellState.FILLED) {
                    blockLength = rows.get(i).get(0);
                    for (int j = 1; j < blockLength; j++)
                        process[i][j] = CellState.FILLED;
                    if (blockLength < width)
                        process[i][blockLength] = CellState.BLANK;
                }
                if (process[i][width-1] == CellState.FILLED) {
                    blockLength = rows.get(i).get(rows.get(i).size() - 1);
                    for (int j = 1; j < blockLength; j++)
                        process[i][width - j - 1] = CellState.FILLED;
                    if (blockLength < width)
                        process[i][width - blockLength - 1] = CellState.BLANK;
                }
            }
        }
    }

    private void resolveByBruteForce() {
        List<int[]> index = new ArrayList<>();
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                if (process[r][c] == CellState.UNKNOWN) {
                    index.add(new int[]{r, c});
                }
            }
        }
        subgenerate(0, index);
    }

    private boolean subgenerate(int pos, List<int[]> index) {
        int size = index.size();
        if (pos == size)
            return checkBruteforceConsistency(index);

        int[] rc = index.get(pos);
        int r = rc[0], c = rc[1];

        for (int code = 1; code <= 2; code++) { // 1 = FILLED, 2 = BLANK
            process[r][c] = CellState.fromCode(code);
            if (subgenerate(pos + 1, index)) {
                return true;
            }
        }

        process[r][c] = CellState.UNKNOWN;
        return false;
    }

    private boolean checkBruteforceConsistency(List<int[]> index) {
        if (index == null || index.isEmpty()) {
            return true;
        }

        boolean[] rowsToCheck = new boolean[height];
        boolean[] colsToCheck = new boolean[width];
        for (int[] rc : index) {
            int r = rc[0];
            int c = rc[1];
            if (r >= 0 && r < height) rowsToCheck[r] = true;
            if (c >= 0 && c < width)  colsToCheck[c] = true;
        }

        for (int r = 0; r < height; r++) {
            if (rowsToCheck[r] && !rowMatchesClues(r)) {
                return false;
            }
        }

        for (int c = 0; c < width; c++) {
            if (colsToCheck[c] && !colMatchesClues(c)) {
                return false;
            }
        }

        return true;
    }

    private boolean rowMatchesClues(int r) {
        List<Integer> runs = new ArrayList<>();
        int j = 0;
        while (j < width) {
            if (process[r][j] == CellState.FILLED) {
                int k = 0;
                while (j < width && process[r][j] == CellState.FILLED) {
                    k++; j++;
                }
                runs.add(k);
            } else {
                j++;
            }
        }

        List<Integer> clue = rows.get(r);
        if (runs.size() != clue.size())
            return false;
        for (int i = 0; i < runs.size(); i++) {
            if (!runs.get(i).equals(clue.get(i)))
                return false;
        }
        return true;
    }

    private boolean colMatchesClues(int c) {
        List<Integer> runs = new ArrayList<>();
        int i = 0;
        while (i < height) {
            if (process[i][c] == CellState.FILLED) {
                int k = 0;
                while (i < height && process[i][c] == CellState.FILLED) {
                    k++; i++;
                }
                runs.add(k);
            } else {
                i++;
            }
        }

        List<Integer> clue = columns.get(c);
        if (runs.size() != clue.size())
            return false;
        for (int t = 0; t < runs.size(); t++) {
            if (!runs.get(t).equals(clue.get(t)))
                return false;
        }
        return true;
    }

    private void backtrackSolve() {
        CellState[][] reserve = new CellState[height][width];
        copyGrid(process, reserve);

        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                if (process[j][i] == CellState.UNKNOWN) {
                    int temp = solved.get(solved.size()-1);
                    solved.clear();
                    solved.add(temp);

                    process[j][i] = CellState.FILLED;
                    do {
                        for (int f = 0; f < height; f++)
                            makeVariations(f, true, 1);
                        for (int f = 0; f < width; f++)
                            makeVariations(f, false, 1);
                    }
                    while(!isSolved() && !isGuessAndCheckNeeded());

                    if (!checkBacktrackSolve()) {
                        copyGrid(reserve, process);
                        process[j][i] = CellState.BLANK;
                        do {
                            for (int f = 0; f < height; f++)
                                makeVariations(f, true, 1);
                            for (int f = 0; f < width; f++)
                                makeVariations(f, false, 1);
                        }
                        while(!isSolved() && !isGuessAndCheckNeeded());
                    }
                    return;
                }
    }

    private boolean checkBacktrackSolve() {
        for (int r = 0; r < height; r++) {
            List<Integer> expected = rows.get(r);
            List<Integer> actual = extractBlocks(process[r]);
            if (!expected.equals(actual)) {
                return false;
            }
        }

        for (int c = 0; c < width; c++) {
            List<Integer> expected = columns.get(c);
            List<Integer> actual = extractBlocks(getColumn(c));
            if (!expected.equals(actual)) {
                return false;
            }
        }

        return true;
    }

    private List<Integer> extractBlocks(CellState[] line) {
        List<Integer> blocks = new ArrayList<>();
        int count = 0;
        for (CellState cell : line) {
            if (cell == CellState.FILLED) {
                count++;
            } else {
                if (count > 0) {
                    blocks.add(count);
                    count = 0;
                }
            }
        }
        if (count > 0) {
            blocks.add(count);
        }
        return blocks;
    }

    private CellState[] getColumn(int c) {
        CellState[] col = new CellState[height];
        for (int r = 0; r < height; r++) {
            col[r] = process[r][c];
        }
        return col;
    }

    private int nonfilledCells() {
        int kol = 0;
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++ )
                if (process[i][j] == CellState.UNKNOWN)
                    kol++;
        return kol;
    }

    private void copyGrid(CellState[][] source, CellState[][] target) {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                target[i][j] = source[i][j];
            }
        }
    }
}
