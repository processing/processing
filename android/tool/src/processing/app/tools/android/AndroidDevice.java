package processing.app.tools.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import processing.app.debug.RunnerListener;
import processing.app.exec.LineProcessor;
import processing.app.exec.ProcessRegistry;
import processing.app.exec.ProcessResult;
import processing.app.exec.StreamPump;
import processing.app.tools.android.LogEntry.Severity;
import processing.core.PApplet;


class AndroidDevice implements AndroidDeviceProperties {
  private final AndroidEnvironment env;
  private final String id;
  private final Set<Integer> activeProcesses = new HashSet<Integer>();
  private final Set<DeviceListener> listeners = 
    Collections.synchronizedSet(new HashSet<DeviceListener>());

  // mutable state
  private Process logcat;

  public AndroidDevice(final AndroidEnvironment env, final String id) {
    this.env = env;
    this.id = id;
  }

  public void bringLauncherToFront() {
    try {
      adb("shell", "am", "start", "-a", "android.intent.action.MAIN", "-c",
        "android.intent.category.HOME");
    } catch (final Exception e) {
      e.printStackTrace(System.err);
    }
  }

  // adb -s emulator-5556 install helloWorld.apk

  // : adb -s HT91MLC00031 install bin/Brightness-debug.apk
  // 532 KB/s (190588 bytes in 0.349s)
  // pkg: /data/local/tmp/Brightness-debug.apk
  // Failure [INSTALL_FAILED_ALREADY_EXISTS]

  // : adb -s HT91MLC00031 install -r bin/Brightness-debug.apk
  // 1151 KB/s (190588 bytes in 0.161s)
  // pkg: /data/local/tmp/Brightness-debug.apk
  // Success

  // safe to just always include the -r (reinstall) flag
  public boolean installApp(final String apkPath, final RunnerListener status) {
    if (!isAlive()) {
      return false;
    }
    bringLauncherToFront();
    try {
      final ProcessResult installResult = adb("install", "-r", apkPath);
      if (!installResult.succeeded()) {
        status.statusError("Could not install the sketch.");
        System.err.println(installResult);
        return false;
      }
      String errorMsg = null;
      for (final String line : installResult) {
        if (line.startsWith("Failure")) {
          errorMsg = line.substring(8);
          System.err.println(line);
        }
      }
      if (errorMsg == null) {
        status.statusNotice("Done installing.");
        return true;
      }
      status.statusError("Error while installing " + errorMsg);
    } catch (final IOException e) {
      status.statusError(e);
    } catch (final InterruptedException e) {
    }
    return false;
  }

  // different version that actually runs through JDI:
  // http://asantoso.wordpress.com/2009/09/26/using-jdb-with-adb-to-debugging-of-android-app-on-a-real-device/
  public boolean launchApp(final String packageName, final String className)
      throws IOException, InterruptedException {
    if (!isAlive()) {
      return false;
    }
    return adb("shell", "am", "start", "-e", "debug", "true", "-a",
      "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER",
      "-n", packageName + "/." + className).succeeded();
  }

  public boolean isEmulator() {
    return id.startsWith("emulator");
  }

  // I/Process ( 9213): Sending signal. PID: 9213 SIG: 9
  private static final Pattern SIG = Pattern
      .compile("PID:\\s+(\\d+)\\s+SIG:\\s+(\\d+)");

  private final List<String> stackTrace = new ArrayList<String>();

  private class LogLineProcessor implements LineProcessor {
    public void processLine(final String line) {
      final LogEntry entry = new LogEntry(line);
      if (entry.message.startsWith("PROCESSING")) {
        if (entry.message.contains("onStart")) {
          startProc(entry.source, entry.pid);
        } else if (entry.message.contains("onStop")) {
          endProc(entry.pid);
        }
      } else if (entry.source.equals("Process")) {
        handleCrash(entry);
      } else if (activeProcesses.contains(entry.pid)) {
        handleConsole(entry);
      }
    }

    private void handleCrash(final LogEntry entry) {
      final Matcher m = SIG.matcher(entry.message);
      if (m.find()) {
        final int pid = Integer.parseInt(m.group(1));
        final int signal = Integer.parseInt(m.group(2));
        if (activeProcesses.contains(pid)) { // only report crashes of *our* sketches, por favor
          /*
           * A crashed sketch first gets a signal 3, which causes the
           * "you've crashed" dialog to appear on the device. After
           * the user dismisses the dialog, a sig 9 is sent.
           * TODO: is it possible to forcibly dismiss the crash dialog?
           */
          if (signal == 3) {
            endProc(pid);
            reportStackTrace(entry);
          }
        }
      }
    }

    private void handleConsole(final LogEntry entry) {
      final boolean isStackTrace = entry.source.equals("AndroidRuntime")
          && entry.severity == Severity.Error;
      if (isStackTrace) {
        if (!entry.message.startsWith("Uncaught handler")) {
          stackTrace.add(entry.message);
          System.err.println(entry.message);
        }
      } else if (entry.source.equals("System.out")
          || entry.source.equals("System.err")) {
        if (entry.severity.useErrorStream) {
          System.err.println(entry.message);
        } else {
          System.out.println(entry.message);
        }
      }
    }
  }

  private void reportStackTrace(final LogEntry entry) {
    if (stackTrace.isEmpty()) {
      System.err.println("That's weird. Proc " + entry.pid
          + " got signal 3, but there's no stack trace.");
    }
    final List<String> stackCopy = Collections
        .unmodifiableList(new ArrayList<String>(stackTrace));
    for (final DeviceListener listener : listeners) {
      listener.stacktrace(stackCopy);
    }
    stackTrace.clear();
  }

  void initialize() throws IOException, InterruptedException {
    adb("logcat", "-c");
    final String[] cmd = generateAdbCommand("logcat");
    final String title = PApplet.join(cmd, ' ');
    logcat = Runtime.getRuntime().exec(cmd);
    ProcessRegistry.watch(logcat);
    new StreamPump(logcat.getInputStream(), "log: " + title).addTarget(
      new LogLineProcessor()).start();
    new StreamPump(logcat.getErrorStream(), "err: " + title).addTarget(
      System.err).start();
    new Thread(new Runnable() {
      public void run() {
        try {
          logcat.waitFor();
          //          final int result = logcat.waitFor();
          //          System.err.println("AndroidDevice: " + getId() + " logcat exited "
          //              + (result == 0 ? "normally" : "with status " + result));
        } catch (final InterruptedException e) {
          System.err
              .println("AndroidDevice: logcat process monitor interrupted");
        } finally {
          shutdown();
        }
      }
    }, "AndroidDevice: logcat process monitor").start();
    //    System.err.println("Receiving log entries from " + id);
  }

  synchronized void shutdown() {
    if (!isAlive()) {
      return;
    }
    //    System.err.println(id + " is shutting down.");
    if (logcat != null) {
      logcat.destroy();
      logcat = null;
      ProcessRegistry.unwatch(logcat);
    }
    env.deviceRemoved(this);
    if (activeProcesses.size() > 0) {
      for (final DeviceListener listener : listeners) {
        listener.sketchStopped();
      }
    }
    listeners.clear();
  }

  synchronized boolean isAlive() {
    return logcat != null;
  }

  public String getId() {
    return id;
  }

  public AndroidEnvironment getEnv() {
    return env;
  }

  private void startProc(final String name, final int pid) {
    //    System.err.println("Process " + name + " started at pid " + pid);
    activeProcesses.add(pid);
  }

  private void endProc(final int pid) {
    //    System.err.println("Process " + pid + " stopped.");
    activeProcesses.remove(pid);
    for (final DeviceListener listener : listeners) {
      listener.sketchStopped();
    }
  }

  public void addListener(final DeviceListener listener) {
    listeners.add(listener);
  }

  public void removeListener(final DeviceListener listener) {
    listeners.remove(listener);
  }

  private ProcessResult adb(final String... cmd) throws InterruptedException,
      IOException {
    final String[] adbCmd = generateAdbCommand(cmd);
    return AndroidSDK.runADB(adbCmd);
  }

  private String[] generateAdbCommand(final String... cmd) {
    //    final String[] adbCmd = new String[3 + cmd.length];
    //    adbCmd[0] = "adb";
    //    adbCmd[1] = "-s";
    //    adbCmd[2] = getId();
    //    System.arraycopy(cmd, 0, adbCmd, 3, cmd.length);
    //    return adbCmd;
    return PApplet.concat(new String[] { "adb", "-s", getId() }, cmd);
  }

  @Override
  public String toString() {
    return "[AndroidDevice " + getId() + "]";
  }

}
