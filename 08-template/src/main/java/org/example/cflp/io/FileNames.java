package org.example.cflp.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * File naming conventions:
 * <pre>
 * instances/&lt;instance&gt;.instance.json[.gz]
 * results/&lt;experiment&gt;/&lt;instance&gt;.&lt;label&gt;.solution.json
 * </pre>
 */
public final class FileNames {

    public static final String INSTANCE_SUFFIX = ".instance.json";
    public static final String SOLUTION_SUFFIX = ".solution.json";

    private FileNames() {
    }

    public static Path instanceFile(Path dir, String instanceName, boolean gzip) {
        return dir.resolve(instanceName + INSTANCE_SUFFIX + (gzip ? ".gz" : ""));
    }

    public static Path solutionFile(Path dir, String instanceName, String label) {
        return dir.resolve(instanceName + "." + label + SOLUTION_SUFFIX);
    }

    public static boolean isInstance(Path file) {
        return hasSuffix(file, INSTANCE_SUFFIX);
    }

    public static boolean isSolution(Path file) {
        return hasSuffix(file, SOLUTION_SUFFIX);
    }

    /** Expands directories (recursively) to the files matching the filter; files are kept as is. */
    public static List<Path> expand(List<Path> paths, java.util.function.Predicate<Path> filter) {
        List<Path> result = new ArrayList<>();
        for (Path p : paths) {
            if (Files.isDirectory(p)) {
                try (Stream<Path> files = Files.walk(p)) {
                    files.filter(Files::isRegularFile).filter(filter).sorted().forEach(result::add);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                result.add(p);
            }
        }
        return result;
    }

    private static boolean hasSuffix(Path file, String suffix) {
        String name = file.getFileName().toString();
        return name.endsWith(suffix) || name.endsWith(suffix + ".gz");
    }
}
