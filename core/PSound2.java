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
 * <P/>
 * This will eventually be merged back into PSound, so you should *not*
 * create new PSound2 objects from scratch, only open them directly
 * from a PApplet using loadSound().
 * <P/>
 * Most of the useful info about how to do all the things
 * in this class was munged together from the
 * <A HREF="http://javaalmanac.com/egs/javax.sound.sampled/pkg.html">
 * Java Alamanac</A>.
 * <P/>
 * If you want your sketch to be notified when the sound has completed,
 * use the following code:
 * <PRE>
 * public void soundEvent(PSound which) {
 *   // do something because the sound 'which' is no longer playing
 * }
 * </PRE>
 */
public class PSound2 extends PSound {
  public Clip clip;
  FloatControl gainControl;
  boolean stopCalled;


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
      try {
        gainControl =
          (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
      } catch (Exception e) {
        System.err.println("Couldn't get gain control for this .wav file");
        e.printStackTrace();
      }

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
                if (!stopCalled) {
                  // if the stop() method was called, then the clip
                  // is already being stopped, and stopping it here will
                  // may pre-empt it from being immediately restarted.
                  // i.e. with the code sound.stop(); sound.play();
                  //if (playing()) {
                  if (clip.isActive()) {
                    // when playing, needs to shut everything off
                    clip.stop();
                    clip.setFramePosition(0);
                  } else {
                    // if not playing, call stop() to reset clip internally
                    clip.stop();
                  }
                }
                stopCalled = false;

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
    stop();
    clip.start();
  }


  /**
   * Free up resources used by this clip, otherwise an exception
   * that reads "LineUnavailableException: No Free Voices" will be thrown
   * after about 32 attempts. [thanks to daniel shiffman for this fix]
   */
  //public void destroy() {
  protected void finalize() throws Throwable {
    //try {
    if (clip != null) {
      clip.flush();
      clip.close();
    }
    //} catch (Exception e) {
    //System.err.println("Error destroying clip: " + e);
  //}
  }


  /**
   * either sets repeat flag, or begins playing (and sets)
   */
  public void loop() {
  /*
    // other possible solution for the looping issues
    looping = true;
    //System.out.println("loop() clip active = " + clip.isActive());
    if (!clip.isActive()) {
      clip.stop();
      clip.setFramePosition(0);
      clip.start();
    }
    */
    clip.loop(Clip.LOOP_CONTINUOUSLY);
  }


  /**
   * ala java 1.3 loop docs:
   * "any current looping should cease and playback should
   * continue to the end of the clip."
   */
  public void noLoop() {
    clip.loop(0);
    //looping = false;
    //if (clip.isActive()) stop();
  }


  public void pause() {
    clip.stop();
  }


  /**
   * Stops the audio and rewinds to the beginning.
   */
  public void stop() {
    //System.out.println("method stop");
    // clip may become null in the midst of this method
    //if (clip != null) clip.stop();
    //if (clip != null) clip.setFramePosition(0);
    clip.stop();
    clip.setFramePosition(0);
    stopCalled = true;
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
    if (gainControl != null) {
      float dB = (float)(Math.log(v)/Math.log(10.0)*20.0);
      gainControl.setValue(dB);
    } else {
      System.err.println("Cannot set the volume for this sound.");
    }
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
