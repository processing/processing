/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2005 Ben Fry and Casey Reas

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
import java.lang.reflect.*;
import sun.audio.*;


/**
 * This is the crappy 8 khz mono ulaw version that's compatible
 * with Java 1.1 and 1.2. For Java 1.3 and higher, PSound2 is used.
 * <P>
 * This code currently does not work, but PSound2 sort of does.
 */
public class PSound {
  // supposedly this is actually 8012.8210513 according to spec
  public static final int SAMPLING_RATE = 8000;

  PApplet parent;
  Method soundEventMethod;

  InputStream stream;
  boolean loop;
  float volume = 1;

  //int length;
  int position;
  int data[];  // 16 bit, from -32768 to +32767


  public PSound() { }  // for 1.3 subclass


  public PSound(PApplet parent, InputStream input) {
    this.parent = parent;

    parent.registerDispose(this);

    try {
      soundEventMethod =
        parent.getClass().getMethod("soundEvent",
                                      new Class[] { PSound.class });

    } catch (Exception e) {
      // no such method, or an error.. which is fine, just ignore
    }
  }


  public void play() {
    AudioPlayer.player.start(stream);
  }


  /**
   * either sets repeat flag, or begins playing (and sets)
   */
  public void loop() {
    loop = true;
  }


  /**
   * ala java 1.3 loop docs:
   * "any current looping should cease and playback should
   * continue to the end of the clip."
   */
  public void noLoop() {
    loop = false;
  }


  public void pause() {
    AudioPlayer.player.stop(stream);
  }


  /**
   * Stops the audio and rewinds to the beginning.
   */
  public void stop() {
    AudioPlayer.player.stop(stream);

  }


  /**
   * current position inside the clip (in seconds, just like video)
   */
  public float time() {
    //return 0;
    return (float)position / (float)SAMPLING_RATE;
  }


  /**
   * duration of the clip in seconds
   */
  public float duration() {
    return 0;
  }


  public void volume(float v) {  // ranges 0..1
    this.volume = v;
  }


  /**
   * General error reporting, all corraled here just in case
   * I think of something slightly more intelligent to do.
   */
  protected void error(String where, Exception e) {
    parent.die("Error inside PSound." + where + "()", e);
    //e.printStackTrace();
  }


  // ------------------------------------------------------------


  class Stream extends InputStream {
    int index;

    public int available() throws IOException {
      return data.length - position;
    }

    public void close() throws IOException { }

    public synchronized void mark() { }

    public boolean markSupported() {
      return false;
    }

    public int read() throws IOException {
      return 0;
    }

    public int read(byte b[]) throws IOException {
      return 0;
    }

    public int read(byte b[], int off, int len) throws IOException {
      return 0;
    }

    public synchronized void reset() {
      position = 0;
    }

    public long skip(long n) {
      position = (position + (int)n) % data.length;
      return n;
    }
  }



  // ------------------------------------------------------------

  // Conversion
  // ulaw from http://www-svr.eng.cam.ac.uk/comp.speech/Section2/Q2.7.html

  static final short BIAS = 0x84;
  static final int CLIP = 32635;

  static final int[] LINEAR_LUT = {
    0, 132, 396, 924, 1980, 4092, 8316, 16764
  };

  static final int[] LAW_LUT = {
    0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3,
    4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
    5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
    5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
    6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
    6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
    6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
    6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7
  };


  /**
   * 16 bit linear to 8 bit ulaw
   */
  static byte linear2ulaw(int linear) {
    //int sign, exponent, mantissa;
    //byte ulaw;

    if (linear > 32767) {
      linear = 32767;
    } else if(linear < -32768) {
      linear = -32768;
    }

    int sign = linear >> 8 & 0x80;
    if (sign != 0) linear = -linear;
    if (linear > CLIP) linear = CLIP;

    linear += BIAS;
    int exponent = LAW_LUT[linear >> 7 & 0xFF];
    int mantissa = linear >> exponent + 3 & 0xf;
    byte ulaw = (byte) ( ~(sign | exponent << 4 | mantissa));
    if (ulaw == 0) ulaw = 2;  // CCITT trap

    return ulaw;
  }

  /**
   * 8 bit ulaw to 16 bit linear
   */
  static int ulaw2linear(int ulaw) {
    ulaw = ~ulaw;

    int sign = ulaw & 0x80;
    int exponent = (ulaw >> 4) & 7;
    int mantissa = ulaw & 0x0F;
    int linear = LINEAR_LUT[exponent] + (mantissa << exponent + 3);

    return (short) ((sign != 0) ? -linear : linear);
  }


  /**
   * cheap resampling with pitch distortion and aliasing
   */
  static int[] resample(int original[], int oldfreq, int newfreq) {
    int resampled[] = null;
    float factor = 0;
    int newlength;

    if (oldfreq > newfreq) {  // downsample
      factor = (float)oldfreq / (float)newfreq;
      newlength = (int)(original.length / factor);
      resampled = new int[newlength];

      for (int i = 0; i < newlength; i++) {
        resampled[i] = original[(int)(i * factor)];
      }
      return resampled;

    } else if (oldfreq < newfreq) {  // upsample (is it necesary??)
      factor = (float)newfreq / (float)oldfreq;
      newlength = (int) (original.length * factor);
      resampled = new int[newlength];

      for (int i = 0; i < newlength; i++) {
        resampled[i] = original[(int) (i * factor)];
      }
      return resampled;
    }
    return original;
  }
}
