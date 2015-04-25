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