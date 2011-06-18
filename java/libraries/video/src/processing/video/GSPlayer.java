/**
 * Part of the GSVideo library: http://gsvideo.sourceforge.net/
 * Copyright (c) 2008-11 Andres Colubri 
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, version 2.1.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 */

package processing.video;

import processing.core.*;

import java.awt.Dimension;
import java.io.*;
import java.nio.*;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.*;

import org.gstreamer.*;
import org.gstreamer.Buffer;
import org.gstreamer.elements.*;

/**
 * This class makes it possible to load and play generic media content through
 * playbin (not only movies, but also audio files, etc).
 */
public class GSPlayer extends PImage implements PConstants {
  protected int streamType;
  protected String filename;

  protected boolean playing = false;
  protected boolean paused = false;
  protected boolean repeat = false;

  protected float fps;
  protected float rate;
  protected int bufWidth;
  protected int bufHeight;
  protected int bufSize;
  
  protected PlayBin2 gplayer;
  
  protected Method playerEventMethod;
  protected Method copyBufferMethod;  

  protected Object eventHandler;
  protected Object copyHandler;  
  
  protected boolean available;
  protected boolean sinkReady;  

  protected RGBDataAppSink rgbSink = null;    
  protected int[] copyPixels = null;

  protected BufferDataAppSink natSink = null;
  protected Buffer natBuffer = null;
  protected boolean copyBufferMode = false;
  protected String copyMask;  
  
  protected ByteDataAppSink dataSink = null;
  protected byte[] copyData = null;  
  public byte[] data = null;  
  
  public String dataCaps;
  protected String tempDataCaps;  
  
  protected boolean firstFrame = true;
    
  /**
   * Creates an instance of GSPlayer loading the media file from filename, 
   * assuming that it is a video file.
   * 
   * @param parent PApplet
   * @param filename String
   */
  public GSPlayer(PApplet parent, String filename) {
    super(0, 0, RGB);
    initGStreamer(parent, filename, GSVideo.VIDEO);
  }
      
  /**
   * Creates an instance of GSPlayer loading the media file from filename,
   * and trying to understand it as the indicated type.
   * 
   * @param parent PApplet
   * @param filename String
   * @param type int
   */   
  public GSPlayer(PApplet parent, String filename, int type) {
    super(0, 0, RGB);
    initGStreamer(parent, filename, type);    
  }  
  
  /**
   * Releases the gstreamer resources associated to this player object.
   * It shouldn't be used after this.
   */  
  public void delete() {
    if (gplayer != null) {
      try {
        if (gplayer.isPlaying()) {
          gplayer.stop();
        }
      } catch (IllegalStateException e) {
        System.err.println("error when deleting player, maybe some native resource is already disposed"); 
      } catch (Exception e) {
        e.printStackTrace();
      }
      
      pixels = null;  
      data = null;      

      copyPixels = null;      
      if (rgbSink != null) {
        rgbSink.removeListener();
        rgbSink.dispose();
        rgbSink = null;
      }      

      copyData = null; 
      if (dataSink != null) {
        dataSink.removeListener();
        dataSink.dispose();
        dataSink = null;
      }      
      
      natBuffer = null;
      if (natSink != null) {
        natSink.removeListener();
        natSink.dispose();
        natSink = null;
      }       
      
      gplayer.dispose();
      gplayer = null;
    }
  }  
  
  /**
   * Same as delete.
   */  
  public void dispose() {
    delete();
  }
  
  /**
   * Sets the object to use as destination for the frames read from the stream.
   * The color conversion mask is automatically set to the one required to
   * copy the frames to OpenGL.
   * 
   * @param Object dest
   */  
  public void setPixelDest(Object dest) {
    copyHandler = dest;      
    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
      copyMask = "red_mask=(int)0xFF000000, green_mask=(int)0xFF0000, blue_mask=(int)0xFF00";        
    } else {
      copyMask = "red_mask=(int)0xFF, green_mask=(int)0xFF00, blue_mask=(int)0xFF0000";
    }   
  }  
  
  /**
   * Sets the object to use as destination for the frames read from the stream.
   * 
   * @param Object dest
   * @param String mask 
   */    
  public void setPixelDest(Object dest, String mask) {
    copyHandler = dest;
    copyMask = mask;
  }    
  
  /**
   * Uses a generic object as handler of the media file. This object should have a
   * movieEvent method that receives a GSMovie argument. This method will
   * be called upon a new frame read event. 
   * 
   */
  public void setEventHandlerObject(Object obj) {
    eventHandler = obj;

    try {
      playerEventMethod = eventHandler.getClass().getMethod("playerEvent",
          new Class[] { GSPlayer.class });
    } catch (Exception e) {
      // no such method, or an error.. which is fine, just ignore
    }
  }

  /**
   * Get the width of the source video. Note: calling this method repeatedly
   * can slow down playback performance.
   * 
   * @return int
   */    
  public int getSourceWidth() {
    Dimension dim = gplayer.getVideoSize();
    if (dim != null) {
      return dim.width;
    } else {
      return 0;
    }
  }
  
  /**
   * Get the height of the source video. Note: calling this method repeatedly
   * can slow down playback performance.
   * 
   * @return int
   */      
  public int getSourceHeight() {
    Dimension dim = gplayer.getVideoSize();
    if (dim != null) {
      return dim.height;
    } else {
      return 0;
    }
  }  
  
  /**
   * Get the original framerate of the source video. Note: calling this method repeatedly
   * can slow down playback performance.
   * 
   * @return float
   */      
  public float getSourceFrameRate() {
    return (float)gplayer.getVideoSinkFrameRate();
  }
  
  /**
   * Set how often new frames are to be read from the stream. Does not actually
   * set the speed of the playback, that's handled by the speed() method.
   * 
   * @param int ifps
   * @see speed
   */
  public void frameRate(float ifps) {
    // We calculate the target ratio in the case both the 
    // current and target framerates are valid (greater than
    // zero), otherwise we leave it as 1.
    float f = (0 < ifps && 0 < fps) ? ifps / fps : 1;

    if (playing) {
      gplayer.pause();
    }

    long t = gplayer.queryPosition(TimeUnit.NANOSECONDS);
    
    boolean res;
    long start, stop;
    if (rate > 0) {
      start = t;
      stop = -1;
    } else {
      start = 0;
      stop = t;
    }    
    
    res = gplayer.seek(rate * f, Format.TIME, SeekFlags.FLUSH,
                       SeekType.SET, start, SeekType.SET, stop);
              
    if (!res) {
      System.err.println("Seek operation failed.");
    }
    
    if (playing) {
      gplayer.play();
    }

    fps = ifps;    
  }

  /**
   * Set a multiplier for how fast/slow the movie should be run. The default is
   * 1.0. speed(2) will play the movie at double speed (2x). speed(0.5) will
   * play at half speed. speed(-1) will play backwards at regular speed.
   * 
   * @param float irate
   */
  public void speed(float irate) {
    // If the frameRate() method is called continuously with very similar
    // rate values, playback might become sluggish. This condition attempts
    // to take care of that.
    if (PApplet.abs(rate - irate) > 0.1) {   
      rate = irate;
      frameRate(fps); // The framerate is the same, but the rate (speed) could be different.
    }
  }

  /**
   * Get the full length of the current stream (in seconds).
   * 
   * @return float
   */
  public float duration() {
    float sec = gplayer.queryDuration().toSeconds();
    float nanosec = gplayer.queryDuration().getNanoSeconds();
    return sec + GSVideo.nanoSecToSecFrac(nanosec);
  }  
  
  /**
   * Return the current time in seconds.
   * 
   * @return float
   */
  public float time() {
    float sec = gplayer.queryPosition().toSeconds();
    float nanosec = gplayer.queryPosition().getNanoSeconds();
    return sec + GSVideo.nanoSecToSecFrac(nanosec);
  }

  /**
   * Get the full length of this movie (in frames).
   * 
   * @return float
   */
  public long length() {
    return (int)(duration() * getSourceFrameRate());
  }  
  
  /**
   * Return the current frame.
   * 
   * @return int
   */
  public int frame() {
    return (int)(time() * getSourceFrameRate());
  }
  
  /**
   * Jump to a specific location (in seconds). The number is a float so
   * fractions of seconds can be used.
   * 
   * @param float where
   */
  public void jump(float where) {
    if (playing) {
      gplayer.pause();
    }
    
    boolean res;
    long start = GSVideo.secToNanoLong(where);
    long stop = -1; // or whatever > new_pos
    
    res = gplayer.seek(1.0, Format.TIME, SeekFlags.FLUSH,
                       SeekType.SET, start, SeekType.SET, stop);
    
    if (!res) {
      System.err.println("Seek operation failed.");
    }    

    if (playing) {
      gplayer.play();
    }
  }

  /**
   * Jump to a specific frame.
   * 
   * @param frame int
   */  
  public void jump(int frame) {
    float srcFramerate = getSourceFrameRate();
    
    // The duration of a single frame:
    float frameDuration = 1 / srcFramerate;
    
    // We move to the middle of the frame by adding 0.5:
    float where = (frame + 0.5f) * frameDuration; 
    
    // Taking into account border effects:
    float diff = duration() - where;
    if (diff < 0) {
      where += diff - 0.25f * frameDuration;
    }
    
    jump(where);
  }  
  
  /**
   * Returns true if the stream is already producing frames.
   * 
   * @return boolean
   */  
  public boolean ready() {
    return 0 < bufSize && sinkReady;
  }
    
  /**
   * Return the true or false depending on whether there is a new frame ready to
   * be read.
   * 
   * @return boolean
   */
  public boolean available() {
    return available;
  }

  /**
   * Returns whether the media is playing or not.
   * 
   * @return boolean
   */
  public boolean isPlaying() {
    return playing;  
  }

  /**
   * Returns whether the media is paused or not. If isPlaying() and isPaused()
   * both return false it means that the media is stopped.
   * 
   * @return boolean
   */
  public boolean isPaused() {
    return paused;  
  }    
  
  /**
   * Returns whether the media is looping or not.
   * 
   * @return boolean
   */
  public boolean isLooping() {
    return repeat;
  }
  
  /**
   * Begin playing the stream, with no repeat.
   */
  public void play() {
    if (!sinkReady) {
      initSink();
    }    
    
    playing = true;
    paused = false;
    gplayer.play();
  }

  /**
   * Begin playing the stream, with repeat.
   */
  public void loop() {
    repeat = true;
    play();
  }

  /**
   * Shut off the repeating loop.
   */
  public void noLoop() {
    repeat = false;
  }

  /**
   * Pause the stream at its current time.
   */
  public void pause() {
    playing = false;
    paused = true;
    gplayer.pause(); 
  }

  /**
   * Stop the stream, and rewind.
   */
  public void stop() {
    if (playing) {      
      goToBeginning();
      playing = false;
    }
    paused = false;      
    gplayer.stop();
  }

  /**
   * Reads the current video frame or data buffer.
   */
  public synchronized void read() {
    if (fps <= 0) {
      // Framerate not set yet, so we obtain from stream,
      // which is already playing since we are in read().
      fps = getSourceFrameRate();
    }    
    
    if (streamType == GSVideo.VIDEO) {
      // We loadPixels() first to ensure that at least we always have a non-null
      // pixels array, even if without any valid image inside.
      loadPixels();
      
      if (copyBufferMode) {
        // The native buffer from gstreamer is copies to the destination object.
        if (natBuffer == null || copyBufferMethod == null) {
          return;
        }    
        
        if (firstFrame) {
          super.init(bufWidth, bufHeight, RGB);
          loadPixels();
          firstFrame = false;
        }
        
        IntBuffer rgbBuffer = natBuffer.getByteBuffer().asIntBuffer();
        try {
          copyBufferMethod.invoke(copyHandler, new Object[] { natBuffer, rgbBuffer, bufWidth, bufHeight });
        } catch (Exception e) {
          e.printStackTrace();
        }
        
        natBuffer = null;      
      } else {
        // Normal operation mode: the pixels just read from gstreamer
        // are copied to the pixels array. 
        if (copyPixels == null) {
          return;
        }  
        
        if (firstFrame) {
          super.init(bufWidth, bufHeight, RGB);
          loadPixels();
          firstFrame = false;
        }     
        
        int[] temp = pixels;
        pixels = copyPixels;
        updatePixels();
        copyPixels = temp;        
      }  
    } else if (streamType == GSVideo.RAW) {
      if (copyData == null) {
        return;
      }        
      
      dataCaps = tempDataCaps;
      if (data == null) {
        data = new byte[copyData.length];
      }
      
      byte[] temp = data;
      data = copyData;
      copyData = temp;
    }
    
    available = false;
  }

  /**
   * Goes to the first frame of the stream.
   */
  public void goToBeginning() {
    gplayer.seek(ClockTime.fromNanos(0));
  }

  /**
   * Change the volume. Values are from 0 to 1.
   * 
   * @param float v 
   */
  public void volume(float v) {
    if (playing) {
      gplayer.setVolume(v);
    }
  }

  /**
   * Returns the text string containing the filename of the media loaded.
   * 
   * @return String 
   */  
  public String getFilename() {
    return filename;
  }  
  
  protected void initGStreamer(PApplet parent, String filename, int type) {
    this.parent = parent;
    gplayer = null;

    File file;

    GSVideo.init();

    // first check to see if this can be read locally from a file.
    try {
      try {
        // first try a local file using the dataPath. usually this will
        // work ok, but sometimes the dataPath is inside a jar file,
        // which is less fun, so this will crap out.
        file = new File(parent.dataPath(filename));
        if (file.exists()) {
          gplayer = new PlayBin2("GSPlayer");
          gplayer.setInputFile(file);
        }
      } catch (Exception e) {
      } // ignored

      // read from a file just hanging out in the local folder.
      // this might happen when the video library is used with some
      // other application, or the person enters a full path name
      if (gplayer == null) {
        try {
          file = new File(filename);
          if (file.exists()) {
            gplayer = new PlayBin2("GSPlayer");
            gplayer.setInputFile(file);
          } else {
            System.err.println("File " + filename + " does not exist. Please check location.");  
          }
        } catch (Exception e) {
        }
      }
      // Network read needs to be implemented...
    } catch (SecurityException se) {
      // online, whups. catch the security exception out here rather than
      // doing it three times (or whatever) for each of the cases above.
    }

    if (gplayer == null) {
      parent.die("Could not load media file " + filename, null);
    }

    // we've got a valid media file! let's rock.
    try {
      this.filename = filename; // for error messages

      // register methods
      parent.registerDispose(this);
      
      setEventHandlerObject(parent);

      rate = 1.0f;
      fps = -1;
      sinkReady = false;      
      bufWidth = bufHeight = bufSize = 0;      
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    streamType = type;
  }  
  
  protected void initSink() {
    if (streamType == GSVideo.VIDEO) {
      
      if (copyHandler != null) {
        try {      
          copyBufferMethod = copyHandler.getClass().getMethod("addPixelsToBuffer",
              new Class[] { Object.class, IntBuffer.class, int.class, int.class });
          copyBufferMode = true;            
        } catch (Exception e) {
          // no such method, or an error.. which is fine, just ignore
          copyBufferMode = false;
        }
        
        if (copyBufferMode) {
          
          try {            
            Method meth = copyHandler.getClass().getMethod("setPixelSource", new Class[] { Object.class});
            meth.invoke(copyHandler, new Object[] { this });            
          } catch (Exception e) {
            copyBufferMode = false;
          }
          
          if (copyBufferMode) {
            natSink = new BufferDataAppSink("nat", copyMask,
              new BufferDataAppSink.Listener() {
                public void bufferFrame(int w, int h, Buffer buffer) {
                  invokeEvent(w, h, buffer);
                }
              });
          
            natSink.setAutoDisposeBuffer(false);
            gplayer.setVideoSink(natSink);
            // The setVideoSink() method sets the videoSink as a property of the PlayBin,
            // which increments the refcount of the videoSink element. Disposing here once
            // to decrement the refcount.
            natSink.dispose();                    
          }
        }
      }
      
      if (!copyBufferMode) {
        rgbSink = new RGBDataAppSink("rgb",
          new RGBDataAppSink.Listener() {
            public void rgbFrame(int w, int h, IntBuffer buffer) {
              invokeEvent(w, h, buffer);
            }
          });
        // Setting direct buffer passing in the video sink, so no new buffers are created
        // and disposed by the GC on each frame (thanks to Octavi Estape for pointing 
        // out this one).      
        rgbSink.setPassDirectBuffer(GSVideo.passDirectBuffer);
        gplayer.setVideoSink(rgbSink);
        // The setVideoSink() method sets the videoSink as a property of the PlayBin,
        // which increments the refcount of the videoSink element. Disposing here once
        // to decrement the refcount.
        rgbSink.dispose();      
      }
    } else if (streamType == GSVideo.AUDIO) {
      gplayer.setVideoSink(ElementFactory.make("fakesink", "audiosink"));
    } else if (streamType == GSVideo.RAW) {
      dataSink = new ByteDataAppSink("data",
        new ByteDataAppSink.Listener() {
          public void byteFrame(Caps caps, int size, ByteBuffer buffer) {
            invokeEvent(caps, size, buffer);
          }
        });
      dataSink.setPassDirectBuffer(GSVideo.passDirectBuffer);
      gplayer.setVideoSink(dataSink);     
      dataSink.dispose(); 
    } else {
      System.err.println("Unrecognized stream type: Please use VIDEO, AUDIO, or RAW.");
      return;
    }

    // Creating bus to handle end-of-stream event.
    Bus bus = gplayer.getBus();
    bus.connect(new Bus.EOS() {
      public void endOfStream(GstObject element) {
        eosEvent();
      }
    });
    
    sinkReady = true;    
  }
  
  protected synchronized void invokeEvent(int w, int h, IntBuffer buffer) {
    available = true;
    bufWidth = w;
    bufHeight = h;
    bufSize = w * h;
    
    if (copyPixels == null) {
      copyPixels = new int[w * h];
    }
    buffer.rewind();    
    try {
      buffer.get(copyPixels);
    } catch (BufferUnderflowException e) {
      e.printStackTrace();
      copyPixels = null;
      return;
    }

    if (playing) {
      // Creates a playerEvent.
      if (playerEventMethod != null) {
        try {
          playerEventMethod.invoke(eventHandler, new Object[] { this });
        } catch (Exception e) {
          System.err.println("error, disabling playerEvent() for " + filename);
          e.printStackTrace();
          playerEventMethod = null;
        }
      }
    }
  }

  protected synchronized void invokeEvent(int w, int h, Buffer buffer) {
    available = true;
    bufWidth = w;
    bufHeight = h;
    bufSize = w * h;
    natBuffer = buffer;     
    
    if (playing) {
      // Creates a playerEvent.
      if (playerEventMethod != null) {
        try {
          playerEventMethod.invoke(eventHandler, new Object[] { this });
        } catch (Exception e) {
          System.err.println("error, disabling movieEvent() for " + filename);
          e.printStackTrace();
          playerEventMethod = null;
        }
      }
    }
  }  
  
  protected synchronized void invokeEvent(Caps caps, int n, ByteBuffer buffer) {
    available = true;
    bufSize = n;
    
    tempDataCaps = caps.toString();
    
    if (copyData == null) {
      copyData = new byte[n];
    }
    buffer.rewind();    
    try {
      buffer.get(copyData);
    } catch (BufferUnderflowException e) {
      e.printStackTrace();
      copyData = null;
      return;
    }

    if (playing) {
      // Creates a playerEvent.
      if (playerEventMethod != null) {
        try {
          playerEventMethod.invoke(eventHandler, new Object[] { this });
        } catch (Exception e) {
          System.err.println("error, disabling playerEvent() for " + filename);
          e.printStackTrace();
          playerEventMethod = null;
        }
      }
    }  
  }
  
  public synchronized void disposeBuffer(Object buf) {
    ((Buffer)buf).dispose();
  }  
  
  protected void eosEvent() {
    if (repeat) {
      goToBeginning();
    } else {
      playing = false;
    }
  }  
}
