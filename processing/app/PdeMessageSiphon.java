/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeMessageSiphon - slurps up messages from compiler
  Part of the Processing project - http://processing.org

  Earlier portions of this code are Copyright (c) 2001-04 MIT
  Other parts are Copyright (c) 2004 Ben Fry and Casey Reas

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
    this.streamReader = new BufferedReader(new InputStreamReader(stream));
    this.consumer = consumer;

    thread = new Thread(this);
    // don't set priority too low, otherwise exceptions won't
    // bubble up in time (i.e. compile errors have a weird delay)
    //thread.setPriority(Thread.MIN_PRIORITY);
    thread.start();
  }


  public void run() {
    try {
      // process data until we hit EOF; this will happily block
      // (effectively sleeping the thread) until new data comes in.
      // when the program is finally done, null will come through.
      //
      String currentLine;
      while ((currentLine = streamReader.readLine()) != null) {
        // \n is added again because readLine() strips it out
        //PdeEditorConsole.systemOut.println("messaging in");
        consumer.message(currentLine + "\n");
        //PdeEditorConsole.systemOut.println("messaging out");
      }
      //PdeEditorConsole.systemOut.println("messaging thread done");
      thread = null;

    } catch (NullPointerException npe) {
      // Fairly common exception during shutdown
      thread = null;

    } catch (Exception e) {
      // On Linux and sometimes on Mac OS X, a "bad file descriptor"
      // message comes up when closing an applet that's run externally.
      // That message just gets supressed here..
      String mess = e.getMessage();
      if ((mess != null) &&
          (mess.indexOf("Bad file descriptor") != -1)) {
        //if (e.getMessage().indexOf("Bad file descriptor") == -1) {
        //System.err.println("PdeMessageSiphon err " + e);
        //e.printStackTrace();
      }
      thread = null;
    }
  }
}
