package org.example.cflp.cli;

import org.example.cflp.io.FileNames;
import org.example.cflp.io.JsonIO;
import org.example.cflp.model.Instance;
import org.example.cflp.model.InstanceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;

@Command(name = "generate", description = "Generate random instances.")
public class GenerateCommand implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(GenerateCommand.class);

    @Option(names = {"-f", "--facilities"}, defaultValue = "20", description = "Number of facilities (default: ${DEFAULT-VALUE}).")
    int facilities;

    @Option(names = {"-c", "--customers"}, defaultValue = "100", description = "Number of customers (default: ${DEFAULT-VALUE}).")
    int customers;

    @Option(names = "--capacity-ratio", defaultValue = "3.0", description = "Total capacity / total demand (default: ${DEFAULT-VALUE}).")
    double capacityRatio;

    @Option(names = {"-n", "--count"}, defaultValue = "1", description = "Number of instances, with seeds seed, seed+1, ... (default: ${DEFAULT-VALUE}).")
    int count;

    @Option(names = {"-s", "--seed"}, defaultValue = "1", description = "First random seed (default: ${DEFAULT-VALUE}).")
    long seed;

    @Option(names = "--gzip", description = "Write compressed .instance.json.gz files.")
    boolean gzip;

    @Option(names = {"-o", "--output-dir"}, defaultValue = "instances", description = "Output directory (default: ${DEFAULT-VALUE}).")
    Path outputDir;

    @Override
    public void run() {
        for (long s = seed; s < seed + count; s++) {
            String name = "cflp-" + facilities + "-" + customers + "-" + s;
            Instance inst = InstanceGenerator.generate(name, facilities, customers, capacityRatio, s);
            Path file = FileNames.instanceFile(outputDir, name, gzip);
            JsonIO.write(file, inst);
            log.info("Wrote {}", file);
        }
    }
}
