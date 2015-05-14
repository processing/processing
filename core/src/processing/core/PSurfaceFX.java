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

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


//class PApplicationFX extends Application {
//  static public PSurfaceFX surface;
//
//  public PApplicationFX() { }
//
//  @Override
//  public void start(final Stage stage) {
//    surface.stage = stage;
//
//    Canvas canvas = surface.canvas;
//    StackPane stackPane = new StackPane();
//    stackPane.getChildren().add(canvas);
//    canvas.widthProperty().bind(stackPane.widthProperty());
//    canvas.heightProperty().bind(stackPane.heightProperty());
//
//    //stage.setScene(new Scene(new Group(canvas)));
//    stage.setScene(new Scene(stackPane));
//    //stage.show();  // move to setVisible(true)?
//  }
//}


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
    static String title;  // title set at launch

    public PApplicationFX() { }

    @Override
    public void start(final Stage stage) {
      surface.stage = stage;
      if (title != null) {
        stage.setTitle(title);
      }

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
    //return frame;
  }


  /** Set the window (and dock, or whatever necessary) title. */
  public void setTitle(String title) {
    PApplicationFX.title = title;  // store this in case the stage still null
    if (stage != null) {
      stage.setTitle(title);
    }
  }


  /** Show or hide the window. */
  public void setVisible(boolean visible) {
    stage.show();
  }


  /** Set true if we want to resize things (default is not resizable) */
  public void setResizable(boolean resizable) {
    stage.setResizable(resizable);
  }


//  class DummyFrame extends Frame {
//
//    public DummyFrame() {
//      super();
//    }
//
//    @Override
//    public void setResizable(boolean resizable) {
//      // TODO
//    }
//
//    @Override
//    public void setVisible(boolean visible) {
//      stage.show();
//    }
//
//    @Override
//    public void setTitle(String title) {
//      if (stage != null) {
//        stage.setTitle(title);
//      } else {
//        System.err.println("stage was null for setTitle()");
//      }
//    }
//  }


  /*
  public class PApplicationFX extends Application {

    public PApplicationFX() {
      super();
    }

    @Override
    public void start(final Stage stage) {
      PSurfaceFX.this.stage = stage;

      StackPane stackPane = new StackPane();
      stackPane.getChildren().add(canvas);
      canvas.widthProperty().bind(stackPane.widthProperty());
      canvas.heightProperty().bind(stackPane.heightProperty());

      //stage.setScene(new Scene(new Group(canvas)));
      stage.setScene(new Scene(stackPane));
      //stage.show();  // move to setVisible(true)?
    }
  }
  */


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

    // TODO this is much too late... why even create the enormous frame for PDF?
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
}