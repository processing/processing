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
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.*;

import org.gstreamer.*;
import org.gstreamer.Buffer;
import org.gstreamer.elements.*;
import org.gstreamer.interfaces.PropertyProbe;
import org.gstreamer.interfaces.Property;

/**
   * ( begin auto-generated from Capture.xml )
   * 
   * Datatype for storing and manipulating video frames from an attached 
   * capture device such as a camera. Use <b>Capture.list()</b> to show the 
   * names of any attached devices. Using the version of the constructor 
   * without <b>name</b> will attempt to use the last device used by a 
   * QuickTime program.
   * 
   * ( end auto-generated )
 * 
 * <h3>Advanced</h3>
 * Class for storing and manipulating video frames from an attached capture
 * device such as a camera.
 * @webref video
 * @usage application
 */
public class Capture extends PImage implements PConstants {
  protected String source;
  
  protected boolean capturing = false;
  
  protected String fps;  
  protected int bufWidth;
  protected int bufHeight;
  
  protected Pipeline gpipeline;
  protected Element gsource;
  
  protected Method captureEventMethod;
  protected Method copyBufferMethod;
  
  protected Object eventHandler;
  protected Object copyHandler;
  
  protected boolean available;
  protected boolean pipelineReady;   
  
  protected RGBDataAppSink rgbSink = null;
  protected int[] copyPixels = null;
  
  protected BufferDataAppSink natSink = null;
  protected Buffer natBuffer = null;
  protected boolean copyBufferMode = false;
  protected String copyMask;  
  
  protected boolean firstFrame = true;
  
  protected ArrayList<int[]> suppResList;
  protected ArrayList<String> suppFpsList;
  
  protected int reqWidth;
  protected int reqHeight;  
  
  /**
   * @param parent typically use "this"
   * @param requestWidth width of the frame
   * @param requestHeight height of the frame
   */
  public Capture(PApplet parent, int requestWidth, int requestHeight) {
    super(requestWidth, requestHeight, RGB);
    initPlatform(parent, requestWidth, requestHeight, new String[] {}, new int[] {},
                 new String[] {}, new String[] {}, "");
  }

  /**
   * <h3>Advanced</h3>
   * Constructor that takes resolution and framerate indicated as a single number.
   * 
   * @param frameRate number of frames to read per second
   */  
  public Capture(PApplet parent, int requestWidth, int requestHeight, int frameRate) {
    super(requestWidth, requestHeight, RGB);
    initPlatform(parent, requestWidth, requestHeight, new String[] {}, new int[] {},
                 new String[] {}, new String[] {}, frameRate + "/1");
  }

  /**
   * <h3>Advanced</h3>
   * This constructor allows to specify the camera name. In Linux, for example, this
   * should be a string of the form /dev/video0, /dev/video1, etc.
   * 
   * @param cameraName name of the camera
   */   
  public Capture(PApplet parent, int requestWidth, int requestHeight, String cameraName) {
    super(requestWidth, requestHeight, RGB);
    initPlatform(parent, requestWidth, requestHeight, new String[] {}, new int[] {},
                 new String[] { devicePropertyName() }, new String[] { cameraName }, "");
  }

  /**
   * <h3>Advanced</h3>
   * This constructor allows to specify the camera name and the desired framerate.
   */     
  public Capture(PApplet parent, int requestWidth, int requestHeight, int frameRate, 
                   String cameraName) {
    super(requestWidth, requestHeight, RGB);
    initPlatform(parent, requestWidth, requestHeight, new String[] {}, new int[] {},
                 new String[] { devicePropertyName() }, new String[] { cameraName }, 
                 frameRate + "/1");
  }  
  
  /**
   * <h3>Advanced</h3>
   * This constructor lets to indicate which source element to use (i.e.: v4l2src, 
   * osxvideosrc, dshowvideosrc, ksvideosrc, etc).
   *
   * @nowebref
   * @param sourceName ???
   */   
  public Capture(PApplet parent, int requestWidth, int requestHeight, int frameRate, 
                   String sourceName, String cameraName) {
    super(requestWidth, requestHeight, RGB);
    initGStreamer(parent, requestWidth, requestHeight, sourceName, new String[] {}, new int[] {}, 
                  new String[] { devicePropertyName() }, new String[] { cameraName }, 
                  frameRate + "/1");
  }

  /**
   * <h3>Advanced</h3>
   * This constructor accepts an arbitrary list of string properties for the source element.
   * The camera name could be one of these properties. The framerate must be specified
   * as a fraction string: 30/1, 15/2, etc.
   *
   * @nowebref
   * @param strPropNames ???
   * @param strPropValues ???
   */    
  public Capture(PApplet parent, int requestWidth, int requestHeight, String frameRate,
                   String sourceName, String[] strPropNames, String[] strPropValues) {
    super(requestWidth, requestHeight, RGB);
    initGStreamer(parent, requestWidth, requestHeight, sourceName, new String[] {}, new int[] {},
                  strPropNames, strPropValues, frameRate);
  }

  /**
   * <h3>Advanced</h3>
   * This constructor accepts an arbitrary list of string properties for the source element,
   * as well as a list of integer properties. This could be useful if a camera cannot by
   * specified by name but by index. Framerate must be a fraction string: 30/1, 15/2, etc.
   *
   * @nowebref
   * @param intPropNames ???
   * @param intPropValues ???
   */   
  public Capture(PApplet parent, int requestWidth, int requestHeight, String frameRate,
                   String sourceName, String[] strPropNames, String[] strPropValues,
                   String[] intPropNames, int[] intPropValues) {
    super(requestWidth, requestHeight, RGB);
    initGStreamer(parent, requestWidth, requestHeight, sourceName, intPropNames, intPropValues,
                  strPropNames, strPropValues, frameRate);
  }

  /**
   * Releases the gstreamer resources associated to this capture object.
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
   * Prints all the gstreamer elements currently used in the
   * current pipeline instance.
   * 
   */    
  public void printElements() {
    List<Element> list = gpipeline.getElementsRecursive();
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
      captureEventMethod = parent.getClass().getMethod("captureEvent",
          new Class[] { Capture.class });
    } catch (Exception e) {
      // no such method, or an error.. which is fine, just ignore
    }
  }
  
  /**
   * Returns true if the stream is already producing frames.
   * 
   * @return boolean
   */  
  public boolean ready() {
    return 0 < bufWidth && 0 < bufHeight && pipelineReady;
  }
  
  /**
   * ( begin auto-generated from Capture_available.xml )
   * 
   * Returns "true" when a new video frame is available to read.
   * 
   * ( end auto-generated )
   * 
   * @webref capture
   * @usage web_application
   */
  public boolean available() {
    return available;
  }

  /**
   * Returns whether the device is capturing frames or not.
   * 
   * @return boolean
   */
  public boolean isCapturing() {
    return capturing;  
  }

  
  /**
   * Starts the capture pipeline.
   */
  public void start() {
    boolean init = false;
    if (!pipelineReady) {
      initPipeline();
      init = true;
    }
    
    capturing = true;
    gpipeline.play();
    
    if (init) {
      // Resolution and FPS initialization needs to be done after the
      // pipeline is set to play.
      initResAndFps();
    }
  }

  /**
   * ( begin auto-generated from Capture_stop.xml )
   * 
   * Stops capturing frames from an attached device.
   * 
   * ( end auto-generated )
   * 
   * @webref capture
   * @usage web_application
   */
  public void stop() {
    capturing = false;
    gpipeline.stop();
  }  
  
  /** 
   * ( begin auto-generated from Capture_read.xml )
   * 
   * Reads the current video frame.
   * 
   * ( end auto-generated )
   * 
   * <h3>Advanced</h3>
   * This method() and invokeEvent() are now synchronized, so that invokeEvent()
   * can't be called whilst we're busy reading. Problematic frame error
   * fixed by Charl P. Botha <charlbotha.com>
   * 
   * @webref capture
   * @usage web_application
   */
  public synchronized void read() {
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
    
    available = false;
  }
  
  /**
   * Returns a list with the resolutions supported by the capture device.
   * Each element of the list is in turn an array of two int, first being
   * the width and second the height.
   * 
   * @return int[][]
   */  
  public int[][] resolutions() {
    int n = suppResList.size();
    int[][] res = new int[n][2];
    for (int i = 0; i < n; i++) {
      int[] wh = (int[])suppResList.get(i);
      res[i] = new int[] {wh[0], wh[1]};
    }
    return res;
  }

  /**
   * Returns a list with the framerates supported by the capture device,
   * expressed as a string like: 30/1, 15/2, etc.
   * 
   * @return String[]
   */  
  public String[] framerates() {
    int n = suppFpsList.size();
    String[] res = new String[n];
    for (int i = 0; i < n; i++) {
      res[i] = (String)suppFpsList.get(i);
    }
    return res;
  }  
  
  /**
   * ( begin auto-generated from Capture_list.xml )
   * 
   * Gets a list of all available capture devices such as a camera. Use 
   * <b>print()</b> to write the information to the text window.
   * 
   * ( end auto-generated )
   * 
   * @webref capture
   * @usage web_application
   */  
  static public String[] list() {
    if (PApplet.platform == LINUX) {
      return list("v4l2src");
    } else if (PApplet.platform == WINDOWS) {
      return list("dshowvideosrc");
    } else if (PApplet.platform == MACOSX) {
      return list("osxvideosrc");
    } else {
      return null;
    }
  }

  /**
   * <h3>Advanced</h3>
   * Get a list of all available captures as a String array. i.e.
   * println(Capture.list()) will show you the goodies.
   * 
   * @param sourceName String
   */
  static public String[] list(String sourceName) {
    return list(sourceName, devicePropertyName());
  }
  
  static protected String[] list(String sourceName, String propertyName) {
    Video.init();
    String[] valuesListing = new String[0];
    Element videoSource = ElementFactory.make(sourceName, "Source");
    PropertyProbe probe = PropertyProbe.wrap(videoSource);
    if (probe != null) {
      Property property = probe.getProperty(propertyName);
      if (property != null) {
        Object[] values = probe.getValues(property);
        if (values != null) {
          valuesListing = new String[values.length];
          for (int i = 0; i < values.length; i++)
            if (values[i] instanceof String)
              valuesListing[i] = (String) values[i];
        }
      }
    }
    return valuesListing;
  }

  /**
   * invokeEvent() and read() are synchronized so that they can not be
   * called simultaneously. when they were not synchronized, this caused
   * the infamous problematic frame crash.
   * found and fixed by Charl P. Botha <charlbotha.com>
   */
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
    
    // Creates a movieEvent.
    if (captureEventMethod != null) {
      try {
        captureEventMethod.invoke(eventHandler, new Object[] { this });
      } catch (Exception e) {
        System.err.println("error, disabling captureEvent() for capture object");
        e.printStackTrace();
        captureEventMethod = null;
      }
    }
  }

  protected synchronized void invokeEvent(int w, int h, Buffer buffer) {
    available = true;
    bufWidth = w;
    bufHeight = h;        
    natBuffer = buffer;
    
    // Creates a movieEvent.
    if (captureEventMethod != null) {
      try {
        captureEventMethod.invoke(eventHandler, new Object[] { this });
      } catch (Exception e) {
        System.err.println("error, disabling captureEvent() for capture object");
        e.printStackTrace();
        captureEventMethod = null;
      }
    }
  }  

  /**
   * Returns the name of the source element used for capture.
   * 
   * @return String 
   */
  public String getSource() {
    return source;
  }    
  
  // Tries to guess the best correct source elements for each platform.
  protected void initPlatform(PApplet parent, int requestWidth, int requestHeight,
                              String[] intPropNames, int[] intPropValues, 
                              String[] strPropNames, String[] strPropValues, 
                              String frameRate) {
    if (PApplet.platform == LINUX) {
      initGStreamer(parent, requestWidth, requestHeight, "v4l2src", intPropNames, intPropValues,
          strPropNames, strPropValues, frameRate);
    } else if (PApplet.platform == WINDOWS) {
      //initGStreamer(parent, requestWidth, requestHeight, "ksvideosrc", intPropNames,
      //     intPropValues, strPropNames, strPropValues, frameRate);
      initGStreamer(parent, requestWidth, requestHeight, "dshowvideosrc", intPropNames,
    		        intPropValues, strPropNames, strPropValues, frameRate);    	
      //init(requestWidth, requestHeight, "dshowvideosrc", intPropNames,
      //    intPropValues, strPropNames, strPropValues, frameRate, addDecoder, null, "");
    } else if (PApplet.platform == MACOSX) {
      initGStreamer(parent, requestWidth, requestHeight, "osxvideosrc", intPropNames,
          intPropValues, strPropNames, strPropValues, frameRate);
    } else {
      parent.die("Error: unrecognized platform.", null);
    }
  }
    
  // The main initialization here.
  protected void initGStreamer(PApplet parent, int requestWidth, int requestHeight, String sourceName,
                               String[] intPropNames, int[] intPropValues, 
                               String[] strPropNames, String[] strPropValues, String frameRate) {
    this.parent = parent;

    Video.init();

    // register methods
    parent.registerDispose(this);

    setEventHandlerObject(parent);

    gpipeline = new Pipeline("GSCapture");
    
    this.source = sourceName;
    
    fps = frameRate;
    reqWidth = requestWidth;
    reqHeight = requestHeight;    
    
    gsource = ElementFactory.make(sourceName, "Source");

    if (intPropNames.length != intPropValues.length) {
      parent.die("Error: number of integer property names is different from number of values.",
          null);
    }

    for (int i = 0; i < intPropNames.length; i++) {
      gsource.set(intPropNames[i], intPropValues[i]);
    }

    if (strPropNames.length != strPropValues.length) {
      parent.die("Error: number of string property names is different from number of values.",
        null);
    }

    for (int i = 0; i < strPropNames.length; i++) {
      gsource.set(strPropNames[i], strPropValues[i]);
    }    
    
    bufWidth = bufHeight = 0;
    pipelineReady = false;    
  }

  protected void initPipeline() {
    String fpsStr = "";
    if (!fps.equals("")) {
      // If the framerate string is empty we left the source element
      // to use the default value.      
      fpsStr = ", framerate=" + fps;
    }    
    
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
          String caps = "width=" + reqWidth + ", height=" + reqHeight + ", " + copyMask;
          
          natSink = new BufferDataAppSink("nat", caps,
              new BufferDataAppSink.Listener() {
                public void bufferFrame(int w, int h, Buffer buffer) {
                  invokeEvent(w, h, buffer);
                }
              });
        
          natSink.setAutoDisposeBuffer(false);
          
          // No need for rgbSink.dispose(), because the addMany() doesn't increment the
          // refcount of the videoSink object.      
          
          gpipeline.addMany(gsource, natSink);
          Element.linkMany(gsource, natSink);
        }      
      }
    }
    
    if (!copyBufferMode) {
      Element conv = ElementFactory.make("ffmpegcolorspace", "ColorConverter");

      Element videofilter = ElementFactory.make("capsfilter", "ColorFilter");
      videofilter.setCaps(new Caps("video/x-raw-rgb, width=" + reqWidth + ", height=" + reqHeight + 
                                   ", bpp=32, depth=24" + fpsStr));
      
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
      
      // No need for rgbSink.dispose(), because the addMany() doesn't increment the
      // refcount of the videoSink object.      
      
      gpipeline.addMany(gsource, conv, videofilter, rgbSink);
      Element.linkMany(gsource, conv, videofilter, rgbSink);    
    } 
    
    pipelineReady = true;     
  }
  
  protected void initResAndFps() {
    // The pipeline needs to be in playing state to be able to
    // report the supported resolutions and framerates of the 
    // capture device.
    getSuppResAndFpsList();
    
    boolean suppRes = !(0 < suppResList.size()); // Default value is true if resolution list empty.
    for (int i = 0; i < suppResList.size(); i++) {
      int[] wh = (int[])suppResList.get(i);
      if (reqWidth == wh[0] && reqHeight == wh[1]) {
        suppRes = true;
        break;
      }
    }
    
    if (!suppRes) {
      System.err.println("The requested resolution of " + reqWidth + "x" + reqHeight + " is not supported by the capture device.");
      System.err.println("Use one of the following resolutions instead:");
      for (int i = 0; i < suppResList.size(); i++) {
        int[] wh = (int[])suppResList.get(i);
        System.err.println(wh[0] + "x" + wh[1]);
      }
    }
    
    boolean suppFps = !(0 < suppFpsList.size()); // Default value is true if fps list empty.
    for (int i = 0; i < suppFpsList.size(); i++) {
      String str = (String)suppFpsList.get(i);
      if (fps.equals("") || fps.equals(str)) {
        suppFps = true;
        break;
      }
    }
    
    if (!suppFps) {
      System.err.println("The requested framerate of " + fps + " is not supported by the capture device.");
      System.err.println("Use one of the following framerates instead:");
      for (int i = 0; i < suppFpsList.size(); i++) {
        String str = (String)suppFpsList.get(i);
        System.err.println(str);
      }
    }    
  }
  
  protected void getSuppResAndFpsList() {
    suppResList = new ArrayList<int[]>();
    suppFpsList = new ArrayList<String>();
   
    for (Element src : gpipeline.getSources()) {
      for (Pad pad : src.getPads()) {
        
        Caps caps = pad.getCaps();
        int n = caps.size(); 
        for (int i = 0; i < n; i++) {           
          Structure str = caps.getStructure(i);
          
          if (!str.hasIntField("width") || !str.hasIntField("height")) continue;
          
          int w = ((Integer)str.getValue("width")).intValue();
          int h = ((Integer)str.getValue("height")).intValue();
          
          boolean newRes = true;
          // Making sure we didn't add this resolution already. 
          // Different caps could have same resolution.
          for (int j = 0; j < suppResList.size(); j++) {
            int[] wh = (int[])suppResList.get(j);
            if (w == wh[0] && h == wh[1]) {
              newRes = false;
              break;
            }
          }
          if (newRes) {
            suppResList.add(new int[] {w, h});
          }          
          
          if (PApplet.platform == WINDOWS) {
            // In Windows the getValueList() method doesn't seem to
            // return a valid list of fraction values, so working on
            // the string representation of the caps structure.
            String str2 = str.toString();
            
            int n0 = str2.indexOf("framerate=(fraction)");
            if (-1 < n0) {
              String temp = str2.substring(n0 + 20, str2.length());
              int n1 = temp.indexOf("[");
              int n2 = temp.indexOf("]");
              if (-1 < n1 && -1 < n2) {
                // A list of fractions enclosed between '[' and ']'
                temp = temp.substring(n1 + 1, n2);  
                String[] fractions = temp.split(",");
                for (int k = 0; k < fractions.length; k++) {
                  addFpsStr(fractions[k].trim());
                }
              } else {
                // A single fraction
                int n3 = temp.indexOf(",");
                int n4 = temp.indexOf(";");
                if (-1 < n3 || -1 < n4) {
                  int n5 = -1;
                  if (n3 == -1) {
                    n5 = n4;
                  } else if (n4 == -1) {
                    n5 = n3;
                  } else {
                    n5 = PApplet.min(n3, n4);
                  }
                  
                  temp = temp.substring(0, n5);
                  addFpsStr(temp.trim());
                }
              }
            }
          } else {
            boolean sigleFrac = false;
            try {
              Fraction fr = str.getFraction("framerate");
              addFps(fr);
              sigleFrac = true;
            } catch (Exception e) { 
            }
            
            if (!sigleFrac) { 
              ValueList flist = str.getValueList("framerate");
              // All the framerates are put together, but this is not
              // entirely accurate since there might be some of them'
              // that work only for certain resolutions.
              for (int k = 0; k < flist.getSize(); k++) {
                Fraction fr = flist.getFraction(k);
                addFps(fr);
              }
            }            
          }          
        }
      }
    }
  }
  
  protected void addFps(Fraction fr) {
    int frn = fr.numerator;
    int frd = fr.denominator;
    addFpsStr(frn + "/" + frd);
  }
  
  protected void addFpsStr(String frstr) {
    boolean newFps = true;
    for (int j = 0; j < suppFpsList.size(); j++) {
      String frstr0 = (String)suppFpsList.get(j);
      if (frstr.equals(frstr0)) {
        newFps = false;
        break;
      }
    }
    if (newFps) {
      suppFpsList.add(frstr);
    }      
  }
  
  static protected String devicePropertyName() {
    // TODO: Check the property names
    if (PApplet.platform == LINUX) {
      return "device"; // Is this correct?
    } else if (PApplet.platform == WINDOWS) {
      return "device-name";
    } else if (PApplet.platform == MACOSX) {
      return "device-name";
    } else {
      return "";
    }
  }
  
  static protected String indexPropertyName() {
    // TODO: Check the property names
    if (PApplet.platform == LINUX) {
      return "device-index"; // Is this correct? Probably not.
    } else if (PApplet.platform == WINDOWS) {
      return "device-index";
    } else if (PApplet.platform == MACOSX) {
      return "device";
    } else {
      return "";
    }
  }
  
  public synchronized void disposeBuffer(Object buf) {
    ((Buffer)buf).dispose();
  }   
}
