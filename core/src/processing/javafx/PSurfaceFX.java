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

//import java.awt.event.FocusEvent;
//import java.awt.event.FocusListener;
//import java.awt.event.KeyListener;
//import java.awt.event.MouseListener;
//import java.awt.event.MouseMotionListener;
//import java.awt.event.MouseWheelEvent;
//import java.awt.event.MouseWheelListener;

import java.util.HashMap;
import java.util.Map;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javafx.util.Duration;
import processing.core.*;


public class PSurfaceFX implements PSurface {
  PApplet sketch;

  PGraphicsFX2D fx;
  Stage stage;
  Canvas canvas;

  final Animation animation;

  public PSurfaceFX(PGraphicsFX2D graphics) {
    fx = graphics;
    canvas = new ResizableCanvas();
    fx.context = canvas.getGraphicsContext2D();

    { // set up main drawing loop
      KeyFrame keyFrame = new KeyFrame(Duration.millis(1000), e -> {
        sketch.handleDraw();
        if (sketch.exitCalled()) {
          //sketch.exitActual();  // just calls System.exit()
          Platform.exit();  // version for safe JavaFX shutdown
        }
      });
      animation = new Timeline(keyFrame);
      animation.setCycleCount(Timeline.INDEFINITE);

      // key frame has duration of 1 second, so the rate of the animation
      // should be set to frames per second

      // setting rate to negative so that event fires at the start of
      // the key frame and first frame is drawn immediately
      animation.setRate(-60);
    }
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

      Canvas canvas = surface.canvas;
      StackPane stackPane = new StackPane();
      stackPane.getChildren().add(canvas);
      canvas.widthProperty().bind(stackPane.widthProperty());
      canvas.heightProperty().bind(stackPane.heightProperty());

      PApplet sketch = surface.sketch;
      int width = sketch.sketchWidth();
      int height = sketch.sketchHeight();

      //stage.setScene(new Scene(new Group(canvas)));
      stage.setScene(new Scene(stackPane, width, height));
      //stage.show();  // move to setVisible(true)?

      // initFrame in different thread is waiting for
      // the stage, assign it only when it is all set up
      surface.stage = stage;
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
  public void setVisible(boolean visible) {
    Platform.runLater(new Runnable() {
      public void run() {
        stage.show();
        canvas.requestFocus();
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
    // TODO implement this in JavaFX
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
    //Dimension window = setFrameSize();

//    int contentW = Math.max(sketchWidth, MIN_WINDOW_WIDTH);
//    int contentH = Math.max(sketchHeight, MIN_WINDOW_HEIGHT);

    if (location != null) {
      // a specific location was received from the Runner
      // (applet has been run more than once, user placed window)
      stage.setX(location[0]);
      stage.setY(location[1]);

    } else if (editorLocation != null) {
      int locationX = editorLocation[0] - 20;
      int locationY = editorLocation[1];

      if (locationX - stage.getWidth() > 10) {
        // if it fits to the left of the window
        stage.setX(locationX - stage.getWidth());
        stage.setY(locationY);

      } else {  // doesn't fit
        // if it fits inside the editor window,
        // offset slightly from upper lefthand corner
        // so that it's plunked inside the text area
        locationX = editorLocation[0] + 66;
        locationY = editorLocation[1] + 66;

        if ((locationX + stage.getWidth() > sketch.displayWidth - 33) ||
            (locationY + stage.getHeight() > sketch.displayHeight - 33)) {
          // otherwise center on screen
          locationX = (int) ((sketch.displayWidth - stage.getWidth()) / 2);
          locationY = (int) ((sketch.displayHeight - stage.getHeight()) / 2);
        }
        stage.setX(locationX);
        stage.setY(locationY);
      }
    } else {  // just center on screen
      //setFrameCentered();
    }
    if (stage.getY() < 0) {
      // Windows actually allows you to place frames where they can't be
      // closed. Awesome. http://dev.processing.org/bugs/show_bug.cgi?id=1508
      //frame.setLocation(frameLoc.x, 30);
      stage.setY(30);
    }

    //canvas.setBounds((contentW - sketchWidth)/2,
    //                 (contentH - sketchHeight)/2,
    //                 sketchWidth, sketchHeight);

    // handle frame resizing events
    //setupFrameResizeListener();

    if (sketch.getGraphics().displayable()) {
      setVisible(true);
    }
  }


  // http://download.java.net/jdk8/jfxdocs/javafx/stage/Stage.html#setFullScreenExitHint-java.lang.String-
  // http://download.java.net/jdk8/jfxdocs/javafx/stage/Stage.html#setFullScreenExitKeyCombination-javafx.scene.input.KeyCombination-
  public void placePresent(int stopColor) {
    // TODO Auto-generated method stub
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


  public void setSize(int width, int height) {
    //System.out.format("%s.setSize(%d, %d)%n", getClass().getSimpleName(), width, height);
    Scene scene = stage.getScene();
    double decorH = stage.getWidth() - scene.getWidth();
    double decorV = stage.getHeight() - scene.getHeight();
    stage.setWidth(width + decorH);
    stage.setHeight(height + decorV);
    fx.setSize(width, height);
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
    if (fps > 0) animation.setRate(-fps);
  }


//  @Override
//  public void requestFocus() {
//    canvas.requestFocus();
//  }


  public void setCursor(int kind) {
    // TODO Auto-generated method stub

  }


  public void setCursor(PImage image, int hotspotX, int hotspotY) {
    // TODO Auto-generated method stub

  }


  public void showCursor() {
    // TODO Auto-generated method stub

  }


  public void hideCursor() {
    // TODO Auto-generated method stub

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
    new HashMap<EventType<? extends MouseEvent>, Integer>();
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
    //EventType<? extends MouseEvent> et = nativeEvent.getEventType();
//    if (et == MouseEvent.MOUSE_PRESSED) {
//      peAction = processing.event.MouseEvent.PRESS;
//    } else if (et == MouseEvent.MOUSE_RELEASED) {
//      peAction = processing.event.MouseEvent.RELEASE;

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
    if (fxEvent.isPrimaryButtonDown()) {
      button = PConstants.LEFT;
    } else if (fxEvent.isSecondaryButtonDown()) {
      button = PConstants.RIGHT;
    } else if (fxEvent.isMiddleButtonDown()) {
      button = PConstants.CENTER;
    }

    // If running on Mac OS, allow ctrl-click as right mouse.
    // Verified to be necessary with Java 8u45.
    if (PApplet.platform == PConstants.MACOSX &&
        fxEvent.isControlDown() &&
        button == PConstants.LEFT) {
      button = PConstants.RIGHT;
    }

    //long when = nativeEvent.getWhen();  // from AWT
    long when = System.currentTimeMillis();
    int x = (int) fxEvent.getX();  // getSceneX()?
    int y = (int) fxEvent.getY();

    sketch.postEvent(new processing.event.MouseEvent(fxEvent, when,
                                                     action, modifiers,
                                                     x, y, button, count));
  }


  // https://docs.oracle.com/javafx/2/api/javafx/scene/input/ScrollEvent.html
  protected void fxScrollEvent(ScrollEvent event) {
//   //case java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL:
//    case java.awt.event.MouseEvent.MOUSE_WHEEL:
//      peAction = MouseEvent.WHEEL;
//      /*
//      if (preciseWheelMethod != null) {
//        try {
//          peAmount = ((Double) preciseWheelMethod.invoke(nativeEvent, (Object[]) null)).floatValue();
//        } catch (Exception e) {
//          preciseWheelMethod = null;
//        }
//      }
//      */
//      peCount = ((MouseWheelEvent) nativeEvent).getWheelRotation();
//      break;
  }


  @SuppressWarnings("deprecation")
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
    KeyCode kc = fxEvent.getCode();
    // Are they f*ing serious?
    char key = kc.impl_getChar().charAt(0);
    int keyCode = kc.impl_getCode();
    sketch.postEvent(new processing.event.KeyEvent(fxEvent, when,
                                                   action, modifiers,
                                                   key, keyCode));
  }
}