package com.example.nonogram.api.dto;

import jakarta.validation.constraints.NotNull;

public record GridDto(@NotNull boolean[][] filled) {}