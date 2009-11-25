package processing.app.tools.android;

import java.io.*;

import processing.core.PApplet;


public class StringRedirectThread extends Thread {
  private Reader in;
  private StringWriter out;
  private boolean finished;

  private static final int BUFFER_SIZE = 2048;

  
  public StringRedirectThread(InputStream in) {
    this.in = new InputStreamReader(in);
    this.out = new StringWriter();
    setPriority(Thread.MAX_PRIORITY-1);
    start();  // [fry]
  }


  public void run() {
    try {
      char[] cbuf = new char[BUFFER_SIZE];
      int count;
      while ((count = in.read(cbuf, 0, BUFFER_SIZE)) >= 0) {
        out.write(cbuf, 0, count);
        // had to add the flush() here.. maybe shouldn't be using writer? [fry]
        out.flush();
      }
      out.flush();

    } catch (IOException exc) {
      exc.printStackTrace();
    }
    finished = true;
  }


  public void finish() {
    while (!finished) {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) { }
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