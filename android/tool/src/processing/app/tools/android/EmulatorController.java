package processing.app.tools.android;

import java.io.IOException;
import processing.app.Preferences;

class EmulatorController {
  static void launch() throws IOException {
    String portString = Preferences.get("android.emulator.port");
    if (portString == null) {
      portString = "5566";
      Preferences.set("android.emulator.port", portString);
    }

    // # starts and uses port 5554 for communication (ut not logs)
    // emulator -avd gee1 -port 5554
    // # only informative messages and up (emulator -help-logcat for more info)
    // emulator -avd gee1 -logcat '*:i'
    // # faster boot
    // emulator -avd gee1 -logcat '*:i' -no-boot-anim
    // # only get System.out and System.err
    // emulator -avd gee1 -logcat 'System.*:i' -no-boot-anim
    // # though lots of messages aren't through System.*, so that's not great
    // # need to instead use the adb interface

    // launch emulator because it's not running yet
    final String[] cmd = new String[] {
      "emulator", "-avd", AVD.ECLAIR.name, "-port", portString, "-no-boot-anim" };
    System.err.println("Launching emulator");
    final Process p = Runtime.getRuntime().exec(cmd);
    // "emulator: ERROR: the user data image is used by another emulator. aborting"
    // make sure that the streams are drained properly
    new StreamPump(p.getInputStream()).addTarget(System.out).start();
    new StreamPump(p.getErrorStream()).addTarget(System.err).start();
    new Thread(new Runnable() {
      public void run() {
        try {
          System.err.println("Starting to wait for emulator process.");
          final int result = p.waitFor();
          System.err.println("Emulator process exited with " + result);
        } catch (final InterruptedException e) {
          System.err.println("Emulator interrupted.");
        }
      }
    });
  }
}
