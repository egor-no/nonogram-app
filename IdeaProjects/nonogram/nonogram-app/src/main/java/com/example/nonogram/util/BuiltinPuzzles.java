package com.example.nonogram.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Component
public class BuiltinPuzzles {
    // ищем и верхний, и нижний регистр; classpath* — чтобы цепляло и JAR’ы
    private static final String GLOB_UPPER = "classpath*:/puzzles/*.JPNXML";
    private static final String GLOB_LOWER = "classpath*:/puzzles/*.jpnxml";

    private final ResourcePatternResolver resolver;

    public BuiltinPuzzles(ResourcePatternResolver resolver) {
        this.resolver = resolver;
    }

    /** Список имён без расширения (и без дублей), отсортированный. */
    public List<String> list() throws IOException {
        Resource[] upper = resolver.getResources(GLOB_UPPER);
        Resource[] lower = resolver.getResources(GLOB_LOWER);

        return Stream.concat(Arrays.stream(upper), Arrays.stream(lower))
                .map(Resource::getFilename)
                .filter(Objects::nonNull)
                .map(this::stripExt)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    /** Открыть встроенный файл по имени (без/с расширением, любой регистр). */
    public InputStream open(String name) throws IOException {
        String base = stripExt(name);
        var r1 = new ClassPathResource("puzzles/" + base + ".JPNXML");
        if (r1.exists()) return r1.getInputStream();

        var r2 = new ClassPathResource("puzzles/" + base + ".jpnxml");
        if (r2.exists()) return r2.getInputStream();

        throw new IOException("Builtin puzzle not found: " + base);
    }

    private String stripExt(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot > 0) ? filename.substring(0, dot) : filename;
    }
}
