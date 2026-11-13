package org.example.cflp.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** File naming conventions: name.instance.json and name.solver.solution.json */
public final class FileNames {

    public static final String INSTANCE_SUFFIX = ".instance.json";
    public static final String SOLUTION_SUFFIX = ".solution.json";

    private FileNames() {
    }

    public static Path instanceFile(Path dir, String instanceName) {
        return dir.resolve(instanceName + INSTANCE_SUFFIX);
    }

    public static Path solutionFile(Path dir, String instanceName, String solver) {
        return dir.resolve(instanceName + "." + solver + SOLUTION_SUFFIX);
    }

    /** Expands directories to the instance files they contain; files are kept as is. */
    public static List<Path> expandInstances(List<Path> paths) {
        List<Path> result = new ArrayList<>();
        for (Path p : paths) {
            if (Files.isDirectory(p)) {
                try (Stream<Path> files = Files.list(p)) {
                    files.filter(f -> f.getFileName().toString().endsWith(INSTANCE_SUFFIX))
                            .sorted()
                            .forEach(result::add);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                result.add(p);
            }
        }
        return result;
    }
}
