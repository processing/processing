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

package codeanticode.gsvideo;

import processing.core.*;

import java.io.File;
import java.nio.ByteBuffer;

import org.gstreamer.Buffer;
import org.gstreamer.elements.RGBDataFileSink;

/**
 * This class makes movies from a running program.
 */
public class GSMovieMaker {
  protected PApplet parent;
  protected boolean recording;
  protected RGBDataFileSink recorder;
  protected int width, height;

  public static final int THEORA = 0;
  public static final int XVID = 1;
  public static final int X264 = 2;
  public static final int DIRAC = 3;
  public static final int MJPEG = 4;
  public static final int MJPEG2K = 5;
  
  public static final int WORST = 0;
  public static final int LOW = 1;
  public static final int MEDIUM = 2;
  public static final int HIGH = 3;
  public static final int BEST = 4;  
  
  /**
   * Constructor that sets the codec to THEORA, MEDIUM quality and 30 fps. 
   * 
   */
  public GSMovieMaker(PApplet parent, int requestWidth, int requestHeight,
      String filename) {
    init(parent, requestWidth, requestHeight, filename, THEORA, MEDIUM, 30);
  }

  /**
   * Constructor that allows to set codec type and fps. 
   * 
   */  
  public GSMovieMaker(PApplet parent, int requestWidth, int requestHeight,
      String filename, int codecType, int ifps) {
    init(parent, requestWidth, requestHeight, filename, codecType, MEDIUM, ifps);
  }

  /**
   * Constructor that allows to set codec type, encoding quality and fps. 
   * 
   */    
  public GSMovieMaker(PApplet parent, int requestWidth, int requestHeight,
      String filename, int codecType, int codecQuality, int ifps) {
    init(parent, requestWidth, requestHeight, filename, codecType,
        codecQuality, ifps);
  }

  /**
   * Constructor that allows to set the gstreamer encoder and muxer by name.
   * Properties for encoder and muxer are left to wherever the default values are.  
   * 
   */
  public GSMovieMaker(PApplet parent, int requestWidth, int requestHeight,
      String filename, String encoder, String muxer, int ifps) {
    init(parent, requestWidth, requestHeight, filename, encoder, muxer, null, null, ifps);
  }  

  /**
   * Constructor that allows to set the gstreamer encoder and muxer by name, as
   * well as the properties.  
   * 
   */  
  public GSMovieMaker(PApplet parent, int requestWidth, int requestHeight,
      String filename, String encoder, String muxer, String[] propNames, Object[] propValues, int ifps) {
    init(parent, requestWidth, requestHeight, filename, encoder, muxer, propNames, propValues, ifps);
  }

  /**
   * Releases the gstreamer resources associated to this movie maker object.
   * It shouldn't be used after this.
   */  
  public void delete() {
    recorder.stop();
    recorder.dispose();
  }  
  
  /**
   * Same as delete.
   */  
  public void dispose() {
    delete();
  }  
  
  /**
   * Adds a new frame to the video being recorded.. 
   * 
   * @param pixels
   *          int[]
   */
  public void addFrame(int[] pixels) {
    if (recording && pixels.length == width * height) {
      Buffer srcBuffer = new Buffer(width * height * 4);

      ByteBuffer tmpBuffer = srcBuffer.getByteBuffer();
      tmpBuffer.clear();
      tmpBuffer.asIntBuffer().put(pixels);

      recorder.pushRGBFrame(srcBuffer);
    }
  }

  /**
   * Starts recording. 
   * 
   */  
  public void start() {
    recorder.start();
    recording = true;
  }

  /**
   * Finishes recording. 
   * 
   */    
  public void finish() {
    recording = false;
    recorder.stop();
  }

  /**
   * Returns the number of frames currently in the pre-encoding queue,
   * waiting to be encoded. 
   * 
   */
  public int getQueuedFrames() {
    return recorder.getNumQueuedFrames();
  }

  /**
   * Returns the number of frames dropped until now. 
   * 
   */  
  public int getDroppedFrames() {
    return recorder.getNumDroppedFrames();    
  }

  /**
   * Sets the maximum size of the pre-encoding and encoding queues. 
   * When the encoding queue is full, the frames start to be accumulated
   * in the pre-encoding queue. By setting the size of the pre-encoding
   * queue to zero, it can grow arbitrarily large.
   * 
   */  
  public void setQueueSize(int npre, int nenc) {
    recorder.setPreQueueSize(npre);
    recorder.setSrcQueueSize(nenc);
  }  
  
  /**
   * Returns true or false depending on whether recording is going
   * on right now or not.
   *
   * @returns boolean
   */    
  public boolean isRecording() {
    return recording;
  }

  protected void init(PApplet iparent, int requestWidth, int requestHeight,
      String filename, int codecType, int codecQuality, int ifps) {
    this.parent = iparent;

    GSVideo.init();

    // register methods
    parent.registerDispose(this);

    width = requestWidth;
    height = requestHeight;    
    
    String[] propNames = null;
    Object[] propValues = null;

    String encoder = "";
    String muxer = "";
    
    // Determining container based on the filename extension.
    String fn = filename.toLowerCase(); 
    if (fn.endsWith(".ogg")) {
      muxer = "oggmux";
    } else if (fn.endsWith(".avi")) {
      muxer = "avimux";  
    } else if (fn.endsWith(".mov")) {
      muxer = "qtmux";
    } else if (fn.endsWith(".flv")) {
      muxer = "flvmux";
    } else if (fn.endsWith(".mkv")) {
      muxer = "matroskamux";
    } else if (fn.endsWith(".mp4")) {
      muxer = "mp4mux";
    } else if (fn.endsWith(".3gp")) {
      muxer = "gppmux";
    } else if (fn.endsWith(".mpg")) {
      muxer = "ffmux_mpeg";      
    } else if (fn.endsWith(".mj2")) {
      muxer = "mj2mux";      
    } else {
      parent.die("Unrecognized video container", null);
    }
    
    // Configuring encoder.
    if (codecType == THEORA) {
      encoder = "theoraenc";

      propNames = new String[1];
      propValues = new Object[1];

      propNames[0] = "quality";
      Integer q = 31;
      if (codecQuality == WORST) {
        q = 0;
      } else if (codecQuality == LOW) {
        q = 15;
      } else if (codecQuality == MEDIUM) {
        q = 31;
      } else if (codecQuality == HIGH) {
        q = 47;
      } else if (codecQuality == BEST) {
        q = 63;
      }
      propValues[0] = q;      
    } else if (codecType == DIRAC) {
      encoder = "schroenc";
      
      propNames = new String[1];
      propValues = new Object[1];

      propNames[0] = "quality";
      Double q = 5.0d;
      if (codecQuality == WORST) {
        q = 0.0d;
      } else if (codecQuality == LOW) {
        q = 2.5d;
      } else if (codecQuality == MEDIUM) {
        q = 5.0d;
      } else if (codecQuality == HIGH) {
        q = 7.5d;
      } else if (codecQuality == BEST) {
        q = 10.0d;
      }
      propValues[0] = q; 
    } else if (codecType == XVID) {
      encoder = "xvidenc";
      
      // TODO: set Properties of xvidenc.
    } else if (codecType == X264) {
      encoder = "x264enc";
      
      propNames = new String[2];
      propValues = new Object[2];      
      
      // The pass property can take the following values:
      // (0): cbr              - Constant Bitrate Encoding (default)
      // (4): quant            - Constant Quantizer
      // (5): qual             - Constant Quality
      // (17): pass1            - VBR Encoding - Pass 1
      // (18): pass2            - VBR Encoding - Pass 2
      // (19): pass3            - VBR Encoding - Pass 3
      propNames[0] = "pass";
      Integer p = 5;
      propValues[0] = p;
      
      // When Constant Quality is specified for pass, then
      // the property quantizer is interpreted as the quality
      // level.
      propNames[1] = "quantizer";
      Integer q = 21;
      if (codecQuality == WORST) {
        q = 50;
      } else if (codecQuality == LOW) {
        q = 35;
      } else if (codecQuality == MEDIUM) {
        q = 21;
      } else if (codecQuality == HIGH) {
        q = 15;
      } else if (codecQuality == BEST) {
        q = 1;
      }
      propValues[1] = q;
      
      // The bitrate can be set with the bitrate property, which is integer and
      // has range: 1 - 102400. Default: 2048 Current: 2048.
      // This probably doesn't have any effect unless we set pass to cbr.
    } else if (codecType == MJPEG) {
      encoder = "jpegenc";
      
      propNames = new String[1];
      propValues = new Object[1];

      propNames[0] = "quality";
      Integer q = 85;
      if (codecQuality == WORST) {
        q = 0;
      } else if (codecQuality == LOW) {
        q = 30;
      } else if (codecQuality == MEDIUM) {
        q = 50;
      } else if (codecQuality == HIGH) {
        q = 85;
      } else if (codecQuality == BEST) {
        q = 100;
      }
      propValues[0] = q;      
    } else if (codecType == MJPEG2K) {
      encoder = "jp2kenc";
    } else {
      parent.die("Unrecognized video codec", null);
    }

    initRecorder(filename, ifps, encoder, muxer,  propNames, propValues);
  }

  protected void init(PApplet iparent, int requestWidth, int requestHeight, String filename, 
      String encoder, String muxer, String[] propNames, Object[] propValues, int ifps) {
    this.parent = iparent;

    GSVideo.init();

    // register methods
    parent.registerDispose(this);

    width = requestWidth;
    height = requestHeight;
    
    initRecorder(filename, ifps, encoder, muxer,  propNames, propValues);
  }  
  
  protected void initRecorder(String filename, int ifps, String encoder, String muxer,  
      String[] propNames, Object[] propValues) {
    
    File file = new File(parent.savePath(filename));
    recorder = new RGBDataFileSink("MovieMaker", width, height, ifps, encoder,
        propNames, propValues, muxer, file);    
    recording = false;
    setQueueSize(60, 30);
  }  
}
