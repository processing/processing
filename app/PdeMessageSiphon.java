/// -*- Mode: JDE; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 2 -*-

import java.io.*;

class PdeMessageSiphon implements Runnable {
  BufferedReader streamReader;
  Thread thread;
  PdeMessageConsumer consumer;

  public PdeMessageSiphon(InputStream stream, PdeMessageConsumer consumer) {

    // we use a BufferedReader in order to be able to read a line
    // at a time
    //
    this.streamReader = new BufferedReader(new InputStreamReader(stream));
    this.consumer = consumer;

    thread = new Thread(this);
    thread.start();
  }

  public void run() {
    
    String currentLine;

    try {
      // process data until we hit EOF; this may block
      //
      while ((currentLine = streamReader.readLine()) != null) {
        consumer.message(currentLine);
        //System.err.println(currentLine);
      }
    } catch (Exception e) { 
      System.err.println("PdeMessageSiphon err " + e);
      thread.stop();
    }
    
    //System.err.println("siphon thread exiting");
  }
}
