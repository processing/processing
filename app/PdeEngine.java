public class PdeEngine {
  PdeEditor editor;

  public PdeEngine(PdeEditor editor) {
    this.editor = editor;
  }

  // implemented by subclasses

  public void start() throws PdeException {
  }

  public void front() {
  }

  public void stop() {
  }

  public void close() {
  }
}
