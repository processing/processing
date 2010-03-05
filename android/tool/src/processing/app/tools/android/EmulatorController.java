package processing.app.tools.android;

import java.io.IOException;
import processing.app.Preferences;

class EmulatorController {
  public static enum State {
    Idle, Launched, Running
  }

  public static EmulatorController getInstance() {
    return INSTANCE;
  }

  private static final EmulatorController INSTANCE = new EmulatorController();

  private volatile State state = State.Idle;

  public State getState() {
    return state;
  }

  private void setState(final State state) {
    this.state = state;
    System.err.println("Emulator state: " + state);
  }

  synchronized public void launch() throws IOException {
    if (state != State.Idle) {
      throw new IllegalStateException(
                                      "You can't launch an emulator whose state is "
                                          + state);
    }

    String portString = Preferences.get("android.emulator.port");
    if (portString == null) {
      portString = "5566";
      Preferences.set("android.emulator.port", portString);
    }

    System.err.println("Launching emulator");

    // See http://developer.android.com/guide/developing/tools/emulator.html
    final Process p = Runtime.getRuntime().exec(
      new String[] {
        "emulator", "-avd", AVD.ECLAIR.name, "-port", portString,
        "-no-boot-anim" });

    // if we've gotten this far, then we've at least succeeded in finding and
    // beginning execution of the emulator, so we are now officially "Launching"
    setState(State.Launched);

    ProcessRegistry.watch(p);
    // "emulator: ERROR: the user data image is used by another emulator. aborting"
    // make sure that the streams are drained properly
    new StreamPump(p.getInputStream()).addTarget(System.out).start();
    new StreamPump(p.getErrorStream()).addTarget(System.err).start();
    new Thread(new Runnable() {
      public void run() {
        try {
          if (AndroidEnvironment.getInstance().getEmulator().get() != null) {
            setState(State.Running);
          }
        } catch (final Exception e) {
          System.err.println("While waiting for emulator to launch " + e);
        }
      }
    }).start();
    new Thread(new Runnable() {
      public void run() {
        try {
          try {
            final int result = p.waitFor();
            System.err
                .println("Emulator process exited "
                    + ((result == 0) ? "normally" : " with status " + result)
                    + ".");
          } catch (final InterruptedException e) {
            System.err.println("Emulator was interrupted.");
          } finally {
            p.destroy();
            ProcessRegistry.unwatch(p);
          }
        } finally {
          setState(State.Idle);
        }
      }
    }, "Emulator Babysitter").start();
  }
}
