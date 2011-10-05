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
import java.net.URI;
import java.nio.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.*;

import org.gstreamer.*;
import org.gstreamer.Buffer;
import org.gstreamer.elements.*;

/**
   * ( begin auto-generated from Movie.xml )
   * 
   * Datatype for storing and playing movies in Apple's QuickTime format. 
   * Movies must be located in the sketch's data directory or an accessible 
   * place on the network to load without an error.
   * 
   * ( end auto-generated )
 * 
 * @webref video
 * @usage application
 */
public class Movie extends PImage implements PConstants {
  protected String filename;
  
  protected boolean playing = false;
  protected boolean paused = false;
  protected boolean repeat = false;
  
  protected float fps;
  protected float rate;
  protected int bufWidth;
  protected int bufHeight;
  
  protected PlayBin2 gplayer;
  
  protected Method movieEventMethod;
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
  
  protected boolean firstFrame = true;
  protected boolean seeking = false;  
  
  /**
   * Creates an instance of GSMovie loading the movie from filename.
   * 
   * @param parent PApplet
   * @param filename String
   */  
  public Movie(PApplet parent, String filename) {
    super(0, 0, RGB);
    initGStreamer(parent, filename);
  }

  /**
   * Releases the gstreamer resources associated to this movie object.
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
      
      copyPixels = null;
      if (rgbSink != null) {
        rgbSink.removeListener();
        rgbSink.dispose();
        rgbSink = null;
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
   * Prints all the gstreamer elements currently used in the
   * current player instance.
   * 
   */    
  public void printElements() {
    List<Element> list = gplayer.getElementsRecursive();
    PApplet.println(list);
    for (Element element : list) {
      PApplet.println(element.toString());
    }   
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
   * Uses a generic object as handler of the movie. This object should have a
   * movieEvent method that receives a GSMovie argument. This method will
   * be called upon a new frame read event. 
   * 
   */
  public void setEventHandlerObject(Object obj) {
    eventHandler = obj;

    try {
      movieEventMethod = eventHandler.getClass().getMethod("movieEvent",
          new Class[] { Movie.class });
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
   * ( begin auto-generated from Movie_frameRate.xml )
   * 
   * Sets how often frames are read from the movie. Setting the <b>fps</b> 
   * parameter to 4, for example, will cause 4 frames to be read per second.
   * 
   * ( end auto-generated )
   * 
   * @webref movie
   * @usage web_application 
   * @param ifps speed of the movie in frames per second
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
   * ( begin auto-generated from Movie_speed.xml )
   * 
   * Sets the relative playback speed of the movie. The <b>rate</b> 
   * parameters sets the speed where 2.0 will play the movie twice as fast, 
   * 0.5 will play at half the speed, and -1 will play the movie in normal 
   * speed in reverse.
   * 
   * ( end auto-generated )
   * 
   
   * @webref movie
   
   * @usage web_application 
   * @param irate speed multiplier for movie playback
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
   * ( begin auto-generated from Movie_duration.xml )
   * 
   * Returns the length of the movie in seconds. If the movie is 1 minute and 
   * 20 seconds long the value returned will be 80.0.
   * 
   * ( end auto-generated )
   * 
   * @webref movie
   * @usage web_application
   */
  public float duration() {
    float sec = gplayer.queryDuration().toSeconds();
    float nanosec = gplayer.queryDuration().getNanoSeconds();
    return sec + Video.nanoSecToSecFrac(nanosec);
  }  
  
  /**
   * ( begin auto-generated from Movie_time.xml )
   * 
   * Returns the location of the playback head in seconds. For example, if 
   * the movie has been playing for 4 seconds, the number 4.0 will be returned.
   * 
   * ( end auto-generated )
   * 
   * @webref movie
   * @usage web_application 
   */
  public float time() {
    float sec = gplayer.queryPosition().toSeconds();
    float nanosec = gplayer.queryPosition().getNanoSeconds();
    return sec + Video.nanoSecToSecFrac(nanosec);
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
    double sec = gplayer.queryPosition().toSeconds() + gplayer.queryPosition().getNanoSeconds() * 1E-9;    
    return (int)(Math.ceil(sec * getSourceFrameRate())) - 1;
  }

  /**
   * ( begin auto-generated from Movie_jump.xml )
   * 
   * Jumps to a specific location within a movie. The parameter <b>where</b> 
   * is in terms of seconds. For example, if the movie is 12.2 seconds long, 
   * calling <b>jump(6.1)</b> would go to the middle of the movie.
   * 
   * ( end auto-generated )
   * 
   * @webref movie
   * @usage web_application 
   * @param where position to jump to specified in seconds
   */
  public void jump(float where) {
    if (seeking) return;
    
    boolean res;
    long pos = Video.secToNanoLong(where);
    
    res = gplayer.seek(1.0, Format.TIME, SeekFlags.FLUSH,
                       SeekType.SET, pos, SeekType.NONE, -1);
    
    if (!res) {
      System.err.println("Seek operation failed.");
    }    
    
    // Will wait until any async state change (seek) has completed    
    seeking = true;
    gplayer.getState();
    seeking = false;
  }

  /**
   * Jump to a specific frame.
   * 
   * @param frame ???
   */  
  public void jump(int frame) {
    float srcFramerate = getSourceFrameRate();
    
    // The duration of a single frame:
    float frameDuration = 1.0f / srcFramerate;
    
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
    return 0 < bufWidth && 0 < bufHeight && sinkReady && !seeking;
  }
  
  /**
   * ( begin auto-generated from Movie_available.xml )
   * 
   * Returns "true" when a new movie frame is available to read.
   * 
   * ( end auto-generated )
   * 
   * @webref movie
   * @usage web_application 
   */
  public boolean available() {
    return available;
  }
  
  /**
   * Returns whether the movie is playing or not.
   * 
   * @return boolean
   */
  public boolean isPlaying() {
    return playing;  
  }

  /**
   * Returns whether the movie is paused or not. If isPlaying() and isPaused()
   * both return false it means that the movie is stopped.
   * 
   * @return boolean
   */
  public boolean isPaused() {
    return paused;  
  }  
  
  /**
   * Returns whether the movie is looping or not.
   * 
   * @return boolean
   */
  public boolean isLooping() {
    return repeat;
  }
  
  /**
   * ( begin auto-generated from Movie_play.xml )
   * 
   * Plays a movie one time and stops at the last frame.
   * 
   * ( end auto-generated )
   * 
   * @webref movie
   * @usage web_application
   */
  public void play() {
    if (seeking) return;
    
    if (!sinkReady) {
      initSink();
    }
    
    playing = true;
    paused = false;
    gplayer.play();    
  }

  /**
   * ( begin auto-generated from Movie_loop.xml )
   * 
   * Plays a movie continuously, restarting it when it is over.
   * 
   * ( end auto-generated )
   * 
   * @webref movie
   * @usage web_application
   */
  public void loop() {
    if (seeking) return;
    
    repeat = true;
    play();
  }

  /**
   * ( begin auto-generated from Movie_noLoop.xml )
   * 
   * If a movie is looping, calling noLoop() will cause it to play until the 
   * end and then stop on the last frame.
   * 
   * ( end auto-generated )
   * 
   * @webref movie
   * @usage web_application
   */
  public void noLoop() {
    if (seeking) return;
    
    if (!sinkReady) {
      initSink();
    }
    
    repeat = false;
  }

  /**
   * ( begin auto-generated from Movie_pause.xml )
   * 
   * Pauses a movie during playback. If a movie is started again with play(), 
   * it will continue from where it was paused.
   * 
   * ( end auto-generated )
   * 
   * @webref movie
   * @usage web_application
   */
  public void pause() {
    if (seeking) return;
    
    if (!sinkReady) {
      initSink();
    }
    
    playing = false;
    paused = true;
    gplayer.pause();    
  }

  /**
   * ( begin auto-generated from Movie_stop.xml )
   * 
   * Stops a movie from continuing. The playback returns to the beginning so 
   * when a movie is played, it will begin from the beginning.
   * 
   * ( end auto-generated )
   * 
   * @webref movie
   * @usage web_application
   */
  public void stop() {
    if (seeking) return;
    
    if (!sinkReady) {
      initSink();
    }
    
    if (playing) {      
      goToBeginning();
      playing = false;
    }
    paused = false;    
    gplayer.stop();
  }

  /**
   * ( begin auto-generated from Movie_read.xml )
   * 
   * Reads the current frame of the movie. 
   * 
   * ( end auto-generated )
   * 
   * @webref movie
   * @usage web_application
   */
  public synchronized void read() {
    if (fps <= 0) {
      // Framerate not set yet, so we obtain from stream,
      // which is already playing since we are in read().
      fps = getSourceFrameRate();
    }
    
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
        resize(bufWidth, bufHeight);      
        firstFrame = false;
      }
    
      int[] temp = pixels;
      pixels = copyPixels;
      updatePixels();
      copyPixels = temp;
    }
    
    available = false;
  }

  /**
   * Goes to the first frame of the movie.
   */
  public void goToBeginning() {
    jump(0.0f);   
  }

  /**
   * Goes to the last frame of the movie.
   */
  public void goToEnd() {
    jump(duration());
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
   * Returns the text string containing the filename of the video loaded.
   * 
   * @return String 
   */  
  public String getFilename() {
    return filename;
  }
  
  protected void initGStreamer(PApplet parent, String filename) {
    this.parent = parent;
    gplayer = null;

    File file;

    Video.init();

    // first check to see if this can be read locally from a file.
    try {
      try {
        // first try a local file using the dataPath. usually this will
        // work ok, but sometimes the dataPath is inside a jar file,
        // which is less fun, so this will crap out.
        file = new File(parent.dataPath(filename));
        if (file.exists()) {
          gplayer = new PlayBin2("GSMovie Player");          
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
            gplayer = new PlayBin2("GSMovie Player");            
            gplayer.setInputFile(file);
          }
        } catch (Exception e) {
          PApplet.println("Shit coming...");
          e.printStackTrace();
        }
      }

      // Network read...      
      if (gplayer == null) {
        try {
          PApplet.println("network read");
          gplayer = new PlayBin2("GSMovie Player");            
          gplayer.setURI(URI.create(filename));
        } catch (Exception e) {
          PApplet.println("Shit coming...");
          e.printStackTrace();
        }      
      }
      
    } catch (SecurityException se) {
      // online, whups. catch the security exception out here rather than
      // doing it three times (or whatever) for each of the cases above.
    }

    if (gplayer == null) {
      parent.die("Could not load movie file " + filename, null);
    }

    // we've got a valid movie! let's rock.
    try {
      // PApplet.println("we've got a valid movie! let's rock.");
      this.filename = filename; // for error messages

      // register methods
      parent.registerDispose(this);

      setEventHandlerObject(parent);
      
      rate = 1.0f;
      fps = -1;
      sinkReady = false;
      bufWidth = bufHeight = 0; 
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  protected void initSink() {
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
      rgbSink.setPassDirectBuffer(Video.passDirectBuffer);
      gplayer.setVideoSink(rgbSink);
      // The setVideoSink() method sets the videoSink as a property of the PlayBin,
      // which increments the refcount of the videoSink element. Disposing here once
      // to decrement the refcount.
      rgbSink.dispose();      
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
      // Creates a movieEvent.
      if (movieEventMethod != null) {
        try {
          movieEventMethod.invoke(eventHandler, new Object[] { this });
        } catch (Exception e) {
          System.err.println("error, disabling movieEvent() for " + filename);
          e.printStackTrace();
          movieEventMethod = null;
        }
      }
    }
  }
  
  protected synchronized void invokeEvent(int w, int h, Buffer buffer) {
    available = true;
    bufWidth = w;
    bufHeight = h;        
    natBuffer = buffer;     
    
    if (playing) {
      // Creates a movieEvent.
      if (movieEventMethod != null) {
        try {
          movieEventMethod.invoke(eventHandler, new Object[] { this });
        } catch (Exception e) {
          System.err.println("error, disabling movieEvent() for " + filename);
          e.printStackTrace();
          movieEventMethod = null;
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
