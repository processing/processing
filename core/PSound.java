/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PSound - java 1.1 audio loader and player
  Part of the Processing project - http://processing.org

  Copyright (c) 2004 Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.core;

import java.io.*;
import sun.audio.*;

// add check for reflection in host applet for sound completion
// also needs to register for stop events with applet


/**
 * This is the crappy 8 khz ulaw version that's compatible
 * with Java 1.1 and 1.2. For Java 1.3 and higher, PSound2 is used.
 */
public class PSound {
  PApplet applet;


  public PSound() { }  // for subclass


  public PSound(PApplet applet, InputStream input) {
    this.applet = applet;

    /*
    try {

    } catch (Exception e) {
      error("<init>", e);
    }
    */
  }


  public void play() {
  }


  /**
   * either sets repeat flag, or begins playing (and sets)
   */
  public void loop() {
  }


  /**
   * ala java 1.3 loop docs:
   * "any current looping should cease and playback should
   * continue to the end of the clip."
   */
  public void noLoop() {
  }


  public void pause() {
  }


  /**
   * Stops the audio and rewinds to the beginning.
   */
  public void stop() {
  }


  /**
   * current position inside the clip (in seconds, just like video)
   */
  public float time() {
    return 0;
  }


  /**
   * duration of the clip in seconds
   */
  public float duration() {
    return 0;
  }


  public void volume(float v) {  // ranges 0..1
  }


  /**
   * General error reporting, all corraled here just in case
   * I think of something slightly more intelligent to do.
   */
  protected void error(String where, Exception e) {
    applet.die("Error inside PSound." + where + "()", e);
    //e.printStackTrace();
  }
}
