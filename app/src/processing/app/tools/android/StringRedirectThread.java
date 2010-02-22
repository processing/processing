package processing.app.tools.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

import processing.core.PApplet;

// if you start a thread in the constructor, then you should be final - jdf
public final class StringRedirectThread extends Thread {
  private final Reader in;

  private final StringWriter out;

  private boolean finished;

  private static final int BUFFER_SIZE = 2048;

  public StringRedirectThread(final InputStream in) {
    this.in = new InputStreamReader(in);
    this.out = new StringWriter();
    setPriority(Thread.MAX_PRIORITY - 1);
    start(); // [fry]
  }

  @Override
  public void run() {
    try {
      final char[] cbuf = new char[BUFFER_SIZE];
      int count;
      while ((count = in.read(cbuf, 0, BUFFER_SIZE)) >= 0) {
        out.write(cbuf, 0, count);
        // had to add the flush() here.. maybe shouldn't be using writer? [fry]
        out.flush();
      }
      out.flush();

    } catch (final IOException exc) {
      exc.printStackTrace();
    }
    finished = true;
  }

  public void finish() {
    while (!finished) {
      try {
        Thread.sleep(50);
      } catch (final InterruptedException e) {
      }
    }
  }

  public boolean isFinished() {
    return finished;
  }

  public String getString() {
    return (finished) ? out.toString() : null;
  }

  public String[] getLines() {
    return finished ? PApplet.split(out.toString(), '\n') : null;
  }
}