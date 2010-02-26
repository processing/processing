package processing.app.tools.android;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class Harness implements AndroidEnvironmentProperties,
    AndroidDeviceProperties {
  public static void main(final String[] args) throws Exception {
    final AndroidEnvironment env = new AndroidEnvironment();
    env.addPropertyChangeListener(PCD);
    env.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(final PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(DEVICE_ADDED)) {
          final AndroidDevice device = (AndroidDevice) evt.getNewValue();
          device.addOutputListener("processing.android.test.testconsole",
            new ProcessOutputListener() {
              public void handleStdout(final String line) {
                System.out.println("testconsole: " + line);
              }

              public void handleStderr(final String line) {
                System.err.println("testconsole: " + line);
              }
            });

          device.addOutputListener("processing.android.test.testmouse",
            new ProcessOutputListener() {
              public void handleStdout(final String line) {
                System.out.println("testmouse: " + line);
              }

              public void handleStderr(final String line) {
                System.err.println("testmouse: " + line);
              }
            });
        }
      }
    });

    env.initialize();
  }

  private static final PropertyChangeListener PCD = new PropertyChangeListener() {
    public void propertyChange(final PropertyChangeEvent evt) {
      System.err.println(evt.getSource());
      System.err.println("    " + evt.getPropertyName());
      System.err.println("    old: " + evt.getOldValue());
      System.err.println("    new: " + evt.getNewValue());
      System.err.println();
    }
  };
}
