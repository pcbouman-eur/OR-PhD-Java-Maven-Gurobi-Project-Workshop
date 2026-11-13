package org.example.cflp;

import org.example.cflp.cli.App;

/**
 * Click "Run" in the IDE: executes the same commands you would type in a terminal,
 * but with breakpoints and the debugger available. Edit freely, this is your scratch pad.
 */
public class DevMain {

    public static void main(String[] args) {
        run("generate", "--count", "3", "--facilities", "30", "--customers", "150", "-o", "work/instances");
        // Two treatments of one experiment, distinguished by label and tag
        run("batch", "work/instances", "-o", "work/results", "-e", "cooling",
                "--solver", "sa", "--t-start", "0.5", "-l", "sa-cold", "-t", "start=cold");
        run("batch", "work/instances", "-o", "work/results", "-e", "cooling",
                "--solver", "sa", "--t-start", "2.0", "-l", "sa-warm", "-t", "start=warm");
        run("validate", "work/instances", "work/results/cooling");
        System.out.println("Now try: python3 analysis/collect.py work/results  and open viewer/index.html");
    }

    private static void run(String... args) {
        System.out.println("$ cflp " + String.join(" ", args));
        int exitCode = App.commandLine().execute(args);
        if (exitCode != 0) {
            throw new IllegalStateException("Command failed with exit code " + exitCode);
        }
    }
}
