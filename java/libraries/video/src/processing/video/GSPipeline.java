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

import java.nio.*;
import java.lang.reflect.*;

import org.gstreamer.*;
import org.gstreamer.Buffer;
import org.gstreamer.elements.*;

/**
 * This class allows to create a custom GStreamer pipeline.
 */
public class GSPipeline extends PImage implements PConstants {
  protected int streamType;
  protected String pipeline;

  protected boolean playing = false;
  protected boolean paused = false;
  protected boolean repeat = false;   
  
  protected int bufWidth;
  protected int bufHeight;
  protected int bufSize;  

  protected Pipeline gpipeline;  
  
  protected Method pipelineEventMethod;
  protected Method copyBufferMethod;   
  
  protected Object eventHandler;  
  protected Object copyHandler;  
 
  protected boolean available;
  protected boolean pipelineReady;  

  protected RGBDataSink rgbSink = null;
  protected int[] copyPixels = null;

  protected BufferDataSink natSink = null;
  protected Buffer natBuffer = null;
  protected boolean copyBufferMode = false;
  protected String copyMask;
    
  protected ByteDataSink dataSink = null;
  protected byte[] copyData = null;
  public byte[] data = null;

  public String dataCaps;
  protected String tempDataCaps;
    
  protected boolean firstFrame = true;
  
  /**
   * Creates an instance of GSPipeline using the provided pipeline
   * string.
   * 
   * @param parent PApplet
   * @param pstr String   
   */  
  public GSPipeline(PApplet parent, String pstr) {
    super(0, 0, RGB);
    initGStreamer(parent, pstr, GSVideo.VIDEO);
  }
  
  /**
   * Creates an instance of GSPipeline using the provided pipeline
   * string.
   * 
   * @param parent PApplet
   * @param pstr String
   * @param type int    
   */
  public GSPipeline(PApplet parent, String pstr, int type) {
    super(0, 0, RGB);
    initGStreamer(parent, pstr, type);
  }

  /**
   * Releases the gstreamer resources associated to this pipeline object.
   * It shouldn't be used after this.
   */
  public void delete() {
    if (gpipeline != null) {
      try {
        if (gpipeline.isPlaying()) {
          gpipeline.stop();
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
      
      gpipeline.dispose();
      gpipeline = null;
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
   * Uses a generic object as handler of the pipeline. This object should have a
   * pipelineEvent method that receives a GSPipeline argument. This method will
   * be called upon a new frame read event. 
   * 
   */
  public void setEventHandlerObject(Object obj) {
    eventHandler = obj;

    try {
      pipelineEventMethod = eventHandler.getClass().getMethod("pipelineEvent",
          new Class[] { GSPipeline.class });
    } catch (Exception e) {
      // no such method, or an error.. which is fine, just ignore
    }
  }
  
  /**
   * Get the full length of this movie (in seconds).
   * 
   * @return float
   */
  public float duration() {
    float sec = gpipeline.queryDuration().toSeconds();
    float nanosec = gpipeline.queryDuration().getNanoSeconds();
    return sec + GSVideo.nanoSecToSecFrac(nanosec);
  }  
  
  /**
   * Return the current time in seconds.
   * 
   * @return float
   */
  public float time() {
    float sec = gpipeline.queryPosition().toSeconds();
    float nanosec = gpipeline.queryPosition().getNanoSeconds();
    return sec + GSVideo.nanoSecToSecFrac(nanosec);
  }  
  
  /**
   * Jump to a specific location (in seconds). The number is a float so
   * fractions of seconds can be used.
   * 
   * @param float where
   */
  public void jump(float where) {
    if (playing) {
      gpipeline.pause();
    }
    
    boolean res;
    long start = GSVideo.secToNanoLong(where);
    long stop = -1; // or whatever > new_pos
    
    res = gpipeline.seek(1.0, Format.TIME, SeekFlags.FLUSH,
                     SeekType.SET, start, SeekType.SET, stop);
    
    if (!res) {
      System.err.println("Seek operation failed.");
    }    

    if (playing) {
      gpipeline.play();
    }
  }  
  
  /**
   * Returns true if the stream is already producing frames.
   * 
   * @return boolean
   */  
  public boolean ready() {
    return 0 < bufSize && pipelineReady;
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
   * Returns whether the stream is playing or not.
   * 
   * @return boolean
   */
  public boolean isPlaying() {
    return playing;  
  }

  /**
   * Returns whether the stream is paused or not. If isPlaying() and isPaused()
   * both return false it means that the stream is stopped.
   * 
   * @return boolean
   */
  public boolean isPaused() {
    return paused;  
  }  
  
  /**
   * Returns whether the stream is looping or not.
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
    if (!pipelineReady) {
      initPipeline();
    }    
    
    playing = true;
    paused = false;
    gpipeline.play();    
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
    gpipeline.pause();
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
    gpipeline.stop();
  }
  
  /**
   * Reads the current video frame.
   */
  public synchronized void read() {
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
    boolean res = gpipeline.seek(ClockTime.fromNanos(0));
    if (!res) {
      System.err.println("Seek operation failed.");
    }    
  }
  
  /**
   * Goes to the last frame of the stream.
   */
  public void goToEnd() {
    long nanos = gpipeline.queryDuration().getNanoSeconds();
    boolean res = gpipeline.seek(ClockTime.fromNanos(nanos));
    if (!res) {
      System.err.println("Seek operation failed.");
    }
  }
  
  /**
   * Get a float-value property from the pipeline. 
   * 
   * @param String name
   * @return boolean 
   */
  public float getProperty(String name) {
    if (playing) {
      return ((Number)gpipeline.get(name)).floatValue();
    }
    return 0;
  }    
  
  /**
   * Set a float-value property in the pipeline. 
   * 
   * @param String name
   * @param float v  
   */
  public void setProperty(String name, float v) {
    if (playing) {
      gpipeline.set(name, v);
    }
  }  
  
  /**
   * Change the volume. Values are from 0 to 1. It will fail
   * if the pipeline doesn't have a volume property available.
   * 
   * @param float v   
   */
  public void volume(float v) {
    setProperty("volume", v);
  }
  
  /**
   * Returns the text string used to build the pipeline.
   * 
   * @return String 
   */
  public String getPipeline() {
    return pipeline;
  }  

  protected void initGStreamer(PApplet parent, String pstr, int type) {
    this.parent = parent;

    gpipeline = null;

    GSVideo.init();

    // register methods
    parent.registerDispose(this);

    setEventHandlerObject(parent);

    pipeline = pstr;    
    
    streamType = type;
    bufWidth = bufHeight = bufSize = 0;
    pipelineReady = false;    
  }
  
  protected void initPipeline() {
    // Determining if the last element is fakesink or filesink.
    int idx;
    String lastElem, lastElemName;
    String[] parts;

    idx = pipeline.lastIndexOf('!');
    lastElem = pipeline.substring(idx + 1, pipeline.length()).trim();

    parts = lastElem.split(" ");
    if (0 < parts.length)
      lastElemName = parts[0];
    else
      lastElemName = "";

    boolean fakeSink = lastElemName.equals("fakesink");
    boolean fileSink = lastElemName.equals("filesink");

    if (PApplet.platform == WINDOWS) {
      // Single backward slashes are replaced by double backward slashes,
      // otherwise gstreamer won't understand file paths.
      pipeline = pipeline.replace("\\", "\\\\");
    }
    
    if (fakeSink || fileSink) {
      // If the pipeline ends in a fakesink or filesink element, the RGBDataSink
      // is not added at the end of it...
      gpipeline = Pipeline.launch(pipeline);
      
    } else {
      if (streamType == GSVideo.VIDEO) {
        // For video pipelines, we add an RGBDataSink or NativeDataSink element at the end.
        
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
              String caps = " ! ffmpegcolorspace ! video/x-raw-rgb, bpp=32, depth=24, endianness=(int)4321, ";
              caps += copyMask;
              
              StringBuilder finalPipeStr = new StringBuilder(pipeline);
              finalPipeStr.append(caps);
              finalPipeStr.append(" ! fakesink name=nat");   
              
              pipeline = finalPipeStr.toString();
              gpipeline = Pipeline.launch(pipeline);
              natSink = new BufferDataSink("nat", gpipeline, 
                new BufferDataSink.Listener() {
                  public void bufferFrame(int w, int h, Buffer buffer) {
                    invokeEvent(w, h, buffer);
                  }
                });
              
              natSink.setAutoDisposeBuffer(false);
            }
          }          
        }
        
        if (!copyBufferMode) {
          // Making sure we are using the right color space and color masks:
          String caps = " ! ffmpegcolorspace ! video/x-raw-rgb, bpp=32, depth=24, endianness=(int)4321, ";
          // JNA creates ByteBuffer using native byte order, set masks according to that.
          if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN)
            caps += "red_mask=(int)0xFF00, green_mask=(int)0xFF0000, blue_mask=(int)0xFF000000";
          else
            caps += "red_mask=(int)0xFF0000, green_mask=(int)0xFF00, blue_mask=(int)0xFF";
            
          StringBuilder finalPipeStr = new StringBuilder(pipeline);
          finalPipeStr.append(caps);
          finalPipeStr.append(" ! fakesink name=rgb");
          
          pipeline = finalPipeStr.toString();
          gpipeline = Pipeline.launch(pipeline);
          rgbSink = new RGBDataSink("rgb", gpipeline, new RGBDataSink.Listener() {
            public void rgbFrame(boolean pre, int w, int h, IntBuffer buffer) {
              invokeEvent(w, h, buffer);
            }
          });
          
          // Setting direct buffer passing in the video sink, so no new buffers are created
          // and disposed by the GC on each frame (thanks to Octavi Estape for pointing 
          // out this one).            
          rgbSink.setPassDirectBuffer(GSVideo.passDirectBuffer);    
          
          // No need for videoSink.dispose(), because the append() doesn't increment the
          // refcount of the videoSink object.
        }
        
        
      } else if (streamType == GSVideo.AUDIO) {
        // For audio pipelines, we launch the pipeline as it is.
        gpipeline = Pipeline.launch(pipeline);       
      } else if (streamType == GSVideo.RAW) {
        StringBuilder finalPipeStr = new StringBuilder(pipeline);
        finalPipeStr.append(" ! fakesink name=data");
        
        pipeline = finalPipeStr.toString();
        gpipeline = Pipeline.launch(pipeline);
        dataSink = new ByteDataSink("data", gpipeline, 
          new ByteDataSink.Listener() {
            public void byteFrame(boolean pre, Caps caps, int size, ByteBuffer buffer) {
              invokeEvent(caps, size, buffer);
            }
          });
        dataSink.setPassDirectBuffer(GSVideo.passDirectBuffer);
      } else {
        System.err.println("Unrecognized stream type: Please use VIDEO, AUDIO, or RAW.");
        return;
      }
    }
    
    // Creating bus to handle end-of-stream event.
    Bus bus = gpipeline.getBus();
    bus.connect(new Bus.EOS() {
      public void endOfStream(GstObject element) {
        eosEvent();
      }
    });
    
    pipelineReady = true;
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

    // Creates a pipelineEvent.
    if (pipelineEventMethod != null) {
      try {
        pipelineEventMethod.invoke(eventHandler, new Object[] { this });
      } catch (Exception e) {
        System.err.println("error, disabling pipelineEvent() for " + pipeline);
        e.printStackTrace();
        pipelineEventMethod = null;
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
      // Creates a movieEvent.
      if (pipelineEventMethod != null) {
        try {
          pipelineEventMethod.invoke(eventHandler, new Object[] { this });
        } catch (Exception e) {
          System.err.println("error, disabling movieEvent() for " + pipeline);
          e.printStackTrace();
          pipelineEventMethod = null;
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
      if (pipelineEventMethod != null) {
        try {
          pipelineEventMethod.invoke(eventHandler, new Object[] { this });
        } catch (Exception e) {
          System.err.println("error, disabling pipelineEvent() for " + pipeline);
          e.printStackTrace();
          pipelineEventMethod = null;
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
