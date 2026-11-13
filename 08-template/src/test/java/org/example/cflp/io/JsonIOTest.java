package org.example.cflp.io;

import org.example.cflp.model.Instance;
import org.example.cflp.model.InstanceGenerator;
import org.example.cflp.model.RunInfo;
import org.example.cflp.model.Solution;
import org.example.cflp.model.SolveStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonIOTest {

    @TempDir
    Path dir;   // fresh, empty directory per test; deleted afterwards

    @Test
    void instanceRoundTrip() {
        Instance original = InstanceGenerator.generate("rt", 5, 20, 3.0, 7);
        Path file = dir.resolve("rt.instance.json");

        JsonIO.write(file, original);
        Instance read = JsonIO.read(file, Instance.class);

        assertEquals(original, read);   // records compare all components
    }

    @Test
    void gzipRoundTrip() {
        Instance original = InstanceGenerator.generate("rt", 5, 20, 3.0, 7);
        Path file = dir.resolve("rt.instance.json.gz");

        JsonIO.write(file, original);

        assertEquals(original, JsonIO.read(file, Instance.class));
    }

    @Test
    void solutionRoundTrip() {
        Instance inst = InstanceGenerator.generate("rt", 3, 4, 3.0, 7);
        RunInfo run = new RunInfo(UUID.randomUUID(), "exp", "label", Map.of("k", "v"),
                Map.of("iterations", 10), Instant.now(), "host", null);
        Solution original = Solution.of(inst, "test", SolveStatus.OPTIMAL, 1.5, new int[]{0, 1, 1, 2}).withRun(run);
        Path file = dir.resolve("rt.solution.json");

        JsonIO.write(file, original);

        assertEquals(original, JsonIO.read(file, Solution.class));
    }
}
