/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-12 Ben Fry and Casey Reas
  The previous version of this code was developed by Hernando Barragan

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

package processing.video;

import processing.core.*;

import java.nio.*;
import java.util.ArrayList;
import java.io.File;
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
  protected static String sourceElementName;
  protected static String devicePropertyName;
  protected static String indexPropertyName;
  // Default gstreamer capture plugin for each platform, and property names.
  static {
    if (PApplet.platform == MACOSX) {
      sourceElementName = "qtkitvideosrc";
      devicePropertyName = "device-name";
      indexPropertyName = "device-index";
    } else if (PApplet.platform == WINDOWS) {
      sourceElementName = "ksvideosrc";
      devicePropertyName = "device-name";
      indexPropertyName = "device-index";
    } else if (PApplet.platform == LINUX) {
      sourceElementName = "v4l2src";
      // The "device" property in v4l2src expects the device location
      // (/dev/video0, etc). v4l2src has "device-name", which requires the
      // human-readable name... but how to query in linux?.
      devicePropertyName = "device";
      indexPropertyName = "device-fd";
    } else {}
  }
  protected static boolean useResMacHack = true;

  public float frameRate;
  public Pipeline pipeline;

  protected boolean capturing = false;

  protected String frameRateString;
  protected int bufWidth;
  protected int bufHeight;

  protected String sourceName;
  protected Element sourceElement;

  protected Method captureEventMethod;
  protected Object eventHandler;

  protected boolean available;
  protected boolean pipelineReady;
  protected boolean newFrame;

  protected RGBDataAppSink rgbSink = null;
  protected int[] copyPixels = null;

  protected boolean firstFrame = true;

  protected int reqWidth;
  protected int reqHeight;

  protected boolean useBufferSink = false;
  protected boolean outdatedPixels = true;
  protected Object bufferSink;
  protected Method sinkCopyMethod;
  protected Method sinkSetMethod;
  protected Method sinkDisposeMethod;
  protected Method sinkGetMethod;
  protected String copyMask;
  protected Buffer natBuffer = null;
  protected BufferDataAppSink natSink = null;


  public Capture(PApplet parent) {
    String[] configs = Capture.list();
    if (configs.length == 0) {
      throw new RuntimeException("There are no cameras available for capture");
    }
    String name = getName(configs[0]);
    int[] size = getSize(configs[0]);
    String fps = getFrameRate(configs[0]);
    String idName;
    Object idValue;
    if (devicePropertyName.equals("")) {
      // For plugins without device name property, the name is casted
      // as an index
      idName = indexPropertyName;
      idValue = new Integer(PApplet.parseInt(name));
    } else {
      idName = devicePropertyName;
      idValue = name;
    }
    initGStreamer(parent, size[0], size[1], sourceElementName,
                  idName, idValue, fps);
  }


  public Capture(PApplet parent, String requestConfig) {
    String name = getName(requestConfig);
    int[] size = getSize(requestConfig);
    String fps = getFrameRate(requestConfig);
    String idName;
    Object idValue;
    if (devicePropertyName.equals("")) {
      // For plugins without device name property, the name is casted
      // as an index
      idName = indexPropertyName;
      idValue = new Integer(PApplet.parseInt(name));
    } else {
      idName = devicePropertyName;
      idValue = name;
    }
    initGStreamer(parent, size[0], size[1], sourceElementName,
                  idName, idValue, fps);
  }


  /**
   * @param parent typically use "this"
   * @param requestWidth width of the frame
   * @param requestHeight height of the frame
   */
  public Capture(PApplet parent, int requestWidth, int requestHeight) {
    super(requestWidth, requestHeight, RGB);
    initGStreamer(parent, requestWidth, requestHeight, sourceElementName,
                  null, null, "");
  }


  /**
   * <h3>Advanced</h3>
   * Constructor that takes resolution and framerate.
   *
   * @param frameRate number of frames to read per second
   */
  public Capture(PApplet parent, int requestWidth, int requestHeight,
                 int frameRate) {
    super(requestWidth, requestHeight, RGB);
    initGStreamer(parent, requestWidth, requestHeight, sourceElementName,
                  null, null, frameRate + "/1");
  }


  /**
   * <h3>Advanced</h3>
   * This constructor allows to specify resolution and camera name.
   *
   * @param cameraName name of the camera
   */
  public Capture(PApplet parent, int requestWidth, int requestHeight,
                 String cameraName) {
    super(requestWidth, requestHeight, RGB);
    String idName;
    Object idValue;
    if (devicePropertyName.equals("")) {
      // For plugins without device name property, the name is casted
      // as an index
      idName = indexPropertyName;
      idValue = new Integer(PApplet.parseInt(cameraName));
    } else {
      idName = devicePropertyName;
      idValue = cameraName;
    }
    initGStreamer(parent, requestWidth, requestHeight, sourceElementName,
                  idName, idValue, "");
  }


  /**
   * <h3>Advanced</h3>
   * This constructor allows to specify the camera name and the desired
   * framerate, in addition to the resolution.
   */
  public Capture(PApplet parent, int requestWidth, int requestHeight,
                 String cameraName, int frameRate) {
    super(requestWidth, requestHeight, RGB);
    String idName;
    Object idValue;
    if (devicePropertyName.equals("")) {
      // For plugins without device name property, the name is casted
      // as an index
      idName = indexPropertyName;
      idValue = new Integer(PApplet.parseInt(cameraName));
    } else {
      idName = devicePropertyName;
      idValue = cameraName;
    }
    initGStreamer(parent, requestWidth, requestHeight, sourceElementName,
                  idName, idValue, frameRate + "/1");
  }


  /**
   * Disposes all the native resources associated to this capture device.
   *
   * NOTE: This is not official API and may/will be removed at any time.
   */
  public void dispose() {
    if (pipeline != null) {
      try {
        if (pipeline.isPlaying()) {
          pipeline.stop();
          pipeline.getState();
        }
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

      pipeline.dispose();
      pipeline = null;

      parent.g.removeCache(this);
      parent.unregisterMethod("dispose", this);
      parent.unregisterMethod("post", this);
    }
  }


  /**
   * Finalizer of the class.
   */
  protected void finalize() throws Throwable {
    try {
      dispose();
    } finally {
      super.finalize();
    }
  }


  /**
   * ( begin auto-generated from Capture_available.xml )
   *
   * Returns "true" when a new video frame is available to read.
   *
   * ( end auto-generated )
   *
   * @webref capture
   * @brief Returns "true" when a new video frame is available to read
   */
  public boolean available() {
    return available;
  }


  /**
   * ( begin auto-generated from Capture_start.xml )
   *
   * Starts capturing frames from the selected device.
   *
   * ( end auto-generated )
   *
   * @webref capture
   * @brief Starts capturing frames from the selected device
   */
  public void start() {
    boolean init = false;
    if (!pipelineReady) {
      initPipeline();
      init = true;
    }

    capturing = true;
    pipeline.play();

    if (init) {
      checkResIsValid();
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
   * @brief Stops capturing frames from an attached device
   */
  public void stop() {
    if (!pipelineReady) {
      initPipeline();
    }

    capturing = false;
    pipeline.stop();
    pipeline.getState();
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
   * @brief Reads the current video frame
   */
  public synchronized void read() {
    if (frameRate < 0) {
      // Framerate not set yet, so we obtain from stream,
      // which is already playing since we are in read().
      frameRate = getSourceFrameRate();
    }

    if (useBufferSink) { // The native buffer from gstreamer is copied to the buffer sink.
      outdatedPixels = true;
      if (natBuffer == null) {
        return;
      }

      if (firstFrame) {
        super.init(bufWidth, bufHeight, ARGB);
        firstFrame = false;
      }

      if (bufferSink == null) {
        Object cache = parent.g.getCache(this);
        if (cache == null) {
          return;
        }
        setBufferSink(cache);
        getSinkMethods();
      }

      ByteBuffer byteBuffer = natBuffer.getByteBuffer();

      try {
        sinkCopyMethod.invoke(bufferSink,
          new Object[] { natBuffer, byteBuffer, bufWidth, bufHeight });
      } catch (Exception e) {
        e.printStackTrace();
      }

      natBuffer = null;
    } else { // The pixels just read from gstreamer are copied to the pixels array.
      if (copyPixels == null) {
        return;
      }

      if (firstFrame) {
        super.init(bufWidth, bufHeight, RGB);
        firstFrame = false;
      }

      int[] temp = pixels;
      pixels = copyPixels;
      updatePixels();
      copyPixels = temp;
    }

    available = false;
    newFrame = true;
  }


  public synchronized void loadPixels() {
    super.loadPixels();
    if (useBufferSink) {
      if (natBuffer != null) {
        // This means that the OpenGL texture hasn't been created so far (the
        // video frame not drawn using image()), but the user wants to use the
        // pixel array, which we can just get from natBuffer.
        IntBuffer buf = natBuffer.getByteBuffer().asIntBuffer();
        buf.rewind();
        buf.get(pixels);
        Video.convertToARGB(pixels, width, height);
      } else if (sinkGetMethod != null) {
        try {
          // sinkGetMethod will copy the latest buffer to the pixels array,
          // and the pixels will be copied to the texture when the OpenGL
          // renderer needs to draw it.
          sinkGetMethod.invoke(bufferSink, new Object[] { pixels });
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      outdatedPixels = false;
    }
  }


  public int get(int x, int y) {
    if (outdatedPixels) loadPixels();
    return super.get(x, y);
  }


  protected void getImpl(int sourceX, int sourceY,
                         int sourceWidth, int sourceHeight,
                         PImage target, int targetX, int targetY) {
    if (outdatedPixels) loadPixels();
    super.getImpl(sourceX, sourceY, sourceWidth, sourceHeight,
                  target, targetX, targetY);
  }


  ////////////////////////////////////////////////////////////

  // List methods.


  /**
   * ( begin auto-generated from Capture_list.xml )
   *
   * Gets a list of all available capture devices such as a camera. Use
   * <b>print()</b> to write the information to the text window.
   *
   * ( end auto-generated )
   *
   * @webref capture
   * @brief Gets a list of all available capture devices such as a camera
   */
  static public String[] list() {
    if (devicePropertyName.equals("")) {
      return list(sourceElementName, indexPropertyName);
    } else {
      return list(sourceElementName, devicePropertyName);
    }
  }


  static protected String[] list(String sourceName, String propertyName) {
    Video.init();
    ArrayList<String> devices = listDevices(sourceName, propertyName);

    ArrayList<String> configList = new ArrayList<String>();
    for (String device: devices) {
      ArrayList<String> resolutions = listResolutions(sourceName, propertyName,
                                                      device);
      if (0 < resolutions.size()) {
        for (String res: resolutions) {
          configList.add("name=" + device + "," + res);
        }
      } else {
        configList.add("name=" + device);
      }
    }

    String[] configs = new String[configList.size()];
    for (int i = 0; i < configs.length; i++) {
      configs[i] = configList.get(i);
    }

    return configs;
  }


  static protected ArrayList<String> listDevices(String sourceName,
                                                 String propertyName) {
    ArrayList<String> devices = new ArrayList<String>();
    try {
      // Using property-probe interface
      Element videoSource = ElementFactory.make(sourceName, "Source");
      PropertyProbe probe = PropertyProbe.wrap(videoSource);
      if (probe != null) {
        Property property = probe.getProperty(propertyName);
        if (property != null) {
          Object[] values = probe.getValues(property);
          if (values != null) {
            for (int i = 0; i < values.length; i++) {
              if (values[i] instanceof String) {
                devices.add((String)values[i]);
              } else if (values[i] instanceof Integer) {
                devices.add(((Integer)values[i]).toString());
              }
            }
          }
        }
      }
    } catch (IllegalArgumentException e) {
      if (PApplet.platform == LINUX) {
        // Linux hack to detect currently connected cameras
        // by looking for device files named /dev/video0, /dev/video1, etc.
        devices = new ArrayList<String>();
        String dir = "/dev";
        File libPath = new File(dir);
        String[] files = libPath.list();
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
            if (-1 < files[i].indexOf("video")) {
              devices.add("/dev/" + files[i]);
            }
          }
        }
      } else {
        PGraphics.showWarning("The capture plugin does not support " +
                              "device query!");
        devices = new ArrayList<String>();
      }
    }
    return devices;
  }


  static protected ArrayList<String> listResolutions(String sourceName,
                                                     String propertyName,
                                                     Object propertyValue) {
    // Creating temporary pipeline so that we can query
    // the resolutions supported by the device.
    Pipeline testPipeline = new Pipeline("test");
    Element source = ElementFactory.make(sourceName, "source");
    source.set(propertyName, propertyValue);

    BufferDataAppSink sink = new BufferDataAppSink("sink", "",
        new BufferDataAppSink.Listener() {
          public void bufferFrame(int w, int h, Buffer buffer) { }
        });
    testPipeline.addMany(source, sink);
    Element.linkMany(source, sink);

    // Play/pause sequence (with getState() calls to to make sure
    // all async operations are done) to trigger the capture momentarily
    // for the device and obtain its supported resolutions.
    testPipeline.play();
    testPipeline.getState();
    testPipeline.pause();
    testPipeline.getState();

    ArrayList<String> resolutions = new ArrayList<String>();
    addResFromSource(resolutions, source);

    testPipeline.stop();
    testPipeline.getState();

    if (sink != null) {
      sink.removeListener();
      sink.dispose();
    }

    testPipeline.dispose();
    return resolutions;
  }


  static protected void addResFromSource(ArrayList<String> res, Element src) {
    if (PApplet.platform == MACOSX && useResMacHack) {
      addResFromSourceMacHack(res, src);
    } else {
      addResFromSourceImpl(res, src);
    }
  }


  static protected void addResFromSourceImpl(ArrayList<String> res,
                                             Element src) {
    for (Pad pad : src.getPads()) {
      Caps caps = pad.getCaps();
      int n = caps.size();
      for (int i = 0; i < n; i++) {
        Structure str = caps.getStructure(i);

        if (!str.hasIntField("width") || !str.hasIntField("height")) continue;

        int w = ((Integer)str.getValue("width")).intValue();
        int h = ((Integer)str.getValue("height")).intValue();

        if (PApplet.platform == WINDOWS) {
          // In Windows the getValueList() method doesn't seem to
          // return a valid list of fraction values, so working on
          // the string representation of the caps structure.
          addResFromString(res, str.toString(), w, h);
        } else {
          addResFromStructure(res, str, w, h);
        }
      }
    }
  }


  // The problem on OSX, at least when using qtkitvideosrc, is that it is only
  // possible to obtain a single supported caps, the native maximum, using
  // getNegotiatedCaps. getCaps() just gives the maximum possible ranges that
  // are useless to build a list of supported resolutions. Using the fact that
  // QTKit allows to capture streams at arbitrary resolutions, then the list is
  // faked by repeatedly dividing the maximum by 2 until the width becomes too
  // small (or not divisible by 2).
  static protected void addResFromSourceMacHack(ArrayList<String> res,
                                                Element src) {
    for (Pad pad : src.getPads()) {
      Caps caps = pad.getNegotiatedCaps();
      int n = caps.size();
      if (0 < n) {
        Structure str = caps.getStructure(0);

        if (!str.hasIntField("width") || !str.hasIntField("height")) return;

        int w = ((Integer)str.getValue("width")).intValue();
        int h = ((Integer)str.getValue("height")).intValue();
        while (80 <= w) {
          int num = 30;
          int den = 1;
          try {
            Fraction fr = str.getFraction("framerate");
            num = fr.numerator;
            den = fr.denominator;
          } catch (Exception e) {
          }

          res.add(makeResolutionString(w, h, num, den));
          if (num == 30 && den == 1) {
            // Adding additional framerates to allow for slower capture. Again,
            // QTKit can output frames at arbitrary rates.
            res.add(makeResolutionString(w, h, 15, 1));
            res.add(makeResolutionString(w, h, 1, 1));
          }

          if (w % 2 == 0 && h % 2 == 0) {
            w /= 2;
            h /= 2;
          } else {
            break;
          }
        }
      }
    }
  }


  static protected void addResFromString(ArrayList<String> res, String str,
                                         int w, int h) {
    int n0 = str.indexOf("framerate=(fraction)");
    if (-1 < n0) {
      String temp = str.substring(n0 + 20, str.length());
      int n1 = temp.indexOf("[");
      int n2 = temp.indexOf("]");
      if (-1 < n1 && -1 < n2) {
        // A list of fractions enclosed between '[' and ']'
        temp = temp.substring(n1 + 1, n2);
        String[] fractions = temp.split(",");
        for (int k = 0; k < fractions.length; k++) {
          String fpsStr = fractions[k].trim();
          res.add(makeResolutionString(w, h, fpsStr));
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
          String fpsStr = temp.trim();
          res.add(makeResolutionString(w, h, fpsStr));
        }
      }
    }
  }


  static protected void addResFromStructure(ArrayList<String> res,
                                            Structure str, int w, int h) {
    boolean singleFrac = false;
    try {
      Fraction fr = str.getFraction("framerate");
      res.add(makeResolutionString(w, h, fr.numerator, fr.denominator));
      singleFrac = true;
    } catch (Exception e) {
    }

    if (!singleFrac) {
      ValueList flist = null;

      try {
        flist = str.getValueList("framerate");
      } catch (Exception e) {
      }

      if (flist != null) {
        // All the framerates are put together, but this is not
        // entirely accurate since there might be some of them
        // that work only for certain resolutions.
        for (int k = 0; k < flist.getSize(); k++) {
          Fraction fr = flist.getFraction(k);
          res.add(makeResolutionString(w, h, fr.numerator, fr.denominator));
        }
      }
    }
  }


  static protected String makeResolutionString(int width, int height, int
                                               fpsNumerator,
                                               int fpsDenominator) {
    String res = "size=" + width + "x" + height + ",fps=" + fpsNumerator;
    if (fpsDenominator != 1) {
      res += "/" + fpsDenominator;
    }
    return res;
  }


  static protected String makeResolutionString(int width, int height,
                                               String fpsStr) {
    String res = "size=" + width + "x" + height;
    String[] parts = fpsStr.split("/");
    if (parts.length == 2) {
      int fpsNumerator = PApplet.parseInt(parts[0]);
      int fpsDenominator = PApplet.parseInt(parts[1]);
      res += ",fps=" + fpsNumerator;
      if (fpsDenominator != 1) {
        res += "/" + fpsDenominator;
      }
    }
    return res;
  }


  protected void checkResIsValid() {
    ArrayList<String> resolutions = new ArrayList<String>();
    addResFromSource(resolutions, sourceElement);

    boolean valid = resolutions.size() == 0;
    for (String res: resolutions) {
      if (validRes(res)) {
        valid = true;
        break;
      }
    }

    if (!valid) {
      String fpsStr = "";
      if (!frameRateString.equals("")) {
        fpsStr = ", " + frameRateString + "fps";
      }
      throw new RuntimeException("The requested resolution of " + reqWidth +
                                 "x" + reqHeight + fpsStr +
                                 " is not supported by the selected capture " +
                                 "device.\n");
    }
  }


  protected void checkValidDevices(String src) {
    ArrayList<String> devices;
    if (devicePropertyName.equals("")) {
      devices = listDevices(src, indexPropertyName);
    } else {
      devices = listDevices(src, devicePropertyName);
    }
    if (devices.size() == 0) {
      throw new RuntimeException("There are no capture devices connected to " +
                                 "this computer.\n");
    }
  }


  protected boolean validRes(String res) {
    int[] size = getSize(res);
    String fps = getFrameRate(res);
    return (reqWidth == 0 || reqHeight == 0 ||
            (size[0] == reqWidth && size[1] == reqHeight)) &&
           (frameRateString.equals("") || frameRateString.equals(fps));
  }


  ////////////////////////////////////////////////////////////

  // Initialization methods.


  // The main initialization here.
  protected void initGStreamer(PApplet parent, int rw, int rh, String src,
                               String idName, Object idValue,
                               String fps) {
    this.parent = parent;

    Video.init();
    checkValidDevices(src);

    // register methods
    parent.registerMethod("dispose", this);
    parent.registerMethod("post", this);

    setEventHandlerObject(parent);

    pipeline = new Pipeline("Video Capture");

    frameRateString = fps;
    if (frameRateString.equals("")) {
      frameRate = -1;
    } else {
      String[] parts = frameRateString.split("/");
      if (parts.length == 2) {
        int fpsDenominator = PApplet.parseInt(parts[0]);
        int fpsNumerator = PApplet.parseInt(parts[1]);
        frameRate = (float)fpsDenominator / (float)fpsNumerator;
      } else if (parts.length == 1) {
        frameRateString += "/1";
        frameRate = PApplet.parseFloat(parts[0]);
      } else {
        frameRateString = "";
        frameRate = -1;
      }
    }

    reqWidth = rw;
    reqHeight = rh;

    sourceName = src;
    sourceElement = ElementFactory.make(src, "Source");

    if (idName != null && !idName.equals("")) {
      sourceElement.set(idName, idValue);
    }

    bufWidth = bufHeight = 0;
    pipelineReady = false;
  }


  protected void initPipeline() {
    String whStr = "";
    if (0 < reqWidth && 0 < reqHeight) {
      whStr = "width=" + reqWidth + ", height=" + reqHeight;
    } else {
      PGraphics.showWarning("Resolution information not available, attempting" +
                            " to open the capture device at 320x240");
      whStr = "width=320, height=240";
    }

    String fpsStr = "";
    if (!frameRateString.equals("")) {
      // If the framerate string is empty we left the source element
      // to use the default value.
      fpsStr = ", framerate=" + frameRateString;
    }

    if (bufferSink != null || (Video.useGLBufferSink && parent.g.isGL())) {
      useBufferSink = true;

      if (bufferSink != null) {
        getSinkMethods();
      }

      if (copyMask == null || copyMask.equals("")) {
        initCopyMask();
      }

      String caps = whStr + fpsStr + ", " + copyMask;

      natSink = new BufferDataAppSink("nat", caps,
          new BufferDataAppSink.Listener() {
            public void bufferFrame(int w, int h, Buffer buffer) {
              invokeEvent(w, h, buffer);
            }
          });

      natSink.setAutoDisposeBuffer(false);

      // No need for rgbSink.dispose(), because the addMany() doesn't increment the
      // refcount of the videoSink object.

      pipeline.addMany(sourceElement, natSink);
      Element.linkMany(sourceElement, natSink);

    } else {
      Element conv = ElementFactory.make("ffmpegcolorspace", "ColorConverter");

      Element videofilter = ElementFactory.make("capsfilter", "ColorFilter");
      videofilter.setCaps(new Caps("video/x-raw-rgb, width=" + reqWidth +
                                   ", height=" + reqHeight +
                                   ", bpp=32, depth=24" + fpsStr));

      rgbSink = new RGBDataAppSink("rgb",
          new RGBDataAppSink.Listener() {
            public void rgbFrame(int w, int h, IntBuffer buffer) {
              invokeEvent(w, h, buffer);
            }
          });

      // Setting direct buffer passing in the video sink.
      rgbSink.setPassDirectBuffer(Video.passDirectBuffer);

      // No need for rgbSink.dispose(), because the addMany() doesn't increment
      // the refcount of the videoSink object.

      pipeline.addMany(sourceElement, conv, videofilter, rgbSink);
      Element.linkMany(sourceElement, conv, videofilter, rgbSink);
    }

    pipelineReady = true;
    newFrame = false;
  }


  /**
   * Uses a generic object as handler of the capture. This object should have a
   * captureEvent method that receives a Capture argument. This method will
   * be called upon a new frame read event.
   *
   */
  protected void setEventHandlerObject(Object obj) {
    eventHandler = obj;

    try {
      captureEventMethod = obj.getClass().getMethod("captureEvent", Capture.class);
      return;
    } catch (Exception e) {
      // no such method, or an error.. which is fine, just ignore
    }

    // The captureEvent method may be declared as receiving Object, rather
    // than Capture.
    try {
      captureEventMethod = obj.getClass().getMethod("captureEvent", Object.class);
      return;
    } catch (Exception e) {
      // no such method, or an error.. which is fine, just ignore
    }
  }


  ////////////////////////////////////////////////////////////

  // Stream event handling.


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
    fireCaptureEvent();
  }


  protected synchronized void invokeEvent(int w, int h, Buffer buffer) {
    available = true;
    bufWidth = w;
    bufHeight = h;
    if (natBuffer != null) {
      // To handle the situation where read() is not called in the sketch,
      // so that the native buffers are not being sent to the sink,
      // and therefore, not disposed by it.
      natBuffer.dispose();
    }
    natBuffer = buffer;
    fireCaptureEvent();
  }


  private void fireCaptureEvent() {
    if (captureEventMethod != null) {
      try {
        captureEventMethod.invoke(eventHandler, this);

      } catch (Exception e) {
        System.err.println("error, disabling captureEvent()");
        e.printStackTrace();
        captureEventMethod = null;
      }
    }
  }


  ////////////////////////////////////////////////////////////

  // Stream query methods.


  protected float getSourceFrameRate() {
    for (Element sink : pipeline.getSinks()) {
      for (Pad pad : sink.getPads()) {
        Fraction frameRate = org.gstreamer.Video.getVideoFrameRate(pad);
        if (frameRate != null) {
          return (float)frameRate.toDouble();
        }
      }
    }
    return 0;
  }


  protected String getName(String config) {
    String name = "";
    String[] parts = PApplet.split(config, ',');
    for (String part: parts) {
      if (-1 < part.indexOf("name")) {
        String[] values = PApplet.split(part, '=');
        if (0 < values.length) {
          name = values[1];
        }
      }
    }
    return name;
  }


  protected int[] getSize(String config) {
    int[] wh = {0, 0};
    String[] parts = PApplet.split(config, ',');
    for (String part: parts) {
      if (-1 < part.indexOf("size")) {
        String[] values = PApplet.split(part, '=');
        if (0 < values.length) {
          String[] whstr = PApplet.split(values[1], 'x');
          if (whstr.length == 2) {
            wh[0] = PApplet.parseInt(whstr[0]);
            wh[1] = PApplet.parseInt(whstr[1]);
          }
        }
      }
    }
    return wh;
  }


  protected String getFrameRate(String config) {
    String fps = "";
    String[] parts = PApplet.split(config, ',');
    for (String part: parts) {
      if (-1 < part.indexOf("fps")) {
        String[] values = PApplet.split(part, '=');
        if (0 < values.length) {
          fps = values[1];
          if (fps.indexOf("/") == -1) {
            fps += "/1";
          }
        }
      }
    }
    return fps;
  }


  ////////////////////////////////////////////////////////////

  // Buffer source interface.


  /**
   * Sets the object to use as destination for the frames read from the stream.
   * The color conversion mask is automatically set to the one required to
   * copy the frames to OpenGL.
   *
   * NOTE: This is not official API and may/will be removed at any time.
   *
   * @param Object dest
   */
  public void setBufferSink(Object sink) {
    bufferSink = sink;
    initCopyMask();
  }


  /**
   * Sets the object to use as destination for the frames read from the stream.
   *
   * NOTE: This is not official API and may/will be removed at any time.
   *
   * @param Object dest
   * @param String mask
   */
  public void setBufferSink(Object sink, String mask) {
    bufferSink = sink;
    copyMask = mask;
  }


  /**
   * NOTE: This is not official API and may/will be removed at any time.
   */
  public boolean hasBufferSink() {
    return bufferSink != null;
  }


  /**
   * NOTE: This is not official API and may/will be removed at any time.
   */
  public synchronized void disposeBuffer(Object buf) {
    ((Buffer)buf).dispose();
  }


  protected void getSinkMethods() {
    try {
      sinkCopyMethod = bufferSink.getClass().getMethod("copyBufferFromSource",
        new Class[] { Object.class, ByteBuffer.class, int.class, int.class });
    } catch (Exception e) {
      throw new RuntimeException("Capture: provided sink object doesn't have " +
                                 "a copyBufferFromSource method.");
    }

    try {
      sinkSetMethod = bufferSink.getClass().getMethod("setBufferSource",
        new Class[] { Object.class });
      sinkSetMethod.invoke(bufferSink, new Object[] { this });
    } catch (Exception e) {
      throw new RuntimeException("Capture: provided sink object doesn't have "+
                                 "a setBufferSource method.");
    }

    try {
      sinkDisposeMethod = bufferSink.getClass().getMethod("disposeSourceBuffer",
        new Class[] { });
    } catch (Exception e) {
      throw new RuntimeException("Capture: provided sink object doesn't have " +
                                 "a disposeSourceBuffer method.");
    }

    try {
      sinkGetMethod = bufferSink.getClass().getMethod("getBufferPixels",
        new Class[] { int[].class });
    } catch (Exception e) {
      throw new RuntimeException("Capture: provided sink object doesn't have " +
                                 "a getBufferPixels method.");
    }
  }


  protected void initCopyMask() {
    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
      copyMask = "red_mask=(int)0xFF000000, green_mask=(int)0xFF0000, blue_mask=(int)0xFF00";
    } else {
      copyMask = "red_mask=(int)0xFF, green_mask=(int)0xFF00, blue_mask=(int)0xFF0000";
    }
  }


  public synchronized void post() {
    if (useBufferSink && sinkDisposeMethod != null) {
      try {
        sinkDisposeMethod.invoke(bufferSink, new Object[] {});
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
