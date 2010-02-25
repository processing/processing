package processing.app.tools.android;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogEntry {
  public static enum Severity {
    Verbose, Debug, Info, Warning, Error, Fatal;
    static Severity fromChar(final char c) {
      if (c == 'V') {
        return Verbose;
      } else if (c == 'D') {
        return Debug;
      } else if (c == 'I') {
        return Info;
      } else if (c == 'W') {
        return Warning;
      } else if (c == 'E') {
        return Error;
      } else if (c == 'F') {
        return Fatal;
      } else {
        throw new IllegalArgumentException("I don't know how to interpret '"
            + c + "' as a log severity");
      }
    }
  }

  public final Severity severity;
  public final String source;
  public final String sourcePid;
  public final String message;

  private static final Pattern PARSER = Pattern
      .compile("^([VDIWEF])/([^\\(]+)\\(\\s*(\\d+)\\):\\s*(.+)$");

  public LogEntry(final String line) {
    final Matcher m = PARSER.matcher(line);
    if (!m.matches()) {
      throw new RuntimeException("I can't understand log entry\n" + line);
    }
    this.severity = Severity.fromChar(m.group(1).charAt(0));
    this.source = m.group(2);
    this.sourcePid = m.group(3);
    this.message = m.group(4);
  }

  @Override
  public String toString() {
    return severity + "/" + source + "(" + sourcePid + "): " + message;
  }
}
