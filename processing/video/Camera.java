/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Camera - whatchin shit on tv
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-05 Ben Fry and Casey Reas
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

import java.lang.reflect.*;

import quicktime.*;
import quicktime.qd.*;
import quicktime.std.*;
import quicktime.std.sg.*;
import quicktime.util.RawEncodedImage;


// this is the useful ref page
// http://developer.apple.com/documentation/Java/Reference/1.4.1/Java141API_QTJ/constant-values.html

public class Camera extends PImage implements Runnable {

  // there are more, but these are all we'll provide for now
  static public final int COMPOSITE = StdQTConstants.compositeIn;  // 0
  static public final int SVIDEO = StdQTConstants.sVideoIn;  // 1
  static public final int COMPONENT = StdQTConstants.rgbComponentIn;  // 2
  static public final int TUNER = StdQTConstants.tvTunerIn;  // 6

  static public final int NTSC = StdQTConstants.ntscIn;
  static public final int PAL = StdQTConstants.palIn;
  static public final int SECAM = StdQTConstants.secamIn;

  PApplet parent;
  Method cameraEventMethod;
  String name; // keep track for error messages (unused)
  Thread runner;

  boolean available = false;

  /** Temporary storage for the raw image data read directly from the camera */
  public int data[];

  public int dataWidth;
  public int dataHeight;
  public int dataRowBytes;

  /** True if this image is currently being cropped */
  public boolean crop;

  public int cropX;
  public int cropY;
  public int cropW;
  public int cropH;

  public int framerate;

  public RawEncodedImage raw;
  public SequenceGrabber capture;

  /** the guy who's doing all the work */
  public SGVideoChannel channel;


  static {
    try {
      QTSession.open();
    } catch (QTException e) {
      e.printStackTrace();
    }
    QTRuntimeException.registerHandler(new QTRuntimeHandler() {
        public void exceptionOccurred(QTRuntimeException e,
                                      Object obj, String s, boolean flag) {
          System.err.println("Problem inside Camera");
          e.printStackTrace();
        }
      });
  }


  public Camera(PApplet parent, int requestWidth, int requestHeight) {
    this(parent, null, requestWidth, requestHeight, 30);
  }

  public Camera(PApplet parent, int reqWidth, int reqHeight, int framerate) {
    this(parent, null, reqWidth, reqHeight, framerate);
  }

  public Camera(PApplet parent, String name, int reqWidth, int reqHeight) {
    this(parent, name, reqWidth, reqHeight, 30);
  }


  /**
   * If 'name' is null or the empty string, it won't set a specific
   * device, which means that QuickTime will use that last device
   * used by a QuickTime application.
   * <P>
   * If the following function:
   * public void cameraEvent(Camera c)
   * is defined int the host PApplet, then it will be called every
   * time a new frame is available from the camera.
   */
  public Camera(PApplet parent, String name,
                int requestWidth, int requestHeight, int framerate) {
    this.parent = parent;
    this.name = name;
    this.framerate = framerate;

    try {
      QDRect qdrect = new QDRect(requestWidth, requestHeight);
      QDGraphics qdgraphics = new QDGraphics(qdrect);

      capture = new SequenceGrabber();
      capture.setGWorld(qdgraphics, null);

      channel = new SGVideoChannel(capture);
      channel.setBounds(qdrect);
      channel.setUsage(2);  // what is this usage number?
      capture.startPreview();  // maybe this comes later?

      PixMap pixmap = qdgraphics.getPixMap();
      raw = pixmap.getPixelData();

      /*
      if (name == null) {
        channel.settingsDialog();

      } else if (name.length() > 0) {
        channel.setDevice(name);
      }
      */
      if ((name != null) && (name.length() > 0)) {
        channel.setDevice(name);
      }

      dataRowBytes = raw.getRowBytes();
      dataWidth = dataRowBytes / 4;
      dataHeight = raw.getSize() / dataRowBytes;

      if (dataWidth != requestWidth) {
        crop = true;
        cropX = 0;
        cropY = 0;
        cropW = requestWidth;
        cropH = requestHeight;
      }
      // initialize my PImage self
      super.init(requestWidth, requestHeight, RGB);

      runner = new Thread(this);
      runner.start();

      parent.registerDispose(this);

      try {
        cameraEventMethod =
          parent.getClass().getMethod("cameraEvent",
                                      new Class[] { Camera.class });
      } catch (Exception e) {
        // no such method, or an error.. which is fine, just ignore
      }

    } catch (StdQTException qte) {
      //qte.printStackTrace();

      int errorCode = qte.errorCode();
      if (errorCode == Errors.couldntGetRequiredComponent) {
        // this can happen when the camera isn't available or
        // wasn't shut down properly
        parent.die("No camera could be found, " +
                   "or the VDIG is not installed correctly.", qte);
      } else {
        parent.die("Error while setting up Camera", qte);
      }
    } catch (Exception e) {
      parent.die("Error while setting up Camera", e);
    }
  }


  /**
   * True if a frame is ready to be read.
   * <PRE>
   * // put this somewhere inside draw
   * if (camera.available()) camera.read();
   * </PRE>
   * Alternatively, you can use cameraEvent(Camera c) to notify you
   * whenever available() is set to true. In which case, things might
   * look like this:
   * <PRE>
   * public void cameraEvent(Camera c) {
   *   c.read();
   *   // do something exciting now that c has been updated
   * }
   * </PRE>
   */
  public boolean available() {
    return available;
  }


  /**
   * Set the video to crop from its original.
   * <P>
   * It seems common that cameras add lines to the top or bottom
   * of an image, so this can be useful for removing them.
   * Internally, the pixel buffer size returned from QuickTime is
   * often a different size than requested, so crop will be set
   * more often than not.
   */
  public void crop(int x, int y, int w, int h) {
    if (imageMode == CORNERS) {
      w -= x;  // w was actually x2
      h -= y;  // h was actually y2
    }

    crop = true;
    cropX = Math.max(0, x);
    cropY = Math.max(0, y);
    cropW = Math.min(w, dataWidth);
    cropH = Math.min(dataHeight, y + h) - cropY;

    // if size has changed, re-init this image
    if ((cropW != width) || (cropH != height)) {
      init(w, h, RGB);
    }
  }


  /**
   * Remove the cropping (if any) of the image.
   * <P>
   * By default, cropping is often enabled to trim out black pixels.
   * But if you'd rather deal with them yourself (so as to avoid
   * an extra lag while the data is moved around) you can shut it off.
   */
  public void noCrop() {
    crop = false;
  }


  public void read() {
    //try {
    //synchronized (capture) {
    synchronized (pixels) {
      //long t1 = System.currentTimeMillis();

      if (crop) {
        /*
        // f#$)(#$ing quicktime / jni is so g-d slow that this
        // code takes literally 100x longer to run
        int sourceOffset = cropX*4 + cropY*dataRowBytes;
        int destOffset = 0;
        for (int y = 0; y < cropH; y++) {
          raw.copyToArray(sourceOffset, pixels, destOffset, cropW);
          sourceOffset += dataRowBytes;
          destOffset += width;
        }
        */
        if (data == null) {
          data = new int[dataWidth * dataHeight];
        }
        raw.copyToArray(0, data, 0, dataWidth * dataHeight);
        int sourceOffset = cropX + cropY*dataWidth;
        int destOffset = 0;
        for (int y = 0; y < cropH; y++) {
          System.arraycopy(data, sourceOffset, pixels, destOffset, cropW);
          sourceOffset += dataWidth;
          destOffset += width;
        }
      } else {  // no crop, just copy directly
        raw.copyToArray(0, pixels, 0, width * height);
      }
      //long t2 = System.currentTimeMillis();
      //System.out.println(t2 - t1);

      available = false;
      // mark this image as modified so that PGraphics2 and PGraphicsGL
      // willproperly re-blit and draw this guy
      updatePixels();
    }
  }


  public void run() {
    while ((Thread.currentThread() == runner) && (capture != null)) {
      try {
        synchronized (capture) {
          capture.idle();
          //read();
          available = true;

          if (cameraEventMethod != null) {
            try {
              cameraEventMethod.invoke(parent, new Object[] { this });
            } catch (Exception e) {
              System.err.println("Disabling cameraEvent() for " + name +
                                 " because of an error.");
              e.printStackTrace();
              cameraEventMethod = null;
            }
          }
        }

      } catch (QTException e) {
        errorMessage("run", e);
      }

      try {
        Thread.sleep(1000 / framerate);
      } catch (InterruptedException e) { }
    }
  }


  /**
   * Set the framerate for how quickly new frames are read
   * from the camera.
   */
  public void framerate(int iframerate) {
    if (iframerate <= 0) {
      System.err.println("Camera: ignoring bad framerate of " +
                         iframerate + " fps.");
      return;
    }
    framerate = iframerate;
  }


  /**
   * Called by applets to stop capturing video.
   */
  public void stop() {
    if (capture != null) {
      try {
        capture.stop(); // stop the "preview"
      } catch (StdQTException e) {
        e.printStackTrace();
      }
      capture = null;
    }
    runner = null; // unwind the thread
  }


  /**
   * Called by PApplet to shut down video so that QuickTime
   * can be used later by another applet.
   */
  public void dispose() {
    stop();
    //System.out.println("calling dispose");
    // this is important so that the next app can do video
    QTSession.close();
  }


  /**
   * General error reporting, all corraled here just in case
   * I think of something slightly more intelligent to do.
   */
  protected void errorMessage(String where, Exception e) {
    parent.die("Error inside Camera." + where + "()", e);
  }


  /**
   * Set the format to ask for from the video digitizer:
   * TUNER, COMPOSITE, SVIDEO, or COMPONENT.
   * <P>
   * The constants are just aliases to the constants returned from
   * QuickTime's getInputFormat() function, so any valid constant from
   * that will work just fine.
   */
  public void source(int which) {
    try {
      VideoDigitizer digitizer = channel.getDigitizerComponent();
      int count = digitizer.getNumberOfInputs();
      for (int i = 0; i < count; i++) {
        //System.out.println("format " + digitizer.getInputFormat(i));
        if (digitizer.getInputFormat(i) == which) {
          digitizer.setInput(i);
          return;
        }
      }
      throw new RuntimeException("The specified source() is not available.");

    } catch (StdQTException e) {
      e.printStackTrace();
      throw new RuntimeException("Could not set the video input source.");
    }
  }


  /**
   * Set the video format standard to use on the
   * video digitizer: NTSC, PAL, or SECAM.
   * <P>
   * The constants are just aliases to the constants used for
   * QuickTime's setInputStandard() function, so any valid
   * constant from that will work just fine.
   */
  public void format(int which) {
    try {
      VideoDigitizer digitizer = channel.getDigitizerComponent();
      digitizer.setInputStandard(which);
    } catch (StdQTException e) {
      e.printStackTrace();
      //throw new RuntimeException("Could not set the video input format");
    }
  }


  /**
   * Show the settings dialog for this input device.
   */
  public void settings() {
    try {
      channel.settingsDialog();
    } catch (StdQTException qte) {
      int errorCode = qte.errorCode();
      if (errorCode != Errors.userCanceledErr) {
        qte.printStackTrace();
        throw new RuntimeException("error inside Camera.settings()");
      }
    }
  }


  /*
  public String[] listInputs() {
    VideoDigitizer digitizer = channel.getDigitizerComponent();
    int count = digitizer.getNumberOfInputs();

    String outgoing[] = new String[count];
    //digitizer.setInput(2); // or something
    //DigitizerInfo di = digitizer.getDigitizerInfo()
  }
  */


  /**
   * Get a list of all available cameras as a String array.
   * i.e. printarr(Camera.list()) will show you the goodies.
   */
  static public String[] list() {
    try {
      SequenceGrabber grabber = new SequenceGrabber();
      SGVideoChannel channel = new SGVideoChannel(grabber);

      //VideoDigitizer digitizer = channel.getDigitizerComponent();
      //digitizer.setInput(2); // or something
      //DigitizerInfo di = digitizer.getDigitizerInfo()

      SGDeviceList deviceList = channel.getDeviceList(0);  // flags is 0
      String listing[] = new String[deviceList.getCount()];
      for (int i = 0; i < deviceList.getCount(); i++) {
        listing[i] = deviceList.getDeviceName(i).getName();
      }
      // properly shut down the channel so the app can use it again
      grabber.disposeChannel(channel);
      return listing;

    } catch (QTException e) {
      e.printStackTrace();
    }
    return null;
  }
}
