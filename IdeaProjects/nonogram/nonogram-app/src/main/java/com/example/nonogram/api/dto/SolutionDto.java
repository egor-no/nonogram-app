package com.example.nonogram.api.dto;

import jakarta.validation.constraints.NotNull;

public record SolutionDto(
        int height,
        int width,
        @NotNull boolean[][] filled
) {}