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
import javax.sound.sampled.*;


/**
 * Java 1.3 audio loader and player.
 * <P>
 * This will eventually be merged back into PSound, so you shouldn't
 * create new PSound2 objects from scratch, only open them directly
 * from a PApplet using loadSound().
 * <PRE>
 * useful info about how to do all this stuff, munged together
 * for this class: http://javaalmanac.com/egs/javax.sound.sampled/pkg.html
 * </PRE>
 */
public class PSound2 extends PSound {
  Clip clip;
  FloatControl gainControl;


  public PSound2(PApplet iparent, InputStream input) {
    this.parent = iparent;

    try {
      AudioInputStream ais =
        AudioSystem.getAudioInputStream(input);

      // At present, ALAW and ULAW encodings must be converted
      // to PCM_SIGNED before it can be played
      AudioFormat format = ais.getFormat();
      if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
        // *** this code appears as though it may just be faulty ***
        format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                 format.getSampleRate(),
                                 format.getSampleSizeInBits()*2,
                                 format.getChannels(),
                                 format.getFrameSize()*2,
                                 format.getFrameRate(),
                                 true);  // big endian
        ais = AudioSystem.getAudioInputStream(format, ais);
      //} else {
        //System.out.println("no conversion necessary");
      }

      int frameLength = (int) ais.getFrameLength();
      int frameSize = format.getFrameSize();
      DataLine.Info info =
        new DataLine.Info(Clip.class, ais.getFormat(),
                          frameLength * frameSize);

      clip = (Clip) AudioSystem.getLine(info);

      // seems that you can't make more than one of these
      gainControl =
        (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);

      // This method does not return until completely loaded
      clip.open(ais);

      // determining when a sample is done
      // Add a listener for line events
      /*
        clip.addLineListener(new LineListener() {
        public void update(LineEvent evt) {
        if (evt.getType() == LineEvent.Type.STOP) {
        }
        }
        });
      */

      parent.registerDispose(this);

      try {
        soundEventMethod =
          parent.getClass().getMethod("soundEvent",
                                      new Class[] { PSound.class });

        // if we're here, then it means that there's a method for it
        clip.addLineListener(new LineListener() {
            public void update(LineEvent event) {
              if (event.getType() == LineEvent.Type.STOP) {
                try {
                  soundEventMethod.invoke(parent,
                                          new Object[] { PSound2.this });
                } catch (Exception e) {
                  System.err.println("error, disabling soundEvent()");
                  e.printStackTrace();
                  soundEventMethod = null;
                }
              }
            }
          });

      } catch (Exception e) {
        // no such method, or an error.. which is fine, just ignore
      }

    } catch (Exception e) {
      error("<init>", e);
    }
  }


  public void play() {
    clip.start();
  }


  /**
   * either sets repeat flag, or begins playing (and sets)
   */
  public void loop() {
    clip.loop(Clip.LOOP_CONTINUOUSLY);
  }


  /**
   * ala java 1.3 loop docs:
   * "any current looping should cease and playback should
   * continue to the end of the clip."
   */
  public void noLoop() {
    clip.loop(0);
  }


  // Play and repeat for a certain number of times
  //int numberOfPlays = 3;
  //clip.loop(numberOfPlays-1);


  public void pause() {
    clip.stop();
  }


  /**
   * Stops the audio and rewinds to the beginning.
   */
  public void stop() {
    // clip may become null in the midst of this method
    //if (clip != null) clip.stop();
    //if (clip != null) clip.setFramePosition(0);
    clip.stop();
    clip.setFramePosition(0);
  }


  /**
   * This is registered externally so that the host applet
   * will kill off the playback thread.
   */
  public void dispose() {
    stop();
    clip = null;
  }


  /**
   * current position inside the clip (in seconds, just like video)
   */
  public float time() {
    return (float) (clip.getMicrosecondPosition()/1000000.0d);
  }


  /**
   * duration of the clip in seconds
   */
  public float duration() {
    return (float) (clip.getBufferSize() /
                    (clip.getFormat().getFrameSize() *
                     clip.getFormat().getFrameRate()));
  }


  /**
   * Set the volume with a value from 0 (off) to 1 (loudest).
   */
  public void volume(float v) {  // ranges 0..1
    float dB = (float)(Math.log(v)/Math.log(10.0)*20.0);
    gainControl.setValue(dB);
  }


  /**
   * General error reporting, all corraled here just in case
   * I think of something slightly more intelligent to do.
   */
  protected void error(String where, Exception e) {
    parent.die("Error inside PSound2." + where + "()", e);
    //e.printStackTrace();
  }
}
