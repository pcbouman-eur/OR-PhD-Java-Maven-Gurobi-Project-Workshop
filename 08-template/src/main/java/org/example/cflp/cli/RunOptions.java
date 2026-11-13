package org.example.cflp.cli;

import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Where results go and how runs are labelled; shared by 'solve' and 'batch'. */
public class RunOptions {

    @Option(names = {"-o", "--output-dir"}, defaultValue = "results", description = "Output directory (default: ${DEFAULT-VALUE}).")
    Path outputDir;

    @Option(names = {"-e", "--experiment"}, defaultValue = "default",
            description = "Experiment name; results go to <output-dir>/<experiment>/ (default: ${DEFAULT-VALUE}).")
    String experiment;

    @Option(names = {"-l", "--label"}, description = "Name of this variant, used in file names (default: solver name).")
    String label;

    @Option(names = {"-t", "--tag"}, description = "Tag as key=value, repeatable: -t cooling=slow -t operators=all")
    Map<String, String> tags = new LinkedHashMap<>();

    Path experimentDir() {
        return outputDir.resolve(experiment);
    }

    String labelOr(String solverName) {
        return label != null ? label : solverName;
    }
}
