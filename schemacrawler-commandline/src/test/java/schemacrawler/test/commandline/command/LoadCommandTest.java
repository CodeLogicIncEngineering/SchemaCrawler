package schemacrawler.test.commandline.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static schemacrawler.test.utility.FileHasContent.classpathResource;
import static schemacrawler.test.utility.FileHasContent.hasSameContentAs;
import static schemacrawler.test.utility.FileHasContent.outputOf;
import static schemacrawler.test.utility.TestUtility.writeStringToTempFile;
import static schemacrawler.tools.commandline.utility.CommandLineUtility.newCommandLine;

import java.sql.Connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import schemacrawler.schemacrawler.InfoLevel;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.test.utility.TestContext;
import schemacrawler.test.utility.TestContextParameterResolver;
import schemacrawler.test.utility.TestDatabaseConnectionParameterResolver;
import schemacrawler.tools.commandline.SchemaCrawlerShellCommands;
import schemacrawler.tools.commandline.command.LoadCommand;
import schemacrawler.tools.commandline.state.ShellState;
import schemacrawler.tools.commandline.state.StateFactory;
import schemacrawler.tools.commandline.utility.CommandLineUtility;
import schemacrawler.tools.options.Config;

@ExtendWith(TestContextParameterResolver.class)
@ExtendWith(TestDatabaseConnectionParameterResolver.class)
public class LoadCommandTest {

  private final String COMMAND_HELP = "command_help/";

  @Test
  public void dynamicOptionValue(final Connection connection) throws Exception {
    final String[] args = {
      "load", "--info-level", "detailed", "--test-load-option", "true", "additional", "-extra"
    };

    final ShellState state = new ShellState();
    state.setSchemaCrawlerOptions(SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions());
    state.setDataSource(() -> connection);

    final CommandLine commandLine =
        newCommandLine(new SchemaCrawlerShellCommands(), new StateFactory(state));
    CommandLineUtility.addLoadCommandOptions(commandLine);

    commandLine.execute(args);

    final Config config = state.getConfig();
    assertThat(config.containsKey("test-load-option"), is(true));
    assertThat(config.getStringValue("test-load-option", null), is("true"));
  }

  @Test
  public void help(final TestContext testContext) throws Exception {
    final ShellState state = new ShellState();
    final LoadCommand optionsParser = new LoadCommand(state);

    @Command(name = "base-command")
    class SomeClass {}

    final CommandLine commandLine = newCommandLine(optionsParser, null);
    final CommandLine baseCommandLine =
        newCommandLine(new SomeClass(), null).addSubcommand(commandLine);

    CommandLineUtility.addLoadCommandOptions(baseCommandLine);

    final String helpMessage = commandLine.getUsageMessage();

    assertThat(
        outputOf(writeStringToTempFile(helpMessage)),
        hasSameContentAs(
            classpathResource(COMMAND_HELP + testContext.testMethodFullName() + ".txt")));
  }

  @Test
  public void infoLevelBadValue() {
    final String[] args = {"--info-level", "someinfolvl"};

    final ShellState state = new ShellState();
    final LoadCommand optionsParser = new LoadCommand(state);

    assertThrows(
        CommandLine.ParameterException.class,
        () -> {
          final CommandLine commandLine = newCommandLine(optionsParser, null);
          commandLine.parseArgs(args);
        });
  }

  @Test
  public void infoLevelNoValue() {
    final String[] args = {"--info-level"};

    final ShellState state = new ShellState();
    final LoadCommand optionsParser = new LoadCommand(state);

    assertThrows(
        CommandLine.ParameterException.class,
        () -> {
          final CommandLine commandLine = newCommandLine(optionsParser, null);
          commandLine.parseArgs(args);
        });
  }

  @Test
  public void infoLevelWithValue() {
    final String[] args = {"--info-level", "detailed", "additional", "-extra"};

    final ShellState state = new ShellState();
    final LoadCommand optionsParser = new LoadCommand(state);
    final CommandLine commandLine = newCommandLine(optionsParser, null);
    commandLine.parseArgs(args);

    final InfoLevel schemaInfoLevel = optionsParser.getInfoLevel();

    assertThat(schemaInfoLevel, is(InfoLevel.detailed));
    assertThat(optionsParser.isLoadRowCounts(), is(false));
  }

  @Test
  public void loadRowCountsWithoutValue() {
    final String[] args = {"--info-level", "detailed", "--load-row-counts", "additional", "-extra"};

    final ShellState state = new ShellState();
    final LoadCommand optionsParser = new LoadCommand(state);
    final CommandLine commandLine = newCommandLine(optionsParser, null);
    commandLine.parseArgs(args);

    final InfoLevel schemaInfoLevel = optionsParser.getInfoLevel();

    assertThat(schemaInfoLevel, is(InfoLevel.detailed));
    assertThat(optionsParser.isLoadRowCounts(), is(true));
  }

  @Test
  public void loadRowCountsWithValue() {
    final String[] args = {
      "--info-level", "detailed", "--load-row-counts", "true", "additional", "-extra"
    };

    final ShellState state = new ShellState();
    final LoadCommand optionsParser = new LoadCommand(state);
    final CommandLine commandLine = newCommandLine(optionsParser, null);
    commandLine.parseArgs(args);

    final InfoLevel schemaInfoLevel = optionsParser.getInfoLevel();

    assertThat(schemaInfoLevel, is(InfoLevel.detailed));
    assertThat(optionsParser.isLoadRowCounts(), is(true));
  }

  @Test
  public void noArgs() {
    final String[] args = new String[0];

    final ShellState state = new ShellState();
    final LoadCommand optionsParser = new LoadCommand(state);

    assertThrows(
        CommandLine.ParameterException.class,
        () -> {
          final CommandLine commandLine = newCommandLine(optionsParser, null);
          commandLine.parseArgs(args);
        });
  }

  @Test
  public void noValidArgs() {
    final String[] args = {"--some-option"};

    final ShellState state = new ShellState();
    final LoadCommand optionsParser = new LoadCommand(state);

    assertThrows(
        CommandLine.ParameterException.class,
        () -> {
          final CommandLine commandLine = newCommandLine(optionsParser, null);
          commandLine.parseArgs(args);
        });
  }
}
