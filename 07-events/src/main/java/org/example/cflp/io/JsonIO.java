package org.example.cflp.io;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** One shared, thread-safe mapper for the whole application. */
public final class JsonIO {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private JsonIO() {
    }

    public static void write(Path file, Object value) {
        createParentDirs(file);
        MAPPER.writeValue(file.toFile(), value);
    }

    public static <T> T read(Path file, Class<T> type) {
        return MAPPER.readValue(file.toFile(), type);
    }

    private static void createParentDirs(Path file) {
        Path parent = file.toAbsolutePath().getParent();
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
