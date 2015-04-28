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
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;


public class PSurfaceDanger extends PSurfaceAWT {

  public PSurfaceDanger(PGraphics graphics) {
    super(graphics);
  }


  @Override
  public Thread createThread() {
    return new AnimationThread() {
      @Override
      public void render() {
        //sketch.handleDraw();
        blit();
      }
    };
  }


  @Override
  protected synchronized void render() {
    if (!canvas.isDisplayable()) {
      return;
    }

    Canvas c = (Canvas) canvas;
    if (c.getBufferStrategy() == null) {  // whole block [121222]
      c.createBufferStrategy(2);
    }
    BufferStrategy strategy = c.getBufferStrategy();
    if (strategy == null) {
      return;
    }
    do {
      do {
        Graphics2D draw = (Graphics2D) strategy.getDrawGraphics();

        // draw the Java2D feller here
        ((PGraphicsJava2D) sketch.g).g2 = draw;
        sketch.handleDraw();

        draw.dispose();

      } while (strategy.contentsRestored());

      strategy.show();

    } while (strategy.contentsLost());
  }
}