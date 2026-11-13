package org.example.cflp.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "cflp",
        mixinStandardHelpOptions = true,
        version = "cflp 1.0",
        description = "Generate, solve and validate capacitated facility location instances.",
        subcommands = {
                GenerateCommand.class,
                SolveCommand.class,
                BatchCommand.class,
                ValidateCommand.class,
                CommandLine.HelpCommand.class
        })
public class App {

    public static void main(String[] args) {
        System.exit(commandLine().execute(args));
    }

    /** Shared by main() and DevMain, so both behave identically. */
    public static CommandLine commandLine() {
        return new CommandLine(new App())
                .setCaseInsensitiveEnumValuesAllowed(true);
    }
}
