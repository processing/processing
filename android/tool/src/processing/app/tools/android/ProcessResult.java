package processing.app.tools.android;

import java.util.Arrays;
import java.util.Iterator;

public class ProcessResult implements Iterable<String> {
  private final String cmd;
  private final long time;
  private final String output;
  private final String error;
  private final int result;

  public ProcessResult(final String cmd, final int result, final String output,
                       final String error, final long time) {
    this.cmd = cmd;
    this.output = output;
    this.error = error;
    this.result = result;
    this.time = time;
  }

  @Override
  public Iterator<String> iterator() {
    return Arrays.asList(output.split("\r?\n")).iterator();
  }

  public boolean succeeded() {
    return result == 0;
  }

  public String getStderr() {
    return error;
  }

  public String getStdout() {
    return output;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(cmd).append("\n");
    sb.append("    status: ").append(result).append("\n");
    sb.append("    ").append(time).append("ms").append("\n");
    sb.append("    stdout:\n").append(output).append("\n");
    sb.append("    stderr:\n").append(error);
    return sb.toString();
  }
}
