package processing.app.tools.android;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import processing.app.Preferences;
import processing.app.exec.ProcessRegistry;
import processing.app.exec.StreamPump;
import processing.core.PApplet;

class EmulatorController {
  public static enum State {
    NOT_RUNNING, WAITING_FOR_BOOT, RUNNING
  }

  public static EmulatorController getInstance() {
    return INSTANCE;
  }

  private static final EmulatorController INSTANCE = new EmulatorController();

  private volatile State state = State.NOT_RUNNING;

  public State getState() {
    return state;
  }

  private void setState(final State state) {
    this.state = state;
    //    System.err.println("Emulator state: " + state);
  }

  /**
   * Blocks until emulator is running, or some catastrophe happens.
   * @throws IOException
   */
  synchronized public void launch() throws IOException {
    if (state != State.NOT_RUNNING) {
      throw new IllegalStateException(
                                      "You can't launch an emulator whose state is "
                                          + state);
    }

    String portString = Preferences.get("android.emulator.port");
    if (portString == null) {
      portString = "5566";
      Preferences.set("android.emulator.port", portString);
    }

    //    System.err.println("EmulatorController: Launching emulator");

    // See http://developer.android.com/guide/developing/tools/emulator.html
    final String[] cmd = new String[] {
      "emulator", "-avd", AVD.ECLAIR.name, "-port", portString, "-no-boot-anim" };
    final Process p = Runtime.getRuntime().exec(cmd);
    ProcessRegistry.watch(p);

    // if we've gotten this far, then we've at least succeeded in finding and
    // beginning execution of the emulator, so we are now officially "Launched"
    setState(State.WAITING_FOR_BOOT);

    final String title = PApplet.join(cmd, ' ');
    new StreamPump(p.getInputStream(), "out: " + title).addTarget(System.out)
        .start();
    new StreamPump(p.getErrorStream(), "err: " + title).addTarget(System.err)
        .start();
    final CountDownLatch latch = new CountDownLatch(1);
    new Thread(new Runnable() {
      public void run() {
        try {
          //          System.err.println("EmulatorController: Waiting for boot.");
          while (state == State.WAITING_FOR_BOOT) {
            Thread.sleep(2000);
            for (final String device : AndroidEnvironment.listDevices()) {
              if (device.contains("emulator")) {
                //                System.err.println("EmulatorController: Emulator booted.");
                setState(State.RUNNING);
                return;
              }
            }
          }
          System.err.println("EmulatorController: Emulator never booted.");
        } catch (final Exception e) {
          System.err.println("While waiting for emulator to boot " + e);
          p.destroy();
        } finally {
          latch.countDown();
        }
      }
    }, "EmulatorController: Wait for emulator to boot").start();
    new Thread(new Runnable() {
      public void run() {
        try {
          try {
            p.waitFor();
            //            final int result = p.waitFor();
            //            System.err
            //                .println("Emulator process exited "
            //                    + ((result == 0) ? "normally" : " with status " + result)
            //                    + ".");
          } catch (final InterruptedException e) {
            System.err.println("Emulator was interrupted.");
          } finally {
            p.destroy();
            ProcessRegistry.unwatch(p);
          }
        } finally {
          setState(State.NOT_RUNNING);
        }
      }
    }, "EmulatorController: Process manager").start();
    try {
      latch.await();
    } catch (final InterruptedException drop) {
      System.err.println("Interrupted while waiting for emulator to launch.");
    }
  }
}
