package org.example.cflp;

import org.example.cflp.cli.App;

/**
 * Click "Run" in the IDE: executes the same commands you would type in a terminal,
 * but with breakpoints and the debugger available. Edit freely, this is your scratch pad.
 */
public class DevMain {

    public static void main(String[] args) {
        run("generate", "--count", "3", "--facilities", "30", "--customers", "150", "-o", "work/instances");
        run("batch", "work/instances", "--solver", "sa", "--iterations", "100000", "-o", "work/solutions");
        run("validate", "work/instances/cflp-30-150-1.instance.json", "work/solutions/cflp-30-150-1.sa.solution.json");
    }

    private static void run(String... args) {
        System.out.println("$ cflp " + String.join(" ", args));
        int exitCode = App.commandLine().execute(args);
        if (exitCode != 0) {
            throw new IllegalStateException("Command failed with exit code " + exitCode);
        }
    }
}
