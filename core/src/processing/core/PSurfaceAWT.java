/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2014 The Processing Foundation

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

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JFrame;

import processing.event.KeyEvent;
import processing.event.MouseEvent;


public class PSurfaceAWT implements PSurface {
  GraphicsDevice displayDevice;

  // used for canvas to determine whether resizable or not
  boolean resizable;  // default is false

  // Internally, we know it's always a JFrame (not just a Frame)
  JFrame frame;

  // Note that x and y may not be zero, depending on the display configuration
  Rectangle screenRect;

  // disabled on retina inside init()
  boolean useActive = true;
//  boolean useActive = false;
//  boolean useStrategy = true;
  boolean useStrategy = false;
  Canvas canvas;

  PApplet sketch;
  PGraphics graphics;

  int sketchWidth;
  int sketchHeight;

  Thread thread;
  boolean paused;
  Object pauseObject = new Object();

  /** As of release 0116, frameRate(60) is called as a default */
  protected float frameRateTarget = 60;
  protected long frameRatePeriod = 1000000000L / 60L;


  public PSurfaceAWT(PGraphics graphics) {
    this.graphics = graphics;

    if (checkRetina()) {
      // The active-mode rendering seems to be 2x slower, so disable it
      // with retina. On a non-retina machine, however, useActive seems
      // the only (or best) way to handle the rendering.
      useActive = false;
    }

    createCanvas();
    addListeners();
  }


  void createCanvas() {
    canvas = new SmoothCanvas();
    canvas.setIgnoreRepaint(true);  // ??

    // send tab keys through to the PApplet
    canvas.setFocusTraversalKeysEnabled(false);

    canvas.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        if (!sketch.looping) {
          sketch.redraw();
        }
      }
    });
  }


  /**
   * Handle grabbing the focus from the parent applet. Other renderers can
   * override this if handling needs to be different.
   */
  public void requestFocus() {
    // for 2.0a6, moving this request to the EDT
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        // Call the request focus event once the image is sure to be on
        // screen and the component is valid. The OpenGL renderer will
        // request focus for its canvas inside beginDraw().
        // http://java.sun.com/j2se/1.4.2/docs/api/java/awt/doc-files/FocusSpec.html
        // Disabling for 0185, because it causes an assertion failure on OS X
        // http://code.google.com/p/processing/issues/detail?id=258
        //        requestFocus();

        // Changing to this version for 0187
        // http://code.google.com/p/processing/issues/detail?id=279
        //requestFocusInWindow();

        // For 3.0, just call this directly on the Canvas object
        if (canvas != null) {
          canvas.requestFocusInWindow();
        }
      }
    });
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  class SmoothCanvas extends Canvas {
    private Dimension oldSize = new Dimension(0, 0);
    private Dimension newSize = new Dimension(0, 0);


    @Override
    public Dimension getPreferredSize() {
      return new Dimension(sketchWidth, sketchHeight);
    }


    @Override
    public Dimension getMinimumSize() {
      return getPreferredSize();
    }


    @Override
    public Dimension getMaximumSize() {
      return resizable ? super.getMaximumSize() : getPreferredSize();
    }


    @Override
    public void validate() {
      super.validate();
      newSize.width = getWidth();
      newSize.height = getHeight();
      if (oldSize.equals(newSize)) {
//        System.out.println("validate() return " + oldSize);
        return;
      } else {
//        System.out.println("validate() render old=" + oldSize + " -> new=" + newSize);
        oldSize = newSize;
        render();
      }
    }


    @Override
    public void update(Graphics g) {
//      System.out.println("updating");
      paint(g);
    }


    @Override
    public void paint(Graphics g) {
//      System.out.println("painting");
//    validate();
      render();
    }


    @Override
    public void addNotify() {
//      System.out.println("adding notify");
      super.addNotify();
      // prior to Java 7 on OS X, this no longer works [121222]
//    createBufferStrategy(2);
    }


    /*
    protected synchronized void render_example() {
      System.out.println("render(), bounds are " + getBounds());
      if (getBufferStrategy() == null) {  // whole block [121222]
        System.out.println("creating a strategy");
        createBufferStrategy(2);
      }
      BufferStrategy strategy = getBufferStrategy();
      if (strategy == null) {
        return;
      }
      // Render single frame
      do {
        // The following loop ensures that the contents of the drawing buffer
        // are consistent in case the underlying surface was recreated
        do {
          Graphics draw = strategy.getDrawGraphics();
          //Insets i = getInsets();
          //int w = (int)(((double)(getWidth() - i.left - i.right))/2+0.5);
          //int h = (int)(((double)(getHeight() - i.top - i.bottom))/2+0.5);
          Insets i = new Insets(0, 0, 0, 0);
          int w = getWidth() / 2;
          int h = getHeight() / 2;

          if (false) {
            draw.setColor(Color.YELLOW);
            draw.fillRect(i.left, i.top + h, w, getHeight() - h);
            draw.fillRect(i.left + w, i.top, getWidth() - w, h);
            draw.setColor(Color.BLACK);
            draw.fillRect(i.left, i.top, w, h);
            draw.fillRect(i.left + w, i.top + h, getWidth() - w, getHeight() - h);
          } else {
            draw.setColor(Color.WHITE);
            draw.fillRect(i.left, i.top, getWidth(), getHeight());

            draw.setColor(Color.BLACK);
            draw.setFont(new Font("SansSerif", Font.PLAIN, 50));
            draw.drawString("this is some text", 50, 100);

//          draw.setColor(Color.BLACK);
//          int mx = Math.max(getWidth(), getHeight());
//          for (int x = -getWidth(); x < getWidth(); x += 5) {
//            draw.drawLine(x, 0, x + mx, mx);
//          }
          }

          draw.dispose();

          // Repeat the rendering if the drawing buffer contents
          // were restored
          System.out.println("restored " + strategy.contentsRestored());
        } while (strategy.contentsRestored());

        // Display the buffer
        System.out.println("showing");
        strategy.show();

        // Repeat the rendering if the drawing buffer was lost
        System.out.println("lost " + strategy.contentsLost());
        System.out.println();
      } while (strategy.contentsLost());
    }
    */


    protected synchronized void render() {
//      if (canvas == null) {
//        removeListeners(this);
//        canvas = new Canvas();
//        add(canvas);
//        setIgnoreRepaint(true);
//        canvas.setIgnoreRepaint(true);
//        addListeners(canvas);
////        add(canvas, BorderLayout.CENTER);
////        doLayout();
//      }

      // not sure why this was here, can't be good
      //canvas.setBounds(0, 0, sketch.width, sketch.height);

      if (!canvas.isDisplayable()) {
//        System.out.println("no peer.. holding");
        return;
      }

//      System.out.println("render(), canvas bounds are " + canvas.getBounds());
      if (canvas.getBufferStrategy() == null) {  // whole block [121222]
//        System.out.println("creating a strategy");
        canvas.createBufferStrategy(2);
      }
      BufferStrategy strategy = canvas.getBufferStrategy();
//      System.out.println(strategy);
      if (strategy == null) {
        return;
      }
      // Render single frame
      do {
        // The following loop ensures that the contents of the drawing buffer
        // are consistent in case the underlying surface was recreated
        do {
          Graphics draw = strategy.getDrawGraphics();
          //draw.drawImage(pg.image, 0, 0, sketch.width, sketch.height, null);
          // draw to width/height, since this may be a 2x image
          draw.drawImage(graphics.image, 0, 0, sketchWidth, sketchHeight, null);
          draw.dispose();

          // Repeat the rendering if the drawing buffer contents
          // were restored
//          System.out.println("restored " + strategy.contentsRestored());
        } while (strategy.contentsRestored());

        // Display the buffer
//        System.out.println("showing");
        strategy.show();

        // Repeat the rendering if the drawing buffer was lost
//        System.out.println("lost " + strategy.contentsLost());
//        System.out.println();
      } while (strategy.contentsLost());
    }
  }


  /*
  @Override
  public void update(Graphics screen) {
    paint(screen);
  }


  @Override
  public void paint(Graphics screen) {
//    int r = (int) random(10000);
//    System.out.println("into paint " + r);
    //super.paint(screen);

    // ignore the very first call to paint, since it's coming
    // from the o.s., and the applet will soon update itself anyway.
    if (frameCount == 0) {
//      println("Skipping frame");
      // paint() may be called more than once before things
      // are finally painted to the screen and the thread gets going
      return;
    }
    // without ignoring the first call, the first several frames
    // are confused because paint() gets called in the midst of
    // the initial nextFrame() call, so there are multiple
    // updates fighting with one another.

    // make sure the screen is visible and usable
    // (also prevents over-drawing when using PGraphicsOpenGL)

//    if (useActive) {
//      return;
//    }

//    if (insideDraw) {
//      new Exception().printStackTrace(System.out);
//    }
    if (!insideDraw && (g != null) && (g.image != null)) {
      if (useStrategy) {
        render();
      } else {
//        System.out.println("drawing to screen");
        //screen.drawImage(g.image, 0, 0, null);  // not retina friendly
        screen.drawImage(g.image, 0, 0, width, height, null);
      }
    } else {
      debug(insideDraw + " " + g + " " + ((g != null) ? g.image : "-"));
    }
  }
*/


  public void blit() {
//    System.out.println("blit");
    ((SmoothCanvas) canvas).render();  // ??
//    canvas.repaint();
    /*
    if (canvas.getGraphicsConfiguration() != null) {
      GraphicsDevice device = canvas.getGraphicsConfiguration().getDevice();
      if (device != displayDevice) {
        System.out.println("display device changed from " + displayDevice + " to " + device);
      }
    }

    if (useActive) {
      if (useStrategy) {
        render();
      } else {
        Graphics screen = canvas.getGraphics();
        if (screen != null) {
          screen.drawImage(g.image, 0, 0, sketch.width, sketch.height, null);
        }
      }
    } else {
      canvas.repaint();
    }
//  getToolkit().sync();  // force repaint now (proper method)
    */
  }


  // what needs to happen here?
  public void initOffscreen() {

  }

  /*
  public Frame initOffscreen() {
    Frame dummy = new Frame();
    dummy.pack();  // get legit AWT graphics
    // but don't show it
    return dummy;
  }
  */

  public Canvas initCanvas(PApplet sketch) {
    this.sketch = sketch;

    // needed for getPreferredSize() et al
    sketchWidth = sketch.sketchWidth();
    sketchHeight = sketch.sketchHeight();

    return canvas;
  }


  public Frame initFrame(PApplet sketch, Color backgroundColor,
                         int deviceIndex, boolean fullScreen, boolean spanDisplays) {
    this.sketch = sketch;

    GraphicsEnvironment environment =
      GraphicsEnvironment.getLocalGraphicsEnvironment();

    if (deviceIndex >= 0) {  // if -1, use the default device
      GraphicsDevice[] devices = environment.getScreenDevices();
      if (deviceIndex < devices.length) {
        displayDevice = devices[deviceIndex];
      } else {
        System.err.format("Display %d does not exist, " +
          "using the default display instead.", deviceIndex);
        for (int i = 0; i < devices.length; i++) {
          System.err.format("Display %d is %s\n", i, devices[i]);
        }
      }
    }
    if (displayDevice == null) {
      displayDevice = environment.getDefaultScreenDevice();
    }

    // Need to save the window bounds at full screen,
    // because pack() will cause the bounds to go to zero.
    // http://dev.processing.org/bugs/show_bug.cgi?id=923
    screenRect = spanDisplays ? getDisplaySpan() :
      displayDevice.getDefaultConfiguration().getBounds();
    // DisplayMode doesn't work here, because we can't get the upper-left
    // corner of the display, which is important for multi-display setups.

    // Set the displayWidth/Height variables inside PApplet, so that they're
    // usable and can even be returned by the sketchWidth()/Height() methods.
    sketch.displayWidth = screenRect.width;
    sketch.displayHeight = screenRect.height;

    sketchWidth = sketch.sketchWidth();
    sketchHeight = sketch.sketchHeight();

    // Sketch has already requested to be the same as the screen's
    // width and height, so let's roll with full screen mode.
    if (screenRect.width == sketchWidth &&
        screenRect.height == sketchHeight) {
      fullScreen = true;
    }

    if (fullScreen || spanDisplays) {
      sketchWidth = screenRect.width;
      sketchHeight = screenRect.height;
    }

    // Using a JFrame fixes a Windows problem with Present mode. This might
    // be our error, but usually this is the sort of crap we usually get from
    // OS X. It's time for a turnaround: Redmond is thinking different too!
    // https://github.com/processing/processing/issues/1955
    frame = new JFrame(displayDevice.getDefaultConfiguration());
//    // Default Processing gray, which will be replaced below if another
//    // color is specified on the command line (i.e. in the prefs).
//    ((JFrame) frame).getContentPane().setBackground(WINDOW_BGCOLOR);
//    // Cannot call setResizable(false) until later due to OS X (issue #467)

    // Removed code above, also removed from what's now in the placeXxxx()
    // methods. Not sure why it was being double-set; hopefully anachronistic.
    if (backgroundColor == null) {
      backgroundColor = WINDOW_BGCOLOR;
    }
    frame.getContentPane().setBackground(backgroundColor);

    // Set the trimmings around the image
    setIconImage(frame);

    // For 0149, moving this code (up to the pack() method) before init().
    // For OpenGL (and perhaps other renderers in the future), a peer is
    // needed before a GLDrawable can be created. So pack() needs to be
    // called on the Frame before applet.init(), which itself calls size(),
    // and launches the Thread that will kick off setup().
    // http://dev.processing.org/bugs/show_bug.cgi?id=891
    // http://dev.processing.org/bugs/show_bug.cgi?id=908

    frame.add(canvas);
    setSize(sketchWidth, sketchHeight);

    if (fullScreen) {
      // Called here because the graphics device is needed before we can
      // determine whether the sketch wants size(displayWidth, displayHeight),
      // and getting the graphics device will be PSurface-specific.
      PApplet.hideMenuBar();

      // Tried to use this to fix the 'present' mode issue.
      // Did not help, and the screenRect setup seems to work fine.
      //frame.setExtendedState(Frame.MAXIMIZED_BOTH);

      // https://github.com/processing/processing/pull/3162
      frame.dispose();  // release native resources, allows setUndecorated()
      frame.setUndecorated(true);
      // another duplicate?
//      if (backgroundColor != null) {
//        frame.getContentPane().setBackground(backgroundColor);
//      }
      // this may be the bounds of all screens
      frame.setBounds(screenRect);
      frame.setVisible(true);  // re-add native resources
    }
    frame.setLayout(null);
    //frame.add(applet);

    // Need to pass back our new sketchWidth/Height here, because it may have
    // been overridden by numbers we calculated above if fullScreen and/or
    // spanScreens was in use.
//    pg = sketch.makePrimaryGraphics(sketchWidth, sketchHeight);
//    pg = sketch.makePrimaryGraphics();

    // resize sketch to sketchWidth/sketchHeight here

    if (fullScreen) {
      frame.invalidate();
    } else {
//      frame.pack();
    }

    // insufficient, places the 100x100 sketches offset strangely
    //frame.validate();

    // disabling resize has to happen after pack() to avoid apparent Apple bug
    // http://code.google.com/p/processing/issues/detail?id=467
    frame.setResizable(false);

    return frame;
  }


  /** Set the window (and dock, or whatever necessary) title. */
  public void setTitle(String title) {
    frame.setTitle(title);
  }


  /** Set true if we want to resize things (default is not resizable) */
  public void setResizable(boolean resizable) {
    this.resizable = resizable;  // really only used for canvas

    if (frame != null) {
      frame.setResizable(resizable);
    }
  }


  public void setVisible(boolean visible) {
    frame.setVisible(visible);

    // removing per https://github.com/processing/processing/pull/3162
    // can remove the code below once 3.0a6 is tested and behaving
/*
    if (visible && PApplet.platform == PConstants.LINUX) {
      // Linux doesn't deal with insets the same way. We get fake insets
      // earlier, and then the window manager will slap its own insets
      // onto things once the frame is realized on the screen. Awzm.
      if (PApplet.platform == PConstants.LINUX) {
        Insets insets = frame.getInsets();
        frame.setSize(Math.max(sketchWidth, MIN_WINDOW_WIDTH) +
                      insets.left + insets.right,
                      Math.max(sketchHeight, MIN_WINDOW_HEIGHT) +
                      insets.top + insets.bottom);
      }
    }
*/
  }


  //public void placeFullScreen(boolean hideStop) {
  public void placePresent(Color stopColor) {
    // After the pack(), the screen bounds are gonna be 0s
    frame.setBounds(screenRect);
    canvas.setBounds((screenRect.width - sketchWidth) / 2,
                     (screenRect.height - sketchHeight) / 2,
                     sketchWidth, sketchHeight);

//    if (PApplet.platform == PConstants.MACOSX) {
//      macosxFullScreenEnable(frame);
//      macosxFullScreenToggle(frame);
//    }

    if (stopColor != null) {
      Label label = new Label("stop");
      label.setForeground(stopColor);
      label.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {
          sketch.exit();
        }
      });
      frame.add(label);

      Dimension labelSize = label.getPreferredSize();
      // sometimes shows up truncated on mac
      //System.out.println("label width is " + labelSize.width);
      labelSize = new Dimension(100, labelSize.height);
      label.setSize(labelSize);
      label.setLocation(20, screenRect.height - labelSize.height - 20);
    }
  }


  public void placeWindow(int[] location) {
    setFrameSize(); //sketchWidth, sketchHeight);

    if (location != null) {
      // a specific location was received from the Runner
      // (applet has been run more than once, user placed window)
      frame.setLocation(location[0], location[1]);

    } else {  // just center on screen
      // Can't use frame.setLocationRelativeTo(null) because it sends the
      // frame to the main display, which undermines the --display setting.
      frame.setLocation(screenRect.x + (screenRect.width - sketchWidth) / 2,
                        screenRect.y + (screenRect.height - sketchHeight) / 2);
    }
    Point frameLoc = frame.getLocation();
    if (frameLoc.y < 0) {
      // Windows actually allows you to place frames where they can't be
      // closed. Awesome. http://dev.processing.org/bugs/show_bug.cgi?id=1508
      frame.setLocation(frameLoc.x, 30);
    }

//    if (backgroundColor != null) {
//      ((JFrame) frame).getContentPane().setBackground(backgroundColor);
//    }

    setCanvasSize(); //sketchWidth, sketchHeight);

    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    // handle frame resizing events
    setupFrameResizeListener();

    // all set for rockin
    if (sketch.getGraphics().displayable()) {
      setVisible(true);
    }
  }


  private void setCanvasSize() {
    int contentW = Math.max(sketchWidth, MIN_WINDOW_WIDTH);
    int contentH = Math.max(sketchHeight, MIN_WINDOW_HEIGHT);

    canvas.setBounds((contentW - sketchWidth)/2,
                     (contentH - sketchHeight)/2,
                     sketchWidth, sketchHeight);
  }


  /** Resize frame for these sketch (canvas) dimensions. */
  private Dimension setFrameSize() {  //int sketchWidth, int sketchHeight) {
    // https://github.com/processing/processing/pull/3162
    frame.addNotify();  // using instead of show() to add the peer [fry]

//    System.out.format("setting frame size %d %d %n", sketchWidth, sketchHeight);
//    new Exception().printStackTrace(System.out);
    Insets insets = frame.getInsets();
    int windowW = Math.max(sketchWidth, MIN_WINDOW_WIDTH) +
      insets.left + insets.right;
    int windowH = Math.max(sketchHeight, MIN_WINDOW_HEIGHT) +
      insets.top + insets.bottom;
    frame.setSize(windowW, windowH);
    return new Dimension(windowW, windowH);
  }


  private void setFrameCentered() {
    // Can't use frame.setLocationRelativeTo(null) because it sends the
    // frame to the main display, which undermines the --display setting.
    frame.setLocation(screenRect.x + (screenRect.width - sketchWidth) / 2,
                      screenRect.y + (screenRect.height - sketchHeight) / 2);
  }


  public void placeWindow(int[] location, int[] editorLocation) {
    //Dimension window = setFrameSize(sketchWidth, sketchHeight);
    Dimension window = setFrameSize(); //sketchWidth, sketchHeight);

    int contentW = Math.max(sketchWidth, MIN_WINDOW_WIDTH);
    int contentH = Math.max(sketchHeight, MIN_WINDOW_HEIGHT);

    if (location != null) {
      // a specific location was received from the Runner
      // (applet has been run more than once, user placed window)
      frame.setLocation(location[0], location[1]);

    } else if (editorLocation != null) {
      int locationX = editorLocation[0] - 20;
      int locationY = editorLocation[1];

      if (locationX - window.width > 10) {
        // if it fits to the left of the window
        frame.setLocation(locationX - window.width, locationY);

      } else {  // doesn't fit
        // if it fits inside the editor window,
        // offset slightly from upper lefthand corner
        // so that it's plunked inside the text area
        locationX = editorLocation[0] + 66;
        locationY = editorLocation[1] + 66;

        if ((locationX + window.width > sketch.displayWidth - 33) ||
            (locationY + window.height > sketch.displayHeight - 33)) {
          // otherwise center on screen
          locationX = (sketch.displayWidth - window.width) / 2;
          locationY = (sketch.displayHeight - window.height) / 2;
        }
        frame.setLocation(locationX, locationY);
      }
    } else {  // just center on screen
      setFrameCentered();
    }
    Point frameLoc = frame.getLocation();
    if (frameLoc.y < 0) {
      // Windows actually allows you to place frames where they can't be
      // closed. Awesome. http://dev.processing.org/bugs/show_bug.cgi?id=1508
      frame.setLocation(frameLoc.x, 30);
    }

//    if (backgroundColor != null) {
////    if (backgroundColor == Color.black) {  //BLACK) {
////      // this means no bg color unless specified
////      backgroundColor = SystemColor.control;
////    }
//      ((JFrame) frame).getContentPane().setBackground(backgroundColor);
//    }

    canvas.setBounds((contentW - sketchWidth)/2,
                     (contentH - sketchHeight)/2,
                     sketchWidth, sketchHeight);

    // handle frame resizing events
    setupFrameResizeListener();

    // TODO this is much too late... why even create the enormous frame for PDF?
    if (sketch.getGraphics().displayable()) {
      setVisible(true);
    }
  }


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


  // needs to resize the frame, which will resize the canvas, and so on...
  public void setSize(int wide, int high) {
//    System.out.format("frame visible %b, setSize(%d, %d) %n", frame.isVisible(), wide, high);
//    new Exception().printStackTrace(System.out);

    sketchWidth = wide;
    sketchHeight = high;

//    canvas.setSize(wide, high);
//    frame.setSize(wide, high);
    if (frame != null) {  // canvas only
      setFrameSize(); //wide, high);
    }
    setCanvasSize();
//    if (frame != null) {
//      frame.setLocationRelativeTo(null);
//    }

    GraphicsConfiguration gc = canvas.getGraphicsConfiguration();
    // If not realized (off-screen, i.e the Color Selector Tool), gc will be null.
    if (gc == null) {
      System.err.println("GraphicsConfiguration null in setSize()");
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
    }

    // Formerly this was broken into separate versions based on offscreen or
    // not, but we may as well create a compatible image; it won't hurt, right?
    int factor = graphics.pixelFactor;
    graphics.image = gc.createCompatibleImage(wide * factor, high * factor);

    //throw new RuntimeException("implement me, see readme.md");
    sketch.width = wide;
    sketch.height = high;

    // sets internal variables for width/height/pixelWidth/pixelHeight
    graphics.setSize(wide, high);
  }


  public void setSmooth(int level) {
  }


  private boolean checkRetina() {
    if (PApplet.platform == PConstants.MACOSX) {
    // This should probably be reset each time there's a display change.
    // A 5-minute search didn't turn up any such event in the Java API.
    // Also, should we use the Toolkit associated with the editor window?
      final String javaVendor = System.getProperty("java.vendor");
      if (javaVendor.contains("Apple")) {
        Float prop = (Float)
          canvas.getToolkit().getDesktopProperty("apple.awt.contentScaleFactor");
        if (prop != null) {
          return prop == 2;
        }
      } else if (javaVendor.contains("Oracle")) {
        String version = System.getProperty("java.version");  // 1.7.0_40
        String[] m = PApplet.match(version, "1.(\\d).*_(\\d+)");

        // Make sure this is Oracle Java 7u40 or later
        if (m != null &&
          PApplet.parseInt(m[1]) >= 7 &&
          PApplet.parseInt(m[1]) >= 40) {
          GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
          GraphicsDevice device = env.getDefaultScreenDevice();

          try {
            Field field = device.getClass().getDeclaredField("scale");
            if (field != null) {
              field.setAccessible(true);
              Object scale = field.get(device);

              if (scale instanceof Integer && ((Integer)scale).intValue() == 2) {
                return true;
              }
            }
          } catch (Exception ignore) { }
        }
      }
    }
    return false;
  }


  // get the bounds for all displays
  static Rectangle getDisplaySpan() {
    Rectangle bounds = new Rectangle();
    GraphicsEnvironment environment =
      GraphicsEnvironment.getLocalGraphicsEnvironment();
    for (GraphicsDevice device : environment.getScreenDevices()) {
      for (GraphicsConfiguration config : device.getConfigurations()) {
        Rectangle2D.union(bounds, config.getBounds(), bounds);
      }
    }
    return bounds;
  }


  /*
  private void checkDisplaySize() {
    if (canvas.getGraphicsConfiguration() != null) {
      GraphicsDevice displayDevice = getGraphicsConfiguration().getDevice();

      if (displayDevice != null) {
        Rectangle screenRect =
          displayDevice.getDefaultConfiguration().getBounds();

        displayWidth = screenRect.width;
        displayHeight = screenRect.height;
      }
    }
  }
  */


  /**
   * Set this sketch to communicate its state back to the PDE.
   * <p/>
   * This uses the stderr stream to write positions of the window
   * (so that it will be saved by the PDE for the next run) and
   * notify on quit. See more notes in the Worker class.
   */
  public void setupExternalMessages() {

    frame.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentMoved(ComponentEvent e) {
          Point where = ((Frame) e.getSource()).getLocation();
          System.err.println(PApplet.EXTERNAL_MOVE + " " +
                             where.x + " " + where.y);
          System.err.flush();  // doesn't seem to help or hurt
        }
      });

    frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
//          System.err.println(PApplet.EXTERNAL_QUIT);
//          System.err.flush();  // important
//          System.exit(0);
          sketch.exit();  // don't quit, need to just shut everything down (0133)
        }
      });
  }


  /**
   * Set up a listener that will fire proper component resize events
   * in cases where frame.setResizable(true) is called.
   */
  public void setupFrameResizeListener() {
    frame.addWindowStateListener(new WindowStateListener() {
      @Override
      // Detecting when the frame is resized in order to handle the frame
      // maximization bug in OSX:
      // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=8036935
      public void windowStateChanged(WindowEvent e) {
        // This seems to be firing when dragging the window on OS X
        // https://github.com/processing/processing/issues/3092
        if (Frame.MAXIMIZED_BOTH == e.getNewState()) {
          // Supposedly, sending the frame to back and then front is a
          // workaround for this bug:
          // http://stackoverflow.com/a/23897602
          // but is not working for me...
          //frame.toBack();
          //frame.toFront();
          // Packing the frame works, but that causes the window to collapse
          // on OS X when the window is dragged. Changing to addNotify() for
          // https://github.com/processing/processing/issues/3092
          //frame.pack();
          frame.addNotify();
        }
      }
    });

    frame.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
//          for (ComponentListener cl : frame.getComponentListeners()) {
//            System.out.println(cl);
//          }
//          System.out.println(e);
//          System.out.println(frame.isResizable());
//          new Exception().printStackTrace();

          // Ignore bad resize events fired during setup to fix
          // http://dev.processing.org/bugs/show_bug.cgi?id=341
          // This should also fix the blank screen on Linux bug
          // http://dev.processing.org/bugs/show_bug.cgi?id=282
          if (frame.isResizable()) {
            // might be multiple resize calls before visible (i.e. first
            // when pack() is called, then when it's resized for use).
            // ignore them because it's not the user resizing things.
            Frame farm = (Frame) e.getComponent();
            if (farm.isVisible()) {
              Insets insets = farm.getInsets();
              Dimension windowSize = farm.getSize();

              // JFrame (unlike java.awt.Frame) doesn't include the left/top
              // insets for placement (though it does seem to need them for
              // overall size of the window. Perhaps JFrame sets its coord
              // system so that (0, 0) is always the upper-left of the content
              // area. Which seems nice, but breaks any f*ing AWT-based code.
              setSize(windowSize.width - insets.left - insets.right,
                      windowSize.height - insets.top - insets.bottom);

              /*
              Rectangle newBounds =
                new Rectangle(0, 0, //insets.left, insets.top,
                              windowSize.width - insets.left - insets.right,
                              windowSize.height - insets.top - insets.bottom);

              System.out.println(e);
              System.out.println(newBounds);

              Rectangle oldBounds = canvas.getBounds();
              if (!newBounds.equals(oldBounds)) {
                // the ComponentListener in PApplet will handle calling size()
                canvas.setBounds(newBounds);

                // In 0225, calling this via reflection so that we can still
                // compile in Java 1.6. This is a trap since we really need
                // to move to 1.7 and cannot support 1.6, but things like text
                // are still a little wonky on 1.7, especially on OS X.
                // This gives us a way to at least test against older VMs.
                canvas.revalidate();   // let the layout manager do its work
              }
              */
            }
          }
        }
      });
  }


  static ArrayList<Image> iconImages;

  static protected void setIconImage(Frame frame) {
    // On OS X, this only affects what shows up in the dock when minimized.
    // So replacing it is actually a step backwards. Brilliant.
    if (PApplet.platform != PConstants.MACOSX) {
      //Image image = Toolkit.getDefaultToolkit().createImage(ICON_IMAGE);
      //frame.setIconImage(image);
      try {
        if (iconImages == null) {
          iconImages = new ArrayList<Image>();
          final int[] sizes = { 16, 32, 48, 64 };

          for (int sz : sizes) {
            //URL url = getClass().getResource("/icon/icon-" + sz + ".png");
            URL url = PApplet.class.getResource("/icon/icon-" + sz + ".png");
            Image image = Toolkit.getDefaultToolkit().getImage(url);
            iconImages.add(image);
            //iconImages.add(Toolkit.getLibImage("icons/pde-" + sz + ".png", frame));
          }
        }
        frame.setIconImages(iconImages);

      } catch (Exception e) { }  // harmless; keep this to ourselves
    }
  }


//  /**
//   * (No longer in use) Use reflection to call
//   * <code>com.apple.eawt.FullScreenUtilities.setWindowCanFullScreen(window, true);</code>
//   */
//  static void macosxFullScreenEnable(Window window) {
//    try {
//      Class<?> util = Class.forName("com.apple.eawt.FullScreenUtilities");
//      Class params[] = new Class[] { Window.class, Boolean.TYPE };
//      Method method = util.getMethod("setWindowCanFullScreen", params);
//      method.invoke(util, window, true);
//
//    } catch (ClassNotFoundException cnfe) {
//      // ignored
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }
//
//
//  /**
//   * (No longer in use) Use reflection to call
//   * <code>com.apple.eawt.Application.getApplication().requestToggleFullScreen(window);</code>
//   */
//  static void macosxFullScreenToggle(Window window) {
//    try {
//      Class<?> appClass = Class.forName("com.apple.eawt.Application");
//
//      Method getAppMethod = appClass.getMethod("getApplication");
//      Object app = getAppMethod.invoke(null, new Object[0]);
//
//      Method requestMethod =
//        appClass.getMethod("requestToggleFullScreen", Window.class);
//      requestMethod.invoke(app, window);
//
//    } catch (ClassNotFoundException cnfe) {
//      // ignored
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }


  //////////////////////////////////////////////////////////////


  /*
  // disabling for now; requires Java 1.7 and "precise" semantics are odd...
  // returns 0.1 for tick-by-tick scrolling on OS X, but it's not a matter of
  // calling ceil() on the value: 1.5 goes to 1, but 2.3 goes to 2.
  // "precise" is a whole different animal, so add later API to shore that up.
  static protected Method preciseWheelMethod;
  static {
    try {
      preciseWheelMethod = MouseWheelEvent.class.getMethod("getPreciseWheelRotation", new Class[] { });
    } catch (Exception e) {
      // ignored, the method will just be set to null
    }
  }
  */


  /**
   * Figure out how to process a mouse event. When loop() has been
   * called, the events will be queued up until drawing is complete.
   * If noLoop() has been called, then events will happen immediately.
   */
  protected void nativeMouseEvent(java.awt.event.MouseEvent nativeEvent) {
    // the 'amount' is the number of button clicks for a click event,
    // or the number of steps/clicks on the wheel for a mouse wheel event.
    int peCount = nativeEvent.getClickCount();

    int peAction = 0;
    switch (nativeEvent.getID()) {
    case java.awt.event.MouseEvent.MOUSE_PRESSED:
      peAction = MouseEvent.PRESS;
      break;
    case java.awt.event.MouseEvent.MOUSE_RELEASED:
      peAction = MouseEvent.RELEASE;
      break;
    case java.awt.event.MouseEvent.MOUSE_CLICKED:
      peAction = MouseEvent.CLICK;
      break;
    case java.awt.event.MouseEvent.MOUSE_DRAGGED:
      peAction = MouseEvent.DRAG;
      break;
    case java.awt.event.MouseEvent.MOUSE_MOVED:
      peAction = MouseEvent.MOVE;
      break;
    case java.awt.event.MouseEvent.MOUSE_ENTERED:
      peAction = MouseEvent.ENTER;
      break;
    case java.awt.event.MouseEvent.MOUSE_EXITED:
      peAction = MouseEvent.EXIT;
      break;
    //case java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL:
    case java.awt.event.MouseEvent.MOUSE_WHEEL:
      peAction = MouseEvent.WHEEL;
      /*
      if (preciseWheelMethod != null) {
        try {
          peAmount = ((Double) preciseWheelMethod.invoke(nativeEvent, (Object[]) null)).floatValue();
        } catch (Exception e) {
          preciseWheelMethod = null;
        }
      }
      */
      peCount = ((MouseWheelEvent) nativeEvent).getWheelRotation();
      break;
    }

    //System.out.println(nativeEvent);
    //int modifiers = nativeEvent.getModifiersEx();
    // If using getModifiersEx(), the regular modifiers don't set properly.
    int modifiers = nativeEvent.getModifiers();

    int peModifiers = modifiers &
      (InputEvent.SHIFT_MASK |
       InputEvent.CTRL_MASK |
       InputEvent.META_MASK |
       InputEvent.ALT_MASK);

    // Windows and OS X seem to disagree on how to handle this. Windows only
    // sets BUTTON1_DOWN_MASK, while OS X seems to set BUTTON1_MASK.
    // This is an issue in particular with mouse release events:
    // http://code.google.com/p/processing/issues/detail?id=1294
    // The fix for which led to a regression (fixed here by checking both):
    // http://code.google.com/p/processing/issues/detail?id=1332
    int peButton = 0;
//    if ((modifiers & InputEvent.BUTTON1_MASK) != 0 ||
//        (modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0) {
//      peButton = LEFT;
//    } else if ((modifiers & InputEvent.BUTTON2_MASK) != 0 ||
//               (modifiers & InputEvent.BUTTON2_DOWN_MASK) != 0) {
//      peButton = CENTER;
//    } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0 ||
//               (modifiers & InputEvent.BUTTON3_DOWN_MASK) != 0) {
//      peButton = RIGHT;
//    }
    if ((modifiers & InputEvent.BUTTON1_MASK) != 0) {
      peButton = PConstants.LEFT;
    } else if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
      peButton = PConstants.CENTER;
    } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
      peButton = PConstants.RIGHT;
    }

    // If running on Mac OS, allow ctrl-click as right mouse. Prior to 0215,
    // this used isPopupTrigger() on the native event, but that doesn't work
    // for mouseClicked and mouseReleased (or others).
    if (PApplet.platform == PConstants.MACOSX) {
      //if (nativeEvent.isPopupTrigger()) {
      if ((modifiers & InputEvent.CTRL_MASK) != 0) {
        peButton = PConstants.RIGHT;
      }
    }

    sketch.postEvent(new MouseEvent(nativeEvent, nativeEvent.getWhen(),
                                    peAction, peModifiers,
                                    nativeEvent.getX(), nativeEvent.getY(),
                                    peButton,
                                    peCount));
  }


  protected void nativeKeyEvent(java.awt.event.KeyEvent event) {
    int peAction = 0;
    switch (event.getID()) {
    case java.awt.event.KeyEvent.KEY_PRESSED:
      peAction = KeyEvent.PRESS;
      break;
    case java.awt.event.KeyEvent.KEY_RELEASED:
      peAction = KeyEvent.RELEASE;
      break;
    case java.awt.event.KeyEvent.KEY_TYPED:
      peAction = KeyEvent.TYPE;
      break;
    }

//    int peModifiers = event.getModifiersEx() &
//      (InputEvent.SHIFT_DOWN_MASK |
//       InputEvent.CTRL_DOWN_MASK |
//       InputEvent.META_DOWN_MASK |
//       InputEvent.ALT_DOWN_MASK);
    int peModifiers = event.getModifiers() &
      (InputEvent.SHIFT_MASK |
       InputEvent.CTRL_MASK |
       InputEvent.META_MASK |
       InputEvent.ALT_MASK);

    sketch.postEvent(new KeyEvent(event, event.getWhen(),
                                  peAction, peModifiers,
                                  event.getKeyChar(), event.getKeyCode()));
  }


  // listeners, for all my men!
  protected void addListeners() {

    canvas.addMouseListener(new MouseListener() {

      public void mousePressed(java.awt.event.MouseEvent e) {
        nativeMouseEvent(e);
      }

      public void mouseReleased(java.awt.event.MouseEvent e) {
        nativeMouseEvent(e);
      }

      public void mouseClicked(java.awt.event.MouseEvent e) {
        nativeMouseEvent(e);
      }

      public void mouseEntered(java.awt.event.MouseEvent e) {
        nativeMouseEvent(e);
      }

      public void mouseExited(java.awt.event.MouseEvent e) {
        nativeMouseEvent(e);
      }
    });

    canvas.addMouseMotionListener(new MouseMotionListener() {

      public void mouseDragged(java.awt.event.MouseEvent e) {
        nativeMouseEvent(e);
      }

      public void mouseMoved(java.awt.event.MouseEvent e) {
        nativeMouseEvent(e);
      }
    });

    canvas.addMouseWheelListener(new MouseWheelListener() {

      public void mouseWheelMoved(MouseWheelEvent e) {
        nativeMouseEvent(e);
      }
    });

    canvas.addKeyListener(new KeyListener() {

      public void keyPressed(java.awt.event.KeyEvent e) {
        nativeKeyEvent(e);
      }


      public void keyReleased(java.awt.event.KeyEvent e) {
        nativeKeyEvent(e);
      }


      public void keyTyped(java.awt.event.KeyEvent e) {
        nativeKeyEvent(e);
      }
    });

    canvas.addFocusListener(new FocusListener() {

      public void focusGained(FocusEvent e) {
        sketch.focused = true;
        sketch.focusGained();
      }

      public void focusLost(FocusEvent e) {
        sketch.focused = false;
        sketch.focusLost();
      }
    });
  }


  /*
  public void addListeners(Component comp) {
    comp.addMouseListener(this);
    comp.addMouseWheelListener(this);
    comp.addMouseMotionListener(this);
    comp.addKeyListener(this);
    comp.addFocusListener(this);
  }


  public void removeListeners(Component comp) {
    comp.removeMouseListener(this);
    comp.removeMouseWheelListener(this);
    comp.removeMouseMotionListener(this);
    comp.removeKeyListener(this);
    comp.removeFocusListener(this);
  }
  */


//  /**
//   * Call to remove, then add, listeners to a component.
//   * Avoids issues with double-adding.
//   */
//  public void updateListeners(Component comp) {
//    removeListeners(comp);
//    addListeners(comp);
//  }

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

      // If size un-initialized, might be a Canvas. Call setSize() here since
      // we now have a parent object that this Canvas can use as a peer.
      if (graphics.image == null) {
//        System.out.format("it's null, sketchW/H already set to %d %d%n", sketchWidth, sketchHeight);
        setSize(sketchWidth, sketchHeight);
//        System.out.format("  but now, sketchW/H changed to %d %d%n", sketchWidth, sketchHeight);
      }

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
//        if (g != null) {
        sketch.handleDraw();
//        }

        if (sketch.frameCount == 1) {
          requestFocus();
        }

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


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  int cursorType = PConstants.ARROW; // cursor type
  boolean cursorVisible = true; // cursor visibility flag
  Cursor invisibleCursor;


  public void setCursor(int kind) {
    // Swap the HAND cursor because MOVE doesn't seem to be available on OS X
    // https://github.com/processing/processing/issues/2358
    if (PApplet.platform == PConstants.MACOSX && kind == PConstants.MOVE) {
      kind = PConstants.HAND;
    }
    canvas.setCursor(Cursor.getPredefinedCursor(kind));
    cursorVisible = true;
    this.cursorType = kind;
  }


  public void setCursor(PImage img, int x, int y) {
    // don't set this as cursor type, instead use cursor_type
    // to save the last cursor used in case cursor() is called
    //cursor_type = Cursor.CUSTOM_CURSOR;
    Image jimage =
      canvas.getToolkit().createImage(new MemoryImageSource(img.width, img.height,
                                        img.pixels, 0, img.width));
    Point hotspot = new Point(x, y);
    Toolkit tk = Toolkit.getDefaultToolkit();
    Cursor cursor = tk.createCustomCursor(jimage, hotspot, "Custom Cursor");
    canvas.setCursor(cursor);
    cursorVisible = true;

  }


  public void showCursor() {
    // maybe should always set here? seems dangerous, since
    // it's likely that java will set the cursor to something
    // else on its own, and the applet will be stuck b/c bagel
    // thinks that the cursor is set to one particular thing
    if (!cursorVisible) {
      cursorVisible = true;
      canvas.setCursor(Cursor.getPredefinedCursor(cursorType));
    }
  }


  public void hideCursor() {
    // in 0216, just re-hide it?
//  if (!cursorVisible) return;  // don't hide if already hidden.

    if (invisibleCursor == null) {
      BufferedImage cursorImg =
        new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
      invisibleCursor =
        canvas.getToolkit().createCustomCursor(cursorImg, new Point(8, 8), "blank");
    }
    // was formerly 16x16, but the 0x0 was added by jdf as a fix
    // for macosx, which wasn't honoring the invisible cursor
//  cursor(invisibleCursor, 8, 8);
    canvas.setCursor(invisibleCursor);
    cursorVisible = false;
  }


  void debug(String format, Object ... args) {
    System.out.format(format + "%n", args);
  }
}