/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-09 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

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

package processing.app.exec;

import java.io.*;

/**
 * Siphons from an InputStream of System.out (from a Process)
 * and sends it to the real System.out.
 */
public class SystemOutSiphon implements Runnable {
  InputStream input;
  Thread thread;

  public SystemOutSiphon(InputStream input) {
    this.input = input;

    thread = new Thread(this);
    // unless this is set to min, it seems to hork the app
    // since it's in charge of stuffing the editor console with strings
    // maybe it's time to get rid of/fix that friggin console
    // ...disabled for 0075, with 0074's fix for code folder hanging
    // this only seems to make the console unresponsive
    //thread.setPriority(Thread.MIN_PRIORITY);
    thread.start();
  }

  public void run() {
    byte boofer[] = new byte[256];

    while (Thread.currentThread() == thread) {
      try {
        // can't use a buffered reader here because incremental
        // print statements are interesting too.. causes some
        // disparity with how System.err gets spewed, oh well.
        int count = input.read(boofer, 0, boofer.length);
        if (count == -1) {
          thread = null;

        } else {
          System.out.print(new String(boofer, 0, count));
          //System.out.flush();
        }

      } catch (IOException e) {
        // this is prolly because the app was quit & the stream broken
        //e.printStackTrace(System.out);
        //e.printStackTrace();
        thread = null;

      } catch (Exception e) {
        //System.out.println("SystemOutSiphon: i just died in your arms tonight");
        // on mac os x, this will spew a "Bad File Descriptor" ex
        // each time an external app is shut down.
        //e.printStackTrace();
        thread = null;
        //System.out.println("");
      }
      //System.out.println("SystemOutSiphon: out");
      //thread = null;
    }
  }
}
