/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeMessageSiphon - slurps up messages from compiler
  Part of the Processing project - http://Proce55ing.net

  Except where noted, code is written by Ben Fry and
  Copyright (c) 2001-03 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License 
  along with this program; if not, write to the Free Software Foundation, 
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

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
    // don't set priority too low, otherwise exceptions won't
    // bubble up in time (i.e. compile errors)
    //thread.setPriority(Thread.MIN_PRIORITY);
    thread.start();
  }


  public void run() {    
    //while (Thread.currentThread() == thread) {
    //System.err.print("p");
    //System.err.println(streamReader);
    String currentLine;

    try {
      // process data until we hit EOF; this will happily block
      // (effectively sleeping the thread) until new data comes in.
      // when the program is finally done, 
      //
      while ((currentLine = streamReader.readLine()) != null) {
        //currentLine = streamReader.readLine();
        //if (currentLine != null) {
        consumer.message(currentLine);
        //System.out.println("siphon wait");
      }
      /*
        if (currentLine == null) {
        System.out.println("PdeMessageSiphon: out");
        thread = null; 
        }
      */
      thread = null;
      //System.err.println("PMS: " + currentLine);
      //}

    } catch (NullPointerException npe) {
      // ignore this guy, since it's prolly just shutting down
      //npe.printStackTrace();
      thread = null;

    } catch (Exception e) { 
      // on linux, a "bad file descriptor" message comes up when
      // closing an applet that's being run externally.
      // use this to cause that to fail silently since not important
      //String mess = e.getMessage();
      //if ((PdeBase.platform != PdeBase.LINUX) || 
      //(e.getMessage().indexOf("Bad file descriptor") == -1)) {
      if (e.getMessage().indexOf("Bad file descriptor") == -1) {
        System.err.println("PdeMessageSiphon err " + e);
        e.printStackTrace();
        thread = null;
      }
    }

    /*
      //Thread.yield();
      try {
      Thread.sleep(100);
      } catch (InterruptedException e) { }
    */
    //System.out.println("PdeMessageSiphon: out");
  }
  //System.err.println("siphon thread exiting");
}
