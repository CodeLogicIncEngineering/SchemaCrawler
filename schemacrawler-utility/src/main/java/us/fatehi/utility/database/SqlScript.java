/*
 * ======================================================================== SchemaCrawler
 * http://www.schemacrawler.com Copyright (c) 2000-2023, Sualeh Fatehi <sualeh@hotmail.com>. All
 * rights reserved. ------------------------------------------------------------------------
 *
 * SchemaCrawler is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * SchemaCrawler and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0, GNU General Public License v3 or GNU Lesser General Public License v3.
 *
 * You may elect to redistribute this code under any of these licenses.
 *
 * The Eclipse Public License is available at: http://www.eclipse.org/legal/epl-v10.html
 *
 * The GNU General Public License v3 and the GNU Lesser General Public License v3 are available at:
 * http://www.gnu.org/licenses/
 *
 * ========================================================================
 */
package us.fatehi.utility.database;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class SqlScript implements Runnable {

  private static final Logger LOGGER = Logger.getLogger(SqlScript.class.getName());

  private static final boolean debug =
      Boolean.valueOf(System.getProperty(SqlScript.class.getCanonicalName() + ".debug", "false"));

  public static void executeScriptFromResource(
      final String scriptResource, final Connection connection) {
    new SqlScript(scriptResource, connection).run();
  }

  private final String scriptResource;
  private final String delimiter;

  private final Connection connection;

  private SqlScript(final String scriptResourceLine, final Connection connection) {
    requireNonNull(scriptResourceLine, "No script resource line provided");
    final String[] split = scriptResourceLine.split(",");
    if (split.length == 1) {
      scriptResource = scriptResourceLine.trim();
      if (scriptResource.isEmpty()) {
        delimiter = "#";
      } else {
        delimiter = ";";
      }
    } else if (split.length == 2) {
      delimiter = split[0].trim();
      scriptResource = split[1].trim();
    } else {
      throw new RuntimeException("Too many fields in " + scriptResourceLine);
    }

    this.connection = requireNonNull(connection, "No database connection provided");
  }

  @Override
  public void run() {

    final boolean skip = delimiter.equals("#");

    if (debug) {
      final String lineLogMessage =
          String.format(
              "%s %s", scriptResource, skip ? "-- skip" : "-- execute, delimiting by " + delimiter);
      LOGGER.log(Level.INFO, lineLogMessage);
      System.out.println(lineLogMessage);
    }

    if (skip) {
      return;
    }

    String sql = null;
    try (final BufferedReader lineReader =
            new BufferedReader(
                new InputStreamReader(this.getClass().getResourceAsStream(scriptResource), UTF_8));
        final Statement statement = connection.createStatement();
        // NOTE: Do not close connection, since we did not open it
        ) {
      final List<String> sqlList = readSql(lineReader);
      for (final Iterator<String> iterator = sqlList.iterator(); iterator.hasNext(); ) {
        sql = iterator.next();
        statement.clearWarnings();
        try {
          if (Pattern.matches("\\s+", sql)) {
            continue;
          }
          if (debug) {
            LOGGER.log(Level.INFO, "\n" + sql);
          }

          final boolean hasResults = statement.execute(sql);
          if (hasResults) {
            throw new SQLWarning(String.format("Results not expected from SQL%n%s%n", sql));
          }

          final SQLWarning warnings = statement.getWarnings();
          if (warnings != null) {
            if (!warnings.getMessage().startsWith("Can't drop database")) {
              throw warnings;
            }
          }

          if (!connection.getAutoCommit()) {
            connection.commit();
          }

        } catch (final SQLWarning e) {
          final int errorCode = e.getErrorCode();
          if (errorCode == 5701 || errorCode == 5703 || errorCode == 1280) {
            // SQL Server information message
            continue;
          }
          final Throwable throwable = getCause(e);
          throw new RuntimeException(throwable);
        }
      }
    } catch (final Exception e) {
      final Throwable throwable = getCause(e);
      final String message =
          String.format("Script: %s -- %s", scriptResource, throwable.getMessage());
      System.err.println(message);
      System.err.println(sql);
      LOGGER.log(Level.WARNING, message, throwable);
      throw new RuntimeException(e);
    }
  }

  private Throwable getCause(final Throwable e) {
    Throwable cause = null;
    Throwable result = e;

    while (null != (cause = result.getCause()) && result != cause) {
      result = cause;
    }
    return result;
  }

  private List<String> readSql(final BufferedReader lineReader) throws IOException {
    final List<String> list = new ArrayList<>();
    String line;
    StringBuilder sql = new StringBuilder();
    while ((line = lineReader.readLine()) != null) {
      final String trimmedLine = line.trim();
      final boolean isComment = trimmedLine.startsWith("--") || trimmedLine.startsWith("//");
      if (!isComment && trimmedLine.endsWith(delimiter)) {
        sql.append(line.substring(0, line.lastIndexOf(delimiter)));
        list.add(sql.toString());
        sql = new StringBuilder();
      } else {
        sql.append(line);
        sql.append("\n");
      }
    }
    // Check if the last line is not delimited
    if (sql.length() > 0) {
      list.add(sql.toString());
    }

    return list;
  }
}
