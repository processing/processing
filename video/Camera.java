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


public class Camera extends PImage
implements StdQTConstants, StdQTConstants4, Runnable {
  PApplet parent;
  Method cameraEventMethod;
  String name; // keep track for error messages
  Thread runner;

  //PImage borderImage;
  //boolean removeBorders = true;
  boolean available = false;

  /** Temporary storage for the raw image data read directly from the camera */
  public int data[];

  public int dataWidth;
  public int dataHeight;
  public int dataRowBytes;

  public boolean crop;
  public int cropX;
  public int cropY;
  public int cropW;
  public int cropH;

  int fps;
  RawEncodedImage raw;
  SequenceGrabber capture;

  static {
    //System.out.println("Camera init");
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
    this(parent, "", requestWidth, requestHeight, 30);
  }

  public Camera(PApplet parent, int reqWidth, int reqHeight, int fps) {
    this(parent, "", reqWidth, reqHeight, fps);
  }

  public Camera(PApplet parent, String name, int reqWidth, int reqHeight) {
    this(parent, name, reqWidth, reqHeight, 30);
  }


  /*
   quicktime.QTSession.open();
  quicktime.std.sg.SequenceGrabber sg = new quicktime.std.sg.SequenceGrabber();
  quicktime.std.sg.SGVideoChannel sc  = new quicktime.std.sg.SGVideoChannel(sg);
  quicktime.std.sg.VideoDigitizer vd  = sc.getDigitizerComponent();
  println( "dv.getNumberOfInputs :" ); println( vd.getNumberOfInputs() ); println();
//change line below to set input source
  vd.setInput(1);
//
  println( "dv.getInput :" ); println( vd.getInput() ); println();
  } catch (Exception e) {
  e.printStackTrace();
  }
  */

  /**
   * If 'name' is the empty string, don't set a specific device,
   * which means that QuickTime will use that last device used by
   * a QuickTime application. If name is set to null, a prompt
   * will show up, allowing the user to choose the good stuff.
   * <P>
   * If the following function:
   * public void cameraEvent(Camera c)
   * is defined int the host PApplet, then it will be called every
   * time a new frame is available from the camera.
   */
  public Camera(PApplet parent, String name,
                int requestWidth, int requestHeight, int fps) {
    this.parent = parent;
    this.name = name;
    this.fps = fps;

    try {
      /*
      QTSession.open();
      QTRuntimeException.registerHandler(new QTRuntimeHandler() {
          public void exceptionOccurred(QTRuntimeException e,
                                        Object obj, String s, boolean flag) {
            System.err.println("Problem inside Camera");
            e.printStackTrace();
          }
        });
      */
      //System.out.println("0");

      QDRect qdrect = new QDRect(requestWidth, requestHeight);
      QDGraphics qdgraphics = new QDGraphics(qdrect);

      //System.out.println("0a");

      capture = new SequenceGrabber();
      //System.out.println("0b");
      capture.setGWorld(qdgraphics, null);

      //System.out.println("0c");

      // CRASHING HERE ON OSX
      SGVideoChannel channel = new SGVideoChannel(capture);
      //System.out.println("0c1");
      channel.setBounds(qdrect);
      //System.out.println("0c2");
      channel.setUsage(2);  // what is this usage number?
      //System.out.println("0c3");
      capture.startPreview();  // maybe this comes later?

      //System.out.println("0d");

      PixMap pixmap = qdgraphics.getPixMap();
      raw = pixmap.getPixelData();

      //System.out.println("0e");

      if (name == null) {
        channel.settingsDialog();

      } else if (name.length() > 0) {
        channel.setDevice(name);
      }
      //System.out.println("0f");
      //channel.setDevice("Logitech QuickCam Express-WDM");
      /*
      if (showDialog) channel.settingsDialog();
      SGDeviceList list = channel.getDeviceList(0);
      System.out.println("count is " + list.getCount());
      for (int i = 0; i < list.getCount(); i++) {
        System.out.println(list.getDeviceName(i).getName());
      }
      //System.out.println(channel.getSettings());
      */

      //int grabWidthBytes = raw.getRowBytes();
      dataRowBytes = raw.getRowBytes();
      dataWidth = dataRowBytes / 4;

      //System.out.println("row bytes " + raw.getRowBytes() + " " +
      //                 (raw.getRowBytes() / 4));
      //int extraBytes = dataRowBytes - requestWidth*4;
      //int extraPixels = extraBytes / 4;
      //int videoWidth = requestWidth + extraPixels;
      dataHeight = raw.getSize() / dataRowBytes;

      //System.out.println("height req, actual: " + requestHeight +
      //                 " " + dataHeight);

      if (dataWidth != requestWidth) {
        crop = true;
        cropX = 0;
        cropY = 0;
        cropW = requestWidth;
        cropH = requestHeight;

        /*
        System.out.println("dataWidth is " + dataWidth +
                           " not " + requestWidth);
        if (removeBorders) {
          int bpixels[] = new int[dataWidth * requestHeight];
          borderImage = new PImage(bpixels, dataWidth, requestHeight, RGB);
        } else {
          requestWidth = dataWidth;
        }
        */
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
        //e.printStackTrace();
      }

    } catch (StdQTException qte) {
      //qte.printStackTrace();

      int errorCode = qte.errorCode();
      // where's the friggin constant for this?
      if (errorCode == -9405) {
        // this can happen when the camera isn't available or
        // wasn't shut down properly
        parent.die("The camera (or VDIG) is not " +
                   "installed correctly (see readme.txt).", qte);
      } else {
        parent.die("Error while setting up Camera", qte);
      }
    } catch (Exception e) {
      parent.die("Error while setting up Camera", e);
    }
  }


  /**
   * True if a frame is ready to be read. Example:
   * if (camera.available()) camera.read();
   * <P>
   * Alternatively, you can use cameraEvent(Camera c) to notify you
   * whenever available() is set to true.
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

        /*
      if (borderImage != null) {  // need to remove borders
        raw.copyToArray(0, borderImage.pixels,
                        0, borderImage.width * borderImage.height);
        int borderIndex = 0;
        int targetIndex = 0;
        for (int i = 0; i < height; i++) {
          System.arraycopy(borderImage.pixels, borderIndex,
                           pixels, targetIndex, width);
            borderIndex += borderImage.width;
            targetIndex += width;
        }
        */
      } else {  // just copy directly
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
        Thread.sleep(1000 / fps);
      } catch (InterruptedException e) { }
    }
  }


  /**
   * Set the framerate for how quickly new frames are read
   * from the camera.
   */
  public void framerate(int ifps) {
    if (ifps <= 0) {
      System.err.println("Camera: ignoring bad framerate of " +
                         ifps + " fps.");
      return;
    }
    fps = ifps;
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
   * Shows the settings dialog for this channel.
   */
  //public void prompt() {
  //channel.settingsDialog();
  //}


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
