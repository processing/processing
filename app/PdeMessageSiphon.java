/// -*- Mode: JDE; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 2 -*-

import java.io.*;

// XXXdmose error checking

class PdeMessageSiphon implements Runnable {
  BufferedReader stdout, stderr;
  PrintStream leechErr;
  Thread thread;
  PdeMessageConsumer consumer;

  public PdeMessageSiphon(InputStream stdout, InputStream stderr,
                          PdeMessageConsumer consumer) {

    // XXXdmose error checking here

    this.stdout = new BufferedReader(new InputStreamReader(stdout));
    this.stderr = new BufferedReader(new InputStreamReader(stderr));
    this.consumer = consumer;

    thread = new Thread(this);
    thread.start();
  }

  public void run() {
    
    String currentLine;

    while (Thread.currentThread() == thread) {

      // XXX put back while loops

      try {
        if (stderr.ready()) {
          currentLine = stderr.readLine();
          System.err.println(currentLine);
          consumer.message(currentLine);
        }

        if (stdout.ready()) {
          currentLine = stdout.readLine();
          System.err.println(currentLine);
          consumer.message(currentLine);
        }

        Thread.sleep(10);

      } catch (Exception e) { 
        System.err.println("PdeMessageSiphon err " + e);

        thread.stop();
        thread = null;
      }
    }
  }
}
