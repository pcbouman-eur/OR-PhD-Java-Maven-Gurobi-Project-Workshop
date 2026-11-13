package org.example.cflp.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        System.exit(commandLine().execute(args));
    }

    /** Shared by main() and DevMain, so both behave identically. */
    public static CommandLine commandLine() {
        return new CommandLine(new App())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setExecutionExceptionHandler((e, cmd, parseResult) -> {
                    log.error("{} failed", cmd.getCommandName(), e);   // stack trace goes to the log file
                    return 1;
                });
    }
}
