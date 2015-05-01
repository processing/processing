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

//import java.awt.Component;
//import java.awt.Frame;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;


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

  //Application app;
  //Stage stage;
  PGraphicsFX fx;
  Stage stage;
  Canvas canvas;
//  Frame dummy;

  AnimationTimer timer;


  public PSurfaceFX(PGraphicsFX graphics) {
    fx = graphics;
    canvas = new ResizableCanvas();
    fx.context = canvas.getGraphicsContext2D();
  }


  class ResizableCanvas extends Canvas {
//    public ResizableCanvas(int width, int height) {
//      super(width, height);
    public ResizableCanvas() {

      // Redraw canvas when size changes.
      //widthProperty().addListener(evt -> draw());
      //heightProperty().addListener(evt -> draw());
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

//    private void draw() {
//      double width = getWidth();
//      double height = getHeight();
//
//      GraphicsContext gc = getGraphicsContext2D();
//      gc.clearRect(0, 0, width, height);
//
//      gc.setStroke(javafx.scene.paint.Color.RED);
//      gc.strokeLine(0, 0, width, height);
//      gc.strokeLine(0, height, width, 0);
//
//      sketch.handleDraw();
//    }

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

    public PApplicationFX() { }

    @Override
    public void start(final Stage stage) {
      surface.stage = stage;

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
    stage.setTitle(title);
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


  public void placeWindow(int[] location) {
    // TODO Auto-generated method stub
  }


  public void placeWindow(int[] location, int[] editorLocation) {
    // TODO Auto-generated method stub

  }


  // http://download.java.net/jdk8/jfxdocs/javafx/stage/Stage.html#setFullScreenExitHint-java.lang.String-
  // http://download.java.net/jdk8/jfxdocs/javafx/stage/Stage.html#setFullScreenExitKeyCombination-javafx.scene.input.KeyCombination-
  public void placePresent(int stopColor) {
    // TODO Auto-generated method stub
  }


  public void setupExternalMessages() {
    // TODO Auto-generated method stub
  }


  public void setSize(int width, int height) {
    System.out.println(getClass().getSimpleName() + " setSize()");
    stage.setWidth(width);
    stage.setHeight(height);
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