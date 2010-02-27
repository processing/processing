package processing.app.tools.android;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class Harness implements AndroidEnvironmentProperties,
    AndroidDeviceProperties {
  public static void main(final String[] args) throws Exception {
    System.err.println("Waiting for device to be plugged in.");
    final AndroidDevice device = AndroidEnvironment.getInstance().getHardware()
        .get();
    System.err.println("Got it.");
    final JFrame f = new JFrame("Harness");
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.add(new JLabel("Close me to quit"));
    f.pack();
    f.setVisible(true);
  }

}
