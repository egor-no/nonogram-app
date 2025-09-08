package com.example.nonogram.util;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Component
public class BuiltinPuzzles {
    private static final String GLOB = "classpath:/puzzles/*.jpnxml";
    private static final String BASE = "classpath:/puzzles/";
    private final ResourcePatternResolver resolver;

    public BuiltinPuzzles(ResourcePatternResolver resolver) {
        this.resolver = resolver;
    }

    /** Список имён без расширения */
    public List<String> list() throws IOException {
        Resource[] resources = resolver.getResources(GLOB);
        return Arrays.stream(resources)
                .map(Resource::getFilename)
                .filter(fn -> fn != null && fn.endsWith(".jpnxml"))
                .map(fn -> fn.substring(0, fn.length() - ".jpnxml".length()))
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    /** Открыть встроенный файл по имени (можно с/без .jpnxml). */
    public InputStream open(String name) throws IOException {
        String file = name.endsWith(".jpnxml") ? name : name + ".jpnxml";
        Resource r = resolver.getResource(BASE + file);
        if (!r.exists()) {
            throw new IOException("Builtin puzzle not found: " + file);
        }
        return r.getInputStream();
    }
}