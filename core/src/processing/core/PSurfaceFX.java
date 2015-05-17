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

//import java.awt.event.FocusEvent;
//import java.awt.event.FocusListener;
//import java.awt.event.KeyListener;
//import java.awt.event.MouseListener;
//import java.awt.event.MouseMotionListener;
//import java.awt.event.MouseWheelEvent;
//import java.awt.event.MouseWheelListener;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

//import processing.event.KeyEvent;
//import processing.event.MouseEvent;


public class PSurfaceFX implements PSurface {
  PApplet sketch;

  PGraphicsFX2D fx;
  Stage stage;
  Canvas canvas;

  AnimationTimer timer;


  public PSurfaceFX(PGraphicsFX2D graphics) {
    fx = graphics;
    canvas = new ResizableCanvas();
    fx.context = canvas.getGraphicsContext2D();
  }


  class ResizableCanvas extends Canvas {

    public ResizableCanvas() {
      widthProperty().addListener(new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> value,
                            Number oldWidth, Number newWidth) {
          sketch.width = newWidth.intValue();
//          draw();
          fx.setSize(sketch.width, sketch.height);
        }
      });
      heightProperty().addListener(new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> value,
                            Number oldHeight, Number newHeight) {
          sketch.height = newHeight.intValue();
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
      surface.stage = stage;
//      if (title != null) {
//        stage.setTitle(title);
//      }

      Canvas canvas = surface.canvas;
      StackPane stackPane = new StackPane();
      stackPane.getChildren().add(canvas);
      canvas.widthProperty().bind(stackPane.widthProperty());
      canvas.heightProperty().bind(stackPane.heightProperty());

      //stage.setScene(new Scene(new Group(canvas)));
      stage.setScene(new Scene(stackPane));
      //stage.show();  // move to setVisible(true)?
    }
  }


  //public Frame initFrame(PApplet sketch, java.awt.Color backgroundColor,
  public void initFrame(PApplet sketch, int backgroundColor,
                         int deviceIndex, boolean fullScreen,
                         boolean spanDisplays) {
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
    setSize(sketch.sketchWidth(), sketch.sketchHeight());
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


  public void setSize(int width, int height) {
    //System.out.format("%s.setSize(%d, %d)%n", getClass().getSimpleName(), width, height);
    stage.setWidth(width);
    stage.setHeight(height);
    fx.setSize(width, height);
  }


//  public Component getComponent() {
//    return null;
//  }


  public void setSmooth(int level) {
    // TODO Auto-generated method stub

  }


  public void setFrameRate(float fps) {
    // TODO Auto-generated method stub

  }


  @Override
  public void requestFocus() {
    // TODO Auto-generated method stub

  }


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
    if (timer == null) {
      timer = new AnimationTimer() {

        @Override
        public void handle(long now) {
          //System.out.println("handle(" + now + ") calling handleDraw()");
          sketch.handleDraw();
        }
      };
      timer.start();
    }
  }


  public void pauseThread() {
    // TODO Auto-generated method stub

  }

  public void resumeThread() {
    // TODO Auto-generated method stub

  }

  public boolean stopThread() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isStopped() {
    // TODO Auto-generated method stub
    return false;
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


  protected void fxMouseEvent(javafx.scene.input.MouseEvent nativeEvent) {
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

    //if (canvas.getBoundsInLocal().contains(me.getX(), me.getY())) {
    //System.out.println(me.getSceneX() + " " + me.getSceneY());
    //}

    sketch.postEvent(new MouseEvent(nativeEvent, nativeEvent.getWhen(),
                                    peAction, peModifiers,
                                    nativeEvent.getX(), nativeEvent.getY(),
                                    peButton,
                                    peCount));
  }


  protected void fxKeyEvent(javafx.scene.input.KeyEvent event) {
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
}