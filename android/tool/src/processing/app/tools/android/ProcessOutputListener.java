package processing.app.tools.android;

public interface ProcessOutputListener {
  public void handleStdout(final String line);

  public void handleStderr(final String line);
}
