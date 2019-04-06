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

package processing.javafx;

import com.sun.glass.ui.Screen;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import processing.core.*;


public class PSurfaceFX implements PSurface {
  PApplet sketch;

  PGraphicsFX2D fx;
  Stage stage;
  Canvas canvas;

  final Animation animation;
  float frameRate = 60;

  private SynchronousQueue<Throwable> drawExceptionQueue = new SynchronousQueue<>();

  public PSurfaceFX(PGraphicsFX2D graphics) {
    fx = graphics;
    canvas = new ResizableCanvas();

    // set up main drawing loop
    KeyFrame keyFrame = new KeyFrame(Duration.millis(1000),
                                     new EventHandler<ActionEvent>() {
      public void handle(ActionEvent event) {
        long startNanoTime = System.nanoTime();
        try {
          sketch.handleDraw();
        } catch (Throwable e) {
          // Let exception handler thread crash with our exception
          drawExceptionQueue.offer(e);
          // Stop animating right now so nothing runs afterwards
          // and crash frame can be for example traced by println()
          animation.stop();
          return;
        }
        long drawNanos = System.nanoTime() - startNanoTime;

        if (sketch.exitCalled()) {
          // using Platform.runLater() didn't work
//          Platform.runLater(new Runnable() {
//            public void run() {
          // instead of System.exit(), safely shut down JavaFX this way
          Platform.exit();
//            }
//          });
        }
        if (sketch.frameCount > 5) {
          animation.setRate(-PApplet.min(1e9f / drawNanos, frameRate));
        }
      }
    });
    animation = new Timeline(keyFrame);
    animation.setCycleCount(Animation.INDEFINITE);

    // key frame has duration of 1 second, so the rate of the animation
    // should be set to frames per second

    // setting rate to negative so that event fires at the start of
    // the key frame and first frame is drawn immediately
    animation.setRate(-frameRate);
  }


  public Object getNative() {
    return canvas;
  }


  class ResizableCanvas extends Canvas {

    public ResizableCanvas() {
      widthProperty().addListener(new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> value,
                            Number oldWidth, Number newWidth) {
//          sketch.width = newWidth.intValue();
          sketch.setSize(newWidth.intValue(), sketch.height);
//          draw();
          fx.setSize(sketch.width, sketch.height);
        }
      });
      heightProperty().addListener(new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> value,
                            Number oldHeight, Number newHeight) {
//          sketch.height = newHeight.intValue();
          sketch.setSize(sketch.width, newHeight.intValue());
//          draw();
          fx.setSize(sketch.width, sketch.height);
        }
      });

      //addEventHandler(eventType, eventHandler);

      EventHandler<MouseEvent> mouseHandler = new EventHandler<MouseEvent>() {
        public void handle(MouseEvent e) {
          fxMouseEvent(e);
        }
      };

      setOnMousePressed(mouseHandler);
      setOnMouseReleased(mouseHandler);
      setOnMouseClicked(mouseHandler);
      setOnMouseEntered(mouseHandler);
      setOnMouseExited(mouseHandler);

      setOnMouseDragged(mouseHandler);
      setOnMouseMoved(mouseHandler);

      setOnScroll(new EventHandler<ScrollEvent>() {
        public void handle(ScrollEvent e) {
          fxScrollEvent(e);
        }
      });

      EventHandler<KeyEvent> keyHandler = new EventHandler<KeyEvent>() {
        public void handle(KeyEvent e) {
          fxKeyEvent(e);
        }
      };

      setOnKeyPressed(keyHandler);
      setOnKeyReleased(keyHandler);
      setOnKeyTyped(keyHandler);

      setFocusTraversable(false);  // prevent tab from de-focusing

      focusedProperty().addListener(new ChangeListener<Boolean>() {
        public void changed(ObservableValue<? extends Boolean> value,
                            Boolean oldValue, Boolean newValue) {
          if (newValue.booleanValue()) {
            sketch.focused = true;
            sketch.focusGained();
          } else {
            sketch.focused = false;
            sketch.focusLost();
          }
        }
      });
    }

    public Stage getStage() {
      return stage;
    }

    @Override
    public boolean isResizable() {
      return true;
    }

    @Override
    public double prefWidth(double height) {
      return getWidth();
    }

    @Override
    public double prefHeight(double width) {
      return getHeight();
    }
  }


  public void initOffscreen(PApplet sketch) {
  }


//  public Component initComponent(PApplet sketch) {
//    return null;
//  }


  static public class PApplicationFX extends Application {
    static public PSurfaceFX surface;
//    static String title;  // title set at launch
//    static boolean resizable;  // set at launch

    public PApplicationFX() { }

    @Override
    public void start(final Stage stage) {
//      if (title != null) {
//        stage.setTitle(title);
//      }

      PApplet sketch = surface.sketch;

      // See JEP 263
      float renderScale = Screen.getMainScreen().getRecommendedOutputScaleX();
      if (PApplet.platform == PConstants.MACOSX) {
        for (Screen s : Screen.getScreens()) {
          renderScale = Math.max(renderScale, s.getRecommendedOutputScaleX());
        }
      }
      float uiScale = Screen.getMainScreen().getRecommendedOutputScaleX();
      if (sketch.pixelDensity == 2 && renderScale < 2) {
        sketch.pixelDensity = 1;
        sketch.g.pixelDensity = 1;
        System.err.println("pixelDensity(2) is not available for this display");
      }

      // Use AWT display code, because FX orders screens in different way
      GraphicsDevice displayDevice = null;

      GraphicsEnvironment environment =
          GraphicsEnvironment.getLocalGraphicsEnvironment();

      int displayNum = sketch.sketchDisplay();
      if (displayNum > 0) {  // if -1, use the default device
        GraphicsDevice[] devices = environment.getScreenDevices();
        if (displayNum <= devices.length) {
          displayDevice = devices[displayNum - 1];
        } else {
          System.err.format("Display %d does not exist, " +
                            "using the default display instead.%n", displayNum);
          for (int i = 0; i < devices.length; i++) {
            System.err.format("Display %d is %s%n", (i+1), devices[i]);
          }
        }
      }
      if (displayDevice == null) {
        displayDevice = environment.getDefaultScreenDevice();
      }

      boolean fullScreen = sketch.sketchFullScreen();
      boolean spanDisplays = sketch.sketchDisplay() == PConstants.SPAN;

      Rectangle primaryScreenRect = displayDevice.getDefaultConfiguration().getBounds();
      Rectangle screenRect = primaryScreenRect;
      if (fullScreen || spanDisplays) {
        double minX = screenRect.getMinX();
        double maxX = screenRect.getMaxX();
        double minY = screenRect.getMinY();
        double maxY = screenRect.getMaxY();
        if (spanDisplays) {
          for (GraphicsDevice s : environment.getScreenDevices()) {
            Rectangle bounds = s.getDefaultConfiguration().getBounds();
            minX = Math.min(minX, bounds.getMinX());
            maxX = Math.max(maxX, bounds.getMaxX());
            minY = Math.min(minY, bounds.getMinY());
            maxY = Math.max(maxY, bounds.getMaxY());
          }
        }
        screenRect = new Rectangle((int) minX, (int) minY,
                                   (int) (maxX - minX), (int) (maxY - minY));
      }

      // Set the displayWidth/Height variables inside PApplet, so that they're
      // usable and can even be returned by the sketchWidth()/Height() methods.
      sketch.displayWidth = (int) screenRect.getWidth();
      sketch.displayHeight = (int) screenRect.getHeight();

      int sketchWidth = sketch.sketchWidth();
      int sketchHeight = sketch.sketchHeight();

      if (fullScreen || spanDisplays) {
        sketchWidth = (int) screenRect.getWidth();
        sketchHeight = (int) screenRect.getHeight();

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setX(screenRect.getMinX());
        stage.setY(screenRect.getMinY());
        stage.setWidth(screenRect.getWidth());
        stage.setHeight(screenRect.getHeight());
      }

      Canvas canvas = surface.canvas;
      surface.fx.context = canvas.getGraphicsContext2D();
      StackPane stackPane = new StackPane();
      stackPane.getChildren().add(canvas);
      canvas.widthProperty().bind(stackPane.widthProperty());
      canvas.heightProperty().bind(stackPane.heightProperty());

      int width = sketchWidth;
      int height = sketchHeight;
      int smooth = sketch.sketchSmooth();

      // Workaround for https://bugs.openjdk.java.net/browse/JDK-8136495
      // https://github.com/processing/processing/issues/3823
      if ((PApplet.platform == PConstants.MACOSX ||
           PApplet.platform == PConstants.LINUX) &&
          PApplet.javaVersionName.compareTo("1.8.0_60") >= 0 &&
          PApplet.javaVersionName.compareTo("1.8.0_72") < 0) {
        System.err.println("smooth() disabled for JavaFX with this Java version due to Oracle bug");
        System.err.println("https://github.com/processing/processing/issues/3795");
        smooth = 0;
      }

      SceneAntialiasing sceneAntialiasing = (smooth == 0) ?
        SceneAntialiasing.DISABLED : SceneAntialiasing.BALANCED;

      stage.setScene(new Scene(stackPane, width, height, false, sceneAntialiasing));

      // initFrame in different thread is waiting for
      // the stage, assign it only when it is all set up
      surface.stage = stage;
    }

    @Override
    public void stop() throws Exception {
      surface.sketch.dispose();
    }
  }


  //public Frame initFrame(PApplet sketch, java.awt.Color backgroundColor,
  public void initFrame(PApplet sketch) {/*, int backgroundColor,
                         int deviceIndex, boolean fullScreen,
                         boolean spanDisplays) {*/
    this.sketch = sketch;
    PApplicationFX.surface = this;
    //Frame frame = new DummyFrame();
    new Thread(new Runnable() {
      public void run() {
        Application.launch(PApplicationFX.class);
      }
    }).start();

    // wait for stage to be initialized on its own thread before continuing
    while (stage == null) {
      try {
        //System.out.println("waiting for launch");
        Thread.sleep(5);
      } catch (InterruptedException e) { }
    }

    startExceptionHandlerThread();

    setProcessingIcon(stage);
  }


  private void startExceptionHandlerThread() {
    Thread exceptionHandlerThread = new Thread(() -> {
      Throwable drawException;
      try {
        drawException = drawExceptionQueue.take();
      } catch (InterruptedException e) {
        return;
      }
      // Adapted from PSurfaceJOGL
      if (drawException != null) {
        if (drawException instanceof ThreadDeath) {
//            System.out.println("caught ThreadDeath");
//            throw (ThreadDeath)cause;
        } else if (drawException instanceof RuntimeException) {
          throw (RuntimeException) drawException;
        } else if (drawException instanceof UnsatisfiedLinkError) {
          throw new UnsatisfiedLinkError(drawException.getMessage());
        } else {
          throw new RuntimeException(drawException);
        }
      }
    });
    exceptionHandlerThread.setDaemon(true);
    exceptionHandlerThread.setName("Processing-FX-ExceptionHandler");
    exceptionHandlerThread.start();
  }


  /** Set the window (and dock, or whatever necessary) title. */
  public void setTitle(String title) {
//    PApplicationFX.title = title;  // store this in case the stage still null
//    if (stage != null) {
    stage.setTitle(title);
//    }
  }


  /** Show or hide the window. */
  @Override
  public void setVisible(final boolean visible) {
    Platform.runLater(new Runnable() {
      public void run() {
        if (visible) {
          stage.show();
          canvas.requestFocus();
        } else {
          stage.hide();
        }
      }
    });
  }


  /** Set true if we want to resize things (default is not resizable) */
  public void setResizable(boolean resizable) {
//    PApplicationFX.resizable = resizable;
//    if (stage != null) {
    stage.setResizable(resizable);
//    }
  }


  public void setIcon(PImage icon) {
    int w = icon.pixelWidth;
    int h = icon.pixelHeight;
    WritableImage im = new WritableImage(w, h);
    im.getPixelWriter().setPixels(0, 0, w, h,
                                  PixelFormat.getIntArgbInstance(),
                                  icon.pixels,
                                  0, w);

    Stage stage = (Stage) canvas.getScene().getWindow();
    stage.getIcons().clear();
    stage.getIcons().add(im);
  }


  List<Image> iconImages;

  protected void setProcessingIcon(Stage stage) {
    // Adapted from PSurfaceAWT
    // Note: FX chooses wrong icon size, should be fixed in Java 9, see:
    // https://bugs.openjdk.java.net/browse/JDK-8091186
    // Removing smaller sizes helps a bit, but big ones are downsized
    try {
      if (iconImages == null) {
        iconImages = new ArrayList<>();
        final int[] sizes = { 48, 64, 128, 256, 512 };

        for (int sz : sizes) {
          URL url = PApplet.class.getResource("/icon/icon-" + sz + ".png");
          Image image = new Image(url.toString());
          iconImages.add(image);
        }
      }
      List<Image> icons = stage.getIcons();
      icons.clear();
      icons.addAll(iconImages);
    } catch (Exception e) { }  // harmless; keep this to ourselves
  }


  @Override
  public void setAlwaysOnTop(boolean always) {
    stage.setAlwaysOnTop(always);
  }


  /*
  @Override
  public void placeWindow(int[] location) {
    //setFrameSize();

    if (location != null) {
      // a specific location was received from the Runner
      // (applet has been run more than once, user placed window)
      stage.setX(location[0]);
      stage.setY(location[1]);

    } else {  // just center on screen
      // Can't use frame.setLocationRelativeTo(null) because it sends the
      // frame to the main display, which undermines the --display setting.
//      frame.setLocation(screenRect.x + (screenRect.width - sketchWidth) / 2,
//                        screenRect.y + (screenRect.height - sketchHeight) / 2);
    }
    if (stage.getY() < 0) {
      // Windows actually allows you to place frames where they can't be
      // closed. Awesome. http://dev.processing.org/bugs/show_bug.cgi?id=1508
      //frame.setLocation(frameLoc.x, 30);
      stage.setY(30);
    }

    //setCanvasSize();

    // TODO add window closing behavior
//    frame.addWindowListener(new WindowAdapter() {
//      @Override
//      public void windowClosing(WindowEvent e) {
//        System.exit(0);
//      }
//    });

    // TODO handle frame resizing events
//    setupFrameResizeListener();

    if (sketch.getGraphics().displayable()) {
      setVisible(true);
    }
  }
  */


  @Override
  public void placeWindow(int[] location, int[] editorLocation) {
    if (sketch.sketchFullScreen()) {
      PApplet.hideMenuBar();
      return;
    }

    int wide = sketch.width;  // stage.getWidth() is NaN here
    //int high = sketch.height;  // stage.getHeight()

    if (location != null) {
      // a specific location was received from the Runner
      // (applet has been run more than once, user placed window)
      stage.setX(location[0]);
      stage.setY(location[1]);

    } else if (editorLocation != null) {
      int locationX = editorLocation[0] - 20;
      int locationY = editorLocation[1];

      if (locationX - wide > 10) {
        // if it fits to the left of the window
        stage.setX(locationX - wide);
        stage.setY(locationY);

      } else {  // doesn't fit
        stage.centerOnScreen();
      }
    } else {  // just center on screen
      stage.centerOnScreen();
    }
  }


  // http://download.java.net/jdk8/jfxdocs/javafx/stage/Stage.html#setFullScreenExitHint-java.lang.String-
  // http://download.java.net/jdk8/jfxdocs/javafx/stage/Stage.html#setFullScreenExitKeyCombination-javafx.scene.input.KeyCombination-
  public void placePresent(int stopColor) {
    // TODO Auto-generated method stub
    PApplet.hideMenuBar();
  }


  @Override
  public void setupExternalMessages() {
    stage.xProperty().addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> value,
                          Number oldX, Number newX) {
        sketch.frameMoved(newX.intValue(), stage.yProperty().intValue());
      }
    });

    stage.yProperty().addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> value,
                          Number oldY, Number newY) {
        sketch.frameMoved(stage.xProperty().intValue(), newY.intValue());
      }
    });

    stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
      public void handle(WindowEvent we) {
        sketch.exit();
      }
    });
  }


  public void setLocation(int x, int y) {
    stage.setX(x);
    stage.setY(y);
  }


  public void setSize(int wide, int high) {
    // When the surface is set to resizable via surface.setResizable(true),
    // a crash may occur if the user sets the window to size zero.
    // https://github.com/processing/processing/issues/5052
    if (high <= 0) {
      high = 1;
    }
    if (wide <= 0) {
      wide = 1;
    }

    //System.out.format("%s.setSize(%d, %d)%n", getClass().getSimpleName(), width, height);
    Scene scene = stage.getScene();
    double decorH = stage.getWidth() - scene.getWidth();
    double decorV = stage.getHeight() - scene.getHeight();
    stage.setWidth(wide + decorH);
    stage.setHeight(high + decorV);
    fx.setSize(wide, high);
  }


//  public Component getComponent() {
//    return null;
//  }


  public void setSmooth(int level) {
    // TODO Auto-generated method stub

  }


  public void setFrameRate(float fps) {
    // setting rate to negative so that event fires at the start of
    // the key frame and first frame is drawn immediately
    if (fps > 0) {
      frameRate = fps;
      animation.setRate(-frameRate);
    }
  }


//  @Override
//  public void requestFocus() {
//    canvas.requestFocus();
//  }

  Cursor lastCursor = Cursor.DEFAULT;

  public void setCursor(int kind) {
    Cursor c;
    switch (kind) {
      case PConstants.ARROW: c = Cursor.DEFAULT; break;
      case PConstants.CROSS: c = Cursor.CROSSHAIR; break;
      case PConstants.HAND: c = Cursor.HAND; break;
      case PConstants.MOVE: c = Cursor.MOVE; break;
      case PConstants.TEXT: c = Cursor.TEXT; break;
      case PConstants.WAIT: c = Cursor.WAIT; break;
      default: c = Cursor.DEFAULT; break;
    }
    lastCursor = c;
    canvas.getScene().setCursor(c);
  }


  public void setCursor(PImage image, int hotspotX, int hotspotY) {
    int w = image.pixelWidth;
    int h = image.pixelHeight;
    WritableImage im = new WritableImage(w, h);
    im.getPixelWriter().setPixels(0, 0, w, h,
                                  PixelFormat.getIntArgbInstance(),
                                  image.pixels,
                                  0, w);
    ImageCursor c = new ImageCursor(im, hotspotX, hotspotY);
    lastCursor = c;
    canvas.getScene().setCursor(c);
  }


  public void showCursor() {
    canvas.getScene().setCursor(lastCursor);
  }


  public void hideCursor() {
    canvas.getScene().setCursor(Cursor.NONE);
  }


  public void startThread() {
    animation.play();
  }


  public void pauseThread() {
    animation.pause();
  }


  public void resumeThread() {
    animation.play();
  }


  public boolean stopThread() {
    animation.stop();
    return true;
  }


  public boolean isStopped() {
    return animation.getStatus() == Animation.Status.STOPPED;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /*
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
  */


  static Map<EventType<? extends MouseEvent>, Integer> mouseMap =
    new HashMap<>();
  static {
    mouseMap.put(MouseEvent.MOUSE_PRESSED, processing.event.MouseEvent.PRESS);
    mouseMap.put(MouseEvent.MOUSE_RELEASED, processing.event.MouseEvent.RELEASE);
    mouseMap.put(MouseEvent.MOUSE_CLICKED, processing.event.MouseEvent.CLICK);
    mouseMap.put(MouseEvent.MOUSE_DRAGGED, processing.event.MouseEvent.DRAG);
    mouseMap.put(MouseEvent.MOUSE_MOVED, processing.event.MouseEvent.MOVE);
    mouseMap.put(MouseEvent.MOUSE_ENTERED, processing.event.MouseEvent.ENTER);
    mouseMap.put(MouseEvent.MOUSE_EXITED, processing.event.MouseEvent.EXIT);
  }

  protected void fxMouseEvent(MouseEvent fxEvent) {
    // the 'amount' is the number of button clicks for a click event,
    // or the number of steps/clicks on the wheel for a mouse wheel event.
    int count = fxEvent.getClickCount();

    int action = mouseMap.get(fxEvent.getEventType());

    int modifiers = 0;
    if (fxEvent.isShiftDown()) {
      modifiers |= processing.event.Event.SHIFT;
    }
    if (fxEvent.isControlDown()) {
      modifiers |= processing.event.Event.CTRL;
    }
    if (fxEvent.isMetaDown()) {
      modifiers |= processing.event.Event.META;
    }
    if (fxEvent.isAltDown()) {
      modifiers |= processing.event.Event.ALT;
    }

    int button = 0;
    switch (fxEvent.getButton()) {
      case PRIMARY:
        button = PConstants.LEFT;
        break;
      case SECONDARY:
        button = PConstants.RIGHT;
        break;
      case MIDDLE:
        button = PConstants.CENTER;
        break;
      case NONE:
        // not currently handled
        break;
    }

    //long when = nativeEvent.getWhen();  // from AWT
    long when = System.currentTimeMillis();
    int x = (int) fxEvent.getX();  // getSceneX()?
    int y = (int) fxEvent.getY();

    sketch.postEvent(new processing.event.MouseEvent(fxEvent, when,
                                                     action, modifiers,
                                                     x, y, button, count));
  }

  // https://docs.oracle.com/javase/8/javafx/api/javafx/scene/input/ScrollEvent.html
  protected void fxScrollEvent(ScrollEvent fxEvent) {
    // the number of steps/clicks on the wheel for a mouse wheel event.
    int count = (int) -(fxEvent.getDeltaY() / fxEvent.getMultiplierY());

    int action = processing.event.MouseEvent.WHEEL;

    int modifiers = 0;
    if (fxEvent.isShiftDown()) {
      modifiers |= processing.event.Event.SHIFT;
    }
    if (fxEvent.isControlDown()) {
      modifiers |= processing.event.Event.CTRL;
    }
    if (fxEvent.isMetaDown()) {
      modifiers |= processing.event.Event.META;
    }
    if (fxEvent.isAltDown()) {
      modifiers |= processing.event.Event.ALT;
    }

    // FX does not supply button info
    int button = 0;

    long when = System.currentTimeMillis();
    int x = (int) fxEvent.getX();  // getSceneX()?
    int y = (int) fxEvent.getY();

    sketch.postEvent(new processing.event.MouseEvent(fxEvent, when,
                                                     action, modifiers,
                                                     x, y, button, count));
  }


  protected void fxKeyEvent(javafx.scene.input.KeyEvent fxEvent) {
    int action = 0;
    EventType<? extends KeyEvent> et = fxEvent.getEventType();
    if (et == KeyEvent.KEY_PRESSED) {
      action = processing.event.KeyEvent.PRESS;
    } else if (et == KeyEvent.KEY_RELEASED) {
      action = processing.event.KeyEvent.RELEASE;
    } else if (et == KeyEvent.KEY_TYPED) {
      action = processing.event.KeyEvent.TYPE;
    }

    int modifiers = 0;
    if (fxEvent.isShiftDown()) {
      modifiers |= processing.event.Event.SHIFT;
    }
    if (fxEvent.isControlDown()) {
      modifiers |= processing.event.Event.CTRL;
    }
    if (fxEvent.isMetaDown()) {
      modifiers |= processing.event.Event.META;
    }
    if (fxEvent.isAltDown()) {
      modifiers |= processing.event.Event.ALT;
    }

    long when = System.currentTimeMillis();

    char keyChar = getKeyChar(fxEvent);
    int keyCode = getKeyCode(fxEvent);
    sketch.postEvent(new processing.event.KeyEvent(fxEvent, when,
                                                   action, modifiers,
                                                   keyChar, keyCode));
  }


  @SuppressWarnings("deprecation")
  private int getKeyCode(KeyEvent fxEvent) {
    if (fxEvent.getEventType() == KeyEvent.KEY_TYPED) {
      return 0;
    }

    KeyCode kc = fxEvent.getCode();
    switch (kc) {
      case ALT_GRAPH:
        return PConstants.ALT;
      default:
        break;
    }
    return kc.getCode();
  }


  @SuppressWarnings("deprecation")
  private char getKeyChar(KeyEvent fxEvent) {
    KeyCode kc = fxEvent.getCode();

    // Overriding chars for some
    // KEY_PRESSED and KEY_RELEASED events
    switch (kc) {
      case UP:
      case KP_UP:
      case DOWN:
      case KP_DOWN:
      case LEFT:
      case KP_LEFT:
      case RIGHT:
      case KP_RIGHT:
      case ALT:
      case ALT_GRAPH:
      case CONTROL:
      case SHIFT:
      case CAPS:
      case META:
      case WINDOWS:
      case CONTEXT_MENU:
      case HOME:
      case PAGE_UP:
      case PAGE_DOWN:
      case END:
      case PAUSE:
      case PRINTSCREEN:
      case INSERT:
      case NUM_LOCK:
      case SCROLL_LOCK:
      case F1:
      case F2:
      case F3:
      case F4:
      case F5:
      case F6:
      case F7:
      case F8:
      case F9:
      case F10:
      case F11:
      case F12:
        return PConstants.CODED;
      case ENTER:
        return '\n';
      case DIVIDE:
        return '/';
      case MULTIPLY:
        return '*';
      case SUBTRACT:
        return '-';
      case ADD:
        return '+';
      case NUMPAD0:
        return '0';
      case NUMPAD1:
        return '1';
      case NUMPAD2:
        return '2';
      case NUMPAD3:
        return '3';
      case NUMPAD4:
        return '4';
      case NUMPAD5:
        return '5';
      case NUMPAD6:
        return '6';
      case NUMPAD7:
        return '7';
      case NUMPAD8:
        return '8';
      case NUMPAD9:
        return '9';
      case DECIMAL:
        // KEY_TYPED does not go through here and will produce
        // dot or comma based on the keyboard layout.
        // For KEY_PRESSED and KEY_RELEASED, let's just go with
        // the dot. Users can detect the key by its keyCode.
        return '.';
      case UNDEFINED:
        // KEY_TYPED has KeyCode: UNDEFINED
        // and falls through here
        break;
      default:
        break;
    }

    // Just go with what FX gives us for the rest of
    // KEY_PRESSED and KEY_RELEASED and all of KEY_TYPED
    String ch;
    if (fxEvent.getEventType() == KeyEvent.KEY_TYPED) {
      ch = fxEvent.getCharacter();
    } else {
      ch = kc.getChar();
    }

    if (ch.length() < 1) return PConstants.CODED;
    if (ch.startsWith("\r")) return '\n'; // normalize enter key
    return ch.charAt(0);
  }
}
