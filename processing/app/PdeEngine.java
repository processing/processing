import java.awt.*;


public class PdeEngine {
  PdeEditor editor;
  Window window;

  public PdeEngine(PdeEditor editor) {
    this.editor = editor;
  }

  // implemented by subclasses

  public void start(Point windowLocation) throws PdeException {
  }

  //public void front() {
  //}

  public void stop() {
  }

  public void close() {
  }
}
