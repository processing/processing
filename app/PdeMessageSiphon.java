/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeMessageSiphon - slurps up messages from compiler
  Part of the Processing project - http://Proce55ing.net

  Copyright (c) 2001-03 
  Ben Fry, Massachusetts Institute of Technology and 
  Casey Reas, Interaction Design Institute Ivrea

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
      e.printStackTrace();
      //thread.stop();  // implicit (and no longer supported)
    }
    //System.err.println("siphon thread exiting");
  }
}
