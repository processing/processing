package processing.video;

import java.lang.reflect.Method;

public class ReflectionCaptureHandler implements CaptureHandler {

  private final Object instance;
  private final Method handler;
  private boolean ok = true;

  public ReflectionCaptureHandler(Object instance, Method handler) {
    this.instance = instance;
    this.handler = handler;
  }

  @Override
  public void handleCapture(Capture capture) {
    if (!ok) {
      return;
    }
    try {
      handler.invoke(instance, capture);
    } catch (Exception e) {
      System.err.println("error, disabling captureEvent() for capture object");
      e.printStackTrace();
      ok = false;
    }
  }

}
