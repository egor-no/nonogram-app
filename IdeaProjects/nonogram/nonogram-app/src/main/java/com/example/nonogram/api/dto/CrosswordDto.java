package com.example.nonogram.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CrosswordDto(
        @NotNull @NotEmpty List<@NotNull @NotEmpty List<@NotNull Integer>> rows,
        @NotNull @NotEmpty List<@NotNull @NotEmpty List<@NotNull Integer>> columns
) {}