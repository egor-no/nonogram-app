package com.example.nonogram.solver;

import com.example.nonogram.core.Solver;
import com.example.nonogram.core.model.Crossword;
import com.example.nonogram.core.model.Solution;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class JPNSolver implements Solver {

    // ---- Данные кроссворда ----
    private int height;
    private int width;
    private List<List<Integer>> rows;
    private List<List<Integer>> columns;

    // ---- Состояние решения ----
    private CellState[][] process;                     // 0=UNKNOWN, 1=FILLED, 2=BLANK
    private List<CellState[]> variants;                // список допустимых вариантов для текущей линии
    private ArrayList<Integer> solved;                 // история прогресса (кол-во НЕ-UNKNOWN)

    // ---- Рабочие поля (как в старом Solvation) ----
    private int[] amounts = new int[3];                // [кол-во блоков, кол-во пустых, суммарно]
    private ArrayList<Integer> numbers;                // текущие числа подсказки для линии
    private int variantPointer = 0;                    // p
    private int variantsPrinted = 0;                   // printed
    private int size;                                  // длина текущей линии
    private int number;                                // индекс текущей линии
    private int side;                                  // 0 = row/left, 1 = col/top
    private boolean solutionFound;                     // для brute-force ветки
    private byte[] now;                                // слепок линии: 0,1,2

    // ---- Solver API ----
    @Override
    public Solution solve(Crossword crossword) {
        initNewSolutionProcess(crossword);

        // РОВНО как в твоём старом коде:
        int a = 0;
        while (!isSolved(false)) {
            for (int i = 0; i < height; i++)
                makeVariations(i, 0, a);      // строки
            for (int i = 0; i < width; i++)
                makeVariations(i, 1, a);      // колонки
            a++;

            // маленькая пауза из старого кода не нужна в сервере; логически опустим
        }

        return Solution.of(crossword, toBooleanGrid(process));
    }

    // ---- Инициализация ----
    private void initNewSolutionProcess(Crossword crossword) {
        height = crossword.height();
        width = crossword.width();
        rows = crossword.getRows();
        columns = crossword.getColumns();

        solved = new ArrayList<>();
        Arrays.fill(amounts, 0);

        process = new CellState[height][width];
        for (int r = 0; r < height; r++) {
            Arrays.fill(process[r], CellState.UNKNOWN);
        }
    }

    private boolean[][] toBooleanGrid(CellState[][] grid) {
        boolean[][] out = new boolean[height][width];
        for (int r = 0; r < height; r++)
            for (int c = 0; c < width; c++)
                out[r][c] = (grid[r][c] == CellState.FILLED);
        return out;
    }

    // ---- Перенос makeVariations из старого кода (без TableModel) ----
    public void makeVariations(int n, int side, int before) {
        // reset принтеров вариантов
        variantPointer = 0;
        variantsPrinted = 0;

                    this.side = side;     // 0=row, 1=col
                    this.number = n;

                    // Подтянуть numbers и now/size из process
                    if (side == 0) { // row/left
                        numbers = new ArrayList<>(rows.get(n));
                        size = width;
                        now = new byte[size];
                        for (int i = 0; i < size; i++) {
                            CellState cs = process[n][i];
                            now[i] = toCode(cs);
                        }
                    } else { // col/top
                        numbers = new ArrayList<>(columns.get(n));
                        size = height;
                        now = new byte[size];
                        for (int i = 0; i < size; i++) {
                            CellState cs = process[i][n];
                            now[i] = toCode(cs);
                        }
                    }

                    if (before == 0) {
                        // findBlack (перекрытия)
                        findBlack();

                        // если пустая подсказка — вся линия BLANK
                        if (numbers.size() == 0) {
                            for (int i = 0; i < size; i++) {
                                putCell(side, number, i, CellState.BLANK);
                            }
                        }
                    } else if (!isLineFilled(side, number)) {
                        // расчёт amounts как в старом коде
                        amounts[0] = numbers.size();
                        int sum = 0;
                        for (int i = 0; i < amounts[0]; i++) sum += numbers.get(i);

                        amounts[1] = size - sum - (amounts[0] - 1); // пустые клетки
                        amounts[2] = amounts[1] + amounts[0];

                        variants = new ArrayList<>();
                        // первый буфер-вариант
                        variants.add(new CellState[size]);
                        Arrays.fill(variants.get(0), null);

                        // генерация перестановок по старой логике
                        solver(0);

                        // Консенсус по позициям (аналог check(i) + проставление в table)
                        if (!variants.isEmpty()) {
                            int cnt = variants.get(0).length;
                            for (int i = 0; i < cnt; i++) {
                                CellState consensus = consensusAt(i);
                    if (consensus != null) {
                        if (side == 0) {
                            putCell(0, number, i, consensus);
                        } else {
                            putCell(1, i, number, consensus);
                        }
                    } else {
                        // неизвестно
                        if (side == 0) putCell(0, number, i, CellState.UNKNOWN);
                        else putCell(1, i, number, CellState.UNKNOWN);
                    }
                }
            }

            // эвристика краёв как в старом: для первой/последней линии измерения
//            if (n == 0 || (side == 0 && n == width - 1) || (side == 1 && n == height - 1))
//                heuristics();

            if (n == 0 || (side == 0 && n == height - 1) || (side == 1 && n == width - 1))
                heuristics();

            variants.clear();
            variants = null;
        }
    }

    // ---- Генератор вариантов (старая логика solver) ----
    private void solver(int pos) {
        boolean done;
        int k = 0;

        if (pos == size) {
            printVariant();
            return;
        }

        for (int blockOrBlank = 0; blockOrBlank < 2; blockOrBlank++) {
            done = false;
            if (amounts[blockOrBlank] != 0) {
                if (blockOrBlank == 0) {
                    // ставим БЛОК
                    k = numbers.get(numbers.size() - amounts[0]); // длина текущего блока
                    boolean ok = true;
                    // внутри блока не должно быть BLANK в now
                    for (int j = 0; j < k; j++) {
                        if (pos + j >= size || now[pos + j] == 2) { ok = false; break; }
                    }
                    // если будет ещё блок позже — между ними обязательно BLANK (и он не может быть FILLED в now)
                    if (amounts[0] != 1) {
                        if (pos + k >= size || now[pos + k] == 1) ok = false;
                    }
                    if (ok) {
                        done = true;
                        // проставим FILLED
                        for (int j = 0; j < k; j++, pos++) {
                            variants.get(variantPointer)[pos] = CellState.FILLED;
                        }
                        // разделительный BLANK (если не последний блок)
                        if (amounts[0] != 1) {
                            variants.get(variantPointer)[pos] = CellState.BLANK;
                        } else {
                            // если последний блок — откат pos на 1 и k на 1, как в старом коде
                            pos--;
                            k--;
                        }
                        amounts[blockOrBlank]--;
                    } else {
                        continue;
                    }
                } else {
                    // ставим ПУСТУЮ клетку
                    if (now[pos] != 1) { // в now не может быть принудительно FILLED
                        variants.get(variantPointer)[pos] = CellState.BLANK;
                        done = true;
                        amounts[blockOrBlank]--;
                    } else {
                        continue;
                    }
                }

                solver(pos + 1);

                if (done) {
                    // откаты счётчиков и pos
                    amounts[blockOrBlank]++;
                    if (blockOrBlank == 0) {
                        pos -= k;
                    }
                }
            }
        }
    }

    // ---- Перекрытия (findBlack) как в старом коде ----
    private void findBlack() {
        int before, after, remain, diff;
        int cnt = numbers.size();

        for (int i = 0; i < cnt; i++) {
            before = 0;
            after = 0;
            for (int j = 0; j < i; j++) before += numbers.get(j) + 1;
            for (int j = i + 1; j < cnt; j++) after += numbers.get(j) + 1;

            remain = size - after - before;              // сколько остаётся под текущий блок
            int L = numbers.get(i);
            if (remain < L * 2) {
                diff = L * 2 - remain;                   // ширина обязательного перекрытия
                for (int l = 0, k = before + (remain - diff) / 2; l < diff; k++, l++) {
                    if (side == 0) {
                        putCell(0, number, k, CellState.FILLED);
                    } else {
                        putCell(1, k, number, CellState.FILLED);
                    }
                }
            }
        }
    }

    // ---- Краевые эвристики (heuristics) как в старом коде ----
    private void heuristics() {
        // колонки
        for (int i = 0; i < width; i++) {
            if (!columns.get(i).isEmpty()) {
                if (process[0][i] == CellState.FILLED) {
                    int n = columns.get(i).get(0);
                    for (int j = 1; j < n && j < height; j++)
                        process[j][i] = CellState.FILLED;
                    if (n < height) process[n][i] = CellState.BLANK;
                }
                if (process[height - 1][i] == CellState.FILLED) {
                    int n = columns.get(i).get(columns.get(i).size() - 1);
                    for (int j = 1; j <= n && j < height; j++)
                        process[height - j][i] = CellState.FILLED;
                    if (n < height) process[height - n - 1][i] = CellState.BLANK;
                }
            }
        }
        // строки
        for (int i = 0; i < height; i++) {
            if (!rows.get(i).isEmpty()) {
                if (process[i][0] == CellState.FILLED) {
                    int n = rows.get(i).get(0);
                    for (int j = 1; j < n && j < width; j++)
                        process[i][j] = CellState.FILLED;
                    if (n < width) process[i][n] = CellState.BLANK;
                }
                if (process[i][width - 1] == CellState.FILLED) {
                    int n = rows.get(i).get(rows.get(i).size() - 1);
                    for (int j = 2; j <= n && j <= width; j++)
                        process[i][width - j] = CellState.FILLED;
                    if (n < width) process[i][width - n - 1] = CellState.BLANK;
                }
            }
        }
    }

    // ---- Старый check() заменим на консенсус по variants ----
    private CellState consensusAt(int k) {
        if (variants == null || variants.isEmpty()) return null;
        // удалим «незаполненный» хвост последнего варианта (как в старом if (!variants.isEmpty()&&variants.get(last)[k]==null) remove)
        int last = variants.size() - 1;
        boolean tailNull = (variants.get(last)[k] == null);
        if (tailNull) {
            variants.remove(last);
            if (variants.isEmpty()) return null;
        }

        CellState first = variants.get(0)[k];
        for (int i = 1; i < variants.size(); i++) {
            if (variants.get(i)[k] != first) {
                return CellState.UNKNOWN;
            }
        }
        return first; // FILLED или BLANK
    }

    private void printVariant() {
        variantsPrinted++;
        if (variantPointer >= variants.size()) {
            // Теоретически не должно случаться, но пусть будет безопасно.
            variants.add(new CellState[size]);
        }
        CellState[] src = variants.get(variantPointer);
        if (src == null) src = new CellState[size];
        int len = src.length;

        CellState[] dst;
        if (variantPointer < variants.size() - 1) {
            dst = variants.get(variantPointer + 1);
            if (dst == null || dst.length != len) {
                dst = new CellState[len];
                variants.set(variantPointer + 1, dst);
            }
        } else {
            dst = new CellState[len];
            variants.add(dst);
        }

        if (len > 0) System.arraycopy(src, 0, dst, 0, len);
        variantPointer++;
    }

    // ---- Проверки завершения / застой / поиск (ровно как раньше) ----
    public boolean isSolved(boolean search) {
        int total = width * height;
        int solvedCells = 0;

        for (int r = 0; r < height; r++)
            for (int c = 0; c < width; c++)
                if (process[r][c] != CellState.UNKNOWN)
                    solvedCells++;
        solved.add(solvedCells);

        if (solved.size() > 3) {
            int n = solved.size();
            if (solved.get(n - 1).equals(solved.get(n - 2)) &&
                    solved.get(n - 2).equals(solved.get(n - 3))) {
                if (!search) {
                    solutionFound = false;
                    search();
                    if (solutionFound) solvedCells = total;
                } else {
                    return true;
                }
            }
        }
        return solvedCells == total;
    }

    private void search() {
        if (nonfilledCells() < 20) {
            // Полный перебор по всем UNKNOWN
            ArrayList<int[]> index = new ArrayList<>();
            for (int c = 0; c < width; c++)
                for (int r = 0; r < height; r++)
                    if (process[r][c] == CellState.UNKNOWN)
                        index.add(new int[]{r, c});
            solutionFound = false;
            subgenerate(0, index, index.size());
            return;
        }

        // Бэктрекинг: берём первую UNKNOWN-клетку и пробуем FILLED/BLANK
        CellState[][] reserve = cloneGrid(process);

        outer:
        for (int c = 0; c < width; c++) {
            for (int r = 0; r < height; r++) {
                if (process[r][c] != CellState.UNKNOWN) continue;

                // обновим историю прогресса (как в старом коде)
                int last = solved.isEmpty() ? 0 : solved.get(solved.size() - 1);
                solved.clear();
                solved.add(last);

                // -------- попытка 1: FILLED --------
                process[r][c] = CellState.FILLED;
                int prev = -1;
                do {
                    int cur = solved.isEmpty() ? 0 : solved.get(solved.size() - 1);
                    if (cur == prev) break; // нет прогресса — выходим из do/while
                    prev = cur;

                    for (int f = 0; f < height; f++) makeVariations(f, 0, 1);
                    for (int f = 0; f < width;  f++) makeVariations(f, 1, 1);
                } while (!isSolved(true));

                // если полная проверка прошла — фиксируем и выходим
                if (checkBigSearch()) {
                    break outer;
                }

                // -------- попытка 2: BLANK --------
                copyGrid(reserve, process);

                // сброс прогресса как в старом: сохраняем только последнюю метрику
                last = solved.isEmpty() ? 0 : solved.get(solved.size() - 1);
                solved.clear();
                solved.add(last);

                process[r][c] = CellState.BLANK;
                prev = -1;
                do {
                    int cur = solved.isEmpty() ? 0 : solved.get(solved.size() - 1);
                    if (cur == prev) break; // нет прогресса — выходим из do/while
                    prev = cur;

                    for (int f = 0; f < height; f++) makeVariations(f, 0, 1);
                    for (int f = 0; f < width;  f++) makeVariations(f, 1, 1);
                } while (!isSolved(true));

                // если полная проверка прошла — фиксируем и выходим
                if (checkBigSearch()) {
                    break outer;
                }

                // Обе ветки не дали валидной полной разметки — откатываем
                copyGrid(reserve, process);
                break outer;
            }
        }
    }


    private void subgenerate(int pos, ArrayList<int[]> index, int size) {
        if (pos == size) {
            solutionFound = checkSearch(index);
            return;
        }
        if (!solutionFound) {
            for (int code = 1; code < 3 && !solutionFound; code++) {
                int r = index.get(pos)[0], c = index.get(pos)[1];
                process[r][c] = CellState.fromCode(code); // 1=FILLED, 2=BLANK
                subgenerate(pos + 1, index, size);
            }
        }
    }

    private boolean checkSearch(ArrayList<int[]> index) {
        // Проверка строк, затронутых в index
        int k, p;
        boolean ok = true;

        for (int i = 0; i < index.size(); i++) {
            int row = index.get(i)[0];
            p = 0;
            for (int j = 0; j < width; j++) {
                if (process[row][j] == CellState.FILLED) {
                    k = 0;
                    while (j < width && process[row][j] == CellState.FILLED) { k++; j++; }
                    if (p >= rows.get(row).size() || !rows.get(row).get(p).equals(k)) {
                        ok = false; break;
                    }
                    p++;
                }
            }
            if (!ok) break;
        }
        if (!ok) return false;

        // Проверка колонок, затронутых в index
        for (int i = 0; i < index.size(); i++) {
            int col = index.get(i)[1];
            p = 0;
            for (int j = 0; j < height; j++) {
                if (process[j][col] == CellState.FILLED) {
                    k = 0;
                    while (j < height && process[j][col] == CellState.FILLED) { k++; j++; }
                    if (p >= columns.get(col).size() || !columns.get(col).get(p).equals(k)) {
                        ok = false; break;
                    }
                    p++;
                }
            }
            if (!ok) break;
        }
        return ok;
    }

    private boolean checkBigSearch() {
        // Полная проверка всех строк
        for (int r = 0; r < height; r++) {
            List<Integer> runs = extractRuns(process[r]);
            if (runs.size() != rows.get(r).size()) return false;
            for (int i = 0; i < runs.size(); i++)
                if (!runs.get(i).equals(rows.get(r).get(i))) return false;
        }
        // Полная проверка всех колонок
        for (int c = 0; c < width; c++) {
            List<Integer> runs = extractRuns(getCol(c));
            if (runs.size() != columns.get(c).size()) return false;
            for (int i = 0; i < runs.size(); i++)
                if (!runs.get(i).equals(columns.get(c).get(i))) return false;
        }
        return true;
    }

    // ---- Вспомогательное ----
    private boolean isLineFilled(int side, int idx) {
        if (side == 0) {
            for (int c = 0; c < width; c++) if (process[idx][c] == CellState.UNKNOWN) return false;
        } else {
            for (int r = 0; r < height; r++) if (process[r][idx] == CellState.UNKNOWN) return false;
        }
        return true;
    }

    private List<Integer> extractRuns(CellState[] line) {
        List<Integer> runs = new ArrayList<>();
        int k = 0;
        for (CellState cs : line) {
            if (cs == CellState.FILLED) k++;
            else if (k > 0) { runs.add(k); k = 0; }
        }
        if (k > 0) runs.add(k);
        return runs;
    }

    private int nonfilledCells() {
        int cnt = 0;
        for (int r = 0; r < height; r++)
            for (int c = 0; c < width; c++)
                if (process[r][c] == CellState.UNKNOWN) cnt++;
        return cnt;
    }

    private CellState[] getCol(int c) {
        CellState[] col = new CellState[height];
        for (int r = 0; r < height; r++) col[r] = process[r][c];
        return col;
    }

    private void putCell(int side, int row, int col, CellState s) {
        if (side == 0) { // фиксируем строку (row = номер строки, col = позиция в строке)
            process[row][col] = s;
        } else {         // фиксируем колонку (col = номер колонки, row = позиция в колонке)
            process[row][col] = s;   // ВАЖНО: именно [row][col], не наоборот
        }
    }

    private CellState[][] cloneGrid(CellState[][] src) {
        CellState[][] dst = new CellState[height][width];
        copyGrid(src, dst);
        return dst;
    }

    private void copyGrid(CellState[][] src, CellState[][] dst) {
        for (int r = 0; r < height; r++) {
            System.arraycopy(src[r], 0, dst[r], 0, width);
        }
    }

    private byte toCode(CellState cs) {
        if (cs == CellState.FILLED) return 1;
        if (cs == CellState.BLANK)  return 2;
        return 0;
    }
}