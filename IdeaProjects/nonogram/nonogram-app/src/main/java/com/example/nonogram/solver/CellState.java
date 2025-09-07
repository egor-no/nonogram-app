package com.example.nonogram.solver;

public enum CellState {
    UNKNOWN(0),
    FILLED(1),
    BLANK(2);

    private final int code;

    CellState(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static CellState fromCode(int code) {
        for (CellState state : values()) {
            if (state.code == code) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown CellState code: " + code);
    }
}
