package org.example.cflp.io;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** One shared, thread-safe mapper for the whole application. Files ending in .gz are (de)compressed. */
public final class JsonIO {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private JsonIO() {
    }

    public static void write(Path file, Object value) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            try (OutputStream out = isGzip(file)
                    ? new GZIPOutputStream(Files.newOutputStream(file))
                    : Files.newOutputStream(file)) {
                MAPPER.writeValue(out, value);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T read(Path file, Class<T> type) {
        try (InputStream in = isGzip(file)
                ? new GZIPInputStream(Files.newInputStream(file))
                : Files.newInputStream(file)) {
            return MAPPER.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    /** E.g. a parameter record as a map, to store it in a generic field. */
    public static Map<String, Object> toMap(Object value) {
        return MAPPER.convertValue(value, new TypeReference<>() {
        });
    }

    private static boolean isGzip(Path file) {
        return file.getFileName().toString().endsWith(".gz");
    }
}
