package processing.app.tools.android;

import java.util.List;

public interface DeviceListener {
  void stacktrace(final List<String> trace);

  void sketchStopped();
}
