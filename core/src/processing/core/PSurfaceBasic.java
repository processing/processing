/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2015 The Processing Foundation

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.1.

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

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;


public class PSurfaceBasic implements PSurface {
  PApplet sketch;

  Thread thread;
  boolean paused;
  Object pauseObject = new Object();

  protected float frameRateTarget = 60;
  protected long frameRatePeriod = 1000000000L / 60L;


  public PSurfaceBasic() { }


  public void initOffscreen(PApplet sketch) {
    // TODO Auto-generated method stub
  }


  public Canvas initCanvas(PApplet sketch) {
    return null;
  }


  public Frame initFrame(PApplet sketch, Color backgroundColor,
                         int deviceIndex, boolean fullScreen,
                         boolean spanDisplays) {
    return null;
  }


  public void placeWindow(int[] location) { }

  public void placeWindow(int[] location, int[] editorLocation) { }

  public void placePresent(Color stopColor) { }

  public void setupExternalMessages() { }


  //


  public void setSize(int width, int height) {
    // TODO Auto-generated method stub

  }

//  public void initImage(PGraphics graphics) {
//    // TODO Auto-generated method stub
//
//  }

  public Component getComponent() {
    return null;
  }

  public void setSmooth(int level) {
    // TODO Auto-generated method stub

  }

  public void requestFocus() {
    // TODO Auto-generated method stub

  }

  public void blit() {
    // TODO Auto-generated method stub
  }

  public void setCursor(int kind) { }

  public void setCursor(PImage image, int hotspotX, int hotspotY) { }

  public void showCursor() { }

  public void hideCursor() { }


  //


  public void startThread() {
    if (thread == null) {
      thread = new AnimationThread();
      thread.start();
    } else {
      throw new IllegalStateException("Thread already started in PSurfaceAWT");
    }
  }


  public boolean stopThread() {
    if (thread == null) {
      return false;
    }
    thread = null;
    return true;
  }


  public boolean isStopped() {
    return thread == null;
  }


  // sets a flag to pause the thread when ready
  public void pauseThread() {
    PApplet.debug("PApplet.run() paused, calling object wait...");
    paused = true;
  }


  // halts the animation thread if the pause flag is set
  protected void checkPause() {
    if (paused) {
      synchronized (pauseObject) {
        try {
          pauseObject.wait();
//          PApplet.debug("out of wait");
        } catch (InterruptedException e) {
          // waiting for this interrupt on a start() (resume) call
        }
      }
    }
//    PApplet.debug("done with pause");
  }


  public void resumeThread() {
    paused = false;
    synchronized (pauseObject) {
      pauseObject.notifyAll();  // wake up the animation thread
    }
  }


  public void setFrameRate(float fps) {
    frameRateTarget = fps;
    frameRatePeriod = (long) (1000000000.0 / frameRateTarget);
    //g.setFrameRate(fps);
  }

  class AnimationThread extends Thread {

    public AnimationThread() {
      super("Animation Thread");
    }

    /**
     * Main method for the primary animation thread.
     * <A HREF="http://java.sun.com/products/jfc/tsc/articles/painting/">Painting in AWT and Swing</A>
     */
    @Override
    public void run() {  // not good to make this synchronized, locks things up
      long beforeTime = System.nanoTime();
      long overSleepTime = 0L;

      int noDelays = 0;
      // Number of frames with a delay of 0 ms before the
      // animation thread yields to other running threads.
      final int NO_DELAYS_PER_YIELD = 15;

      /*
      // If size un-initialized, might be a Canvas. Call setSize() here since
      // we now have a parent object that this Canvas can use as a peer.
      if (graphics.image == null) {
//        System.out.format("it's null, sketchW/H already set to %d %d%n", sketchWidth, sketchHeight);
        try {
          EventQueue.invokeAndWait(new Runnable() {
            public void run() {
              setSize(sketchWidth, sketchHeight);
            }
          });
        } catch (InterruptedException ie) {
          ie.printStackTrace();
        } catch (InvocationTargetException ite) {
          ite.printStackTrace();
        }
//        System.out.format("  but now, sketchW/H changed to %d %d%n", sketchWidth, sketchHeight);
      }
      */

      // un-pause the sketch and get rolling
      sketch.start();

      while ((Thread.currentThread() == thread) && !sketch.finished) {
        checkPause();

        // Don't resize the renderer from the EDT (i.e. from a ComponentEvent),
        // otherwise it may attempt a resize mid-render.
//        Dimension currentSize = canvas.getSize();
//        if (currentSize.width != sketchWidth || currentSize.height != sketchHeight) {
//          System.err.format("need to resize from %s to %d, %d%n", currentSize, sketchWidth, sketchHeight);
//        }

        // render a single frame
//        try {
//          EventQueue.invokeAndWait(new Runnable() {
//            public void run() {
        sketch.handleDraw();

//        EventQueue.invokeLater(new Runnable() {
//          public void run() {
        if (sketch.frameCount == 1) {
          requestFocus();
        }
//          }
//        });

//            }
//          });
//        } catch (InterruptedException ie) {
//          ie.printStackTrace();
//        } catch (InvocationTargetException ite) {
//          ite.getTargetException().printStackTrace();
//        }

        // wait for update & paint to happen before drawing next frame
        // this is necessary since the drawing is sometimes in a
        // separate thread, meaning that the next frame will start
        // before the update/paint is completed

        long afterTime = System.nanoTime();
        long timeDiff = afterTime - beforeTime;
        //System.out.println("time diff is " + timeDiff);
        long sleepTime = (frameRatePeriod - timeDiff) - overSleepTime;

        if (sleepTime > 0) {  // some time left in this cycle
          try {
            Thread.sleep(sleepTime / 1000000L, (int) (sleepTime % 1000000L));
            noDelays = 0;  // Got some sleep, not delaying anymore
          } catch (InterruptedException ex) { }

          overSleepTime = (System.nanoTime() - afterTime) - sleepTime;

        } else {    // sleepTime <= 0; the frame took longer than the period
          overSleepTime = 0L;
          noDelays++;

          if (noDelays > NO_DELAYS_PER_YIELD) {
            Thread.yield();   // give another thread a chance to run
            noDelays = 0;
          }
        }

        beforeTime = System.nanoTime();
      }

      sketch.dispose();  // call to shutdown libs?

      // If the user called the exit() function, the window should close,
      // rather than the sketch just halting.
      if (sketch.exitCalled) {
        sketch.exitActual();
      }
    }
  }
}