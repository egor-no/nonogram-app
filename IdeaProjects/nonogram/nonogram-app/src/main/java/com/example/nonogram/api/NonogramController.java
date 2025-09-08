package com.example.nonogram.api;

import com.example.nonogram.api.dto.CrosswordDto;
import com.example.nonogram.api.dto.GridDto;
import com.example.nonogram.api.dto.SolutionDto;
import com.example.nonogram.core.AntiSolver;
import com.example.nonogram.core.Solver;
import com.example.nonogram.core.model.Crossword;
import com.example.nonogram.core.model.Solution;
import com.example.nonogram.service.NonogramService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api")
public class NonogramController {

    private final Solver solver;
    private final AntiSolver antiSolver;
    private final NonogramService service;

    public NonogramController(Solver solver, AntiSolver antiSolver, NonogramService service) {
        this.solver = solver;
        this.antiSolver = antiSolver;
        this.service = service;
    }

    @PostMapping(value = "/solve", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SolutionDto solve(@Valid @RequestBody CrosswordDto dto) {
        Crossword cw = Mapper.toModel(dto);
        Solution sol = solver.solve(cw);
        return Mapper.toDto(sol);
    }

    @PostMapping(value = "/solve/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SolutionDto solveUpload(@RequestPart("file") MultipartFile file) throws Exception {
        try (InputStream in = file.getInputStream()) {
            var result = service.readAndSolve(in);
            return Mapper.toDto(result.solution());
        }
    }

    @PostMapping(value = "/antisolve", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CrosswordDto antiSolve(@Valid @RequestBody GridDto grid) {
        var cw = antiSolver.antiSolve(grid.filled());
        return Mapper.toDto(cw);
    }

    @PostMapping(value = "/export/jpnxml",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> exportJpnXml(@Valid @RequestBody GridDto grid) throws IOException {
        var cw   = antiSolver.antiSolve(grid.filled());
        var data = service.writeToBytes(cw);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header("Content-Disposition", "attachment; filename=\"nonogram.jpnxml\"")
                .body(data);
    }
}
