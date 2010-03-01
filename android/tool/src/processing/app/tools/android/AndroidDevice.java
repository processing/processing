package processing.app.tools.android;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import processing.app.debug.RunnerListener;
import processing.app.tools.android.LogEntry.Severity;

class AndroidDevice implements AndroidDeviceProperties {
  private final AndroidEnvironment env;
  private final String id;
  private final Set<Integer> activeProcesses = new HashSet<Integer>();

  // mutable state
  private Process logcat;

  public AndroidDevice(final AndroidEnvironment env, final String id) {
    this.env = env;
    this.id = id;
  }

  public void bringLauncherToFront() {
    try {
      new ProcessHelper(generateAdbCommand("shell", "am", "start", "-n",
        "com.android.launcher2/.Launcher")).execute();
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
    bringLauncherToFront();
    try {
      final ProcessHelper p = new ProcessHelper(generateAdbCommand("install",
        "-r", // safe to always use -r switch
        apkPath));

      final ProcessResult installResult = p.execute();
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

  // better version that actually runs through JDI:
  // http://asantoso.wordpress.com/2009/09/26/using-jdb-with-adb-to-debugging-of-android-app-on-a-real-device/
  public boolean launchApp(final String packageName, final String className)
      throws IOException, InterruptedException {
    final ProcessHelper startSketch = new ProcessHelper(generateAdbCommand(
      "shell", "am", "start", "-e", "debug", "true", "-a",
      "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER",
      "-n", packageName + "/." + className));
    return startSketch.execute().succeeded();
  }

  public boolean isEmulator() {
    return id.startsWith("emulator");
  }

  // I/Process ( 9213): Sending signal. PID: 9213 SIG: 9
  private static final Pattern SIG = Pattern
      .compile("PID:\\s+(\\d+)\\s+SIG:\\s+(\\d+)");

  private class LogLineProcessor implements LineProcessor {
    public void processLine(final String line) {
      final LogEntry entry = new LogEntry(line);
      final String src = entry.source;
      final String msg = entry.message;
      final Severity sev = entry.severity;
      if (msg.startsWith("PROCESSING")) {
        if (msg.contains("onStart")) {
          startProc(src, entry.sourcePid);
        } else if (msg.contains("onStop")) {
          endProc(entry.sourcePid);
        }
      } else if (src.equals("Process")) {
        final Matcher m = SIG.matcher(msg);
        if (m.find()) {
          final int pid = Integer.parseInt(m.group(1));
          final int signal = Integer.parseInt(m.group(2));
          if (signal == 9) {
            endProc(pid);
          }
        }
      } else if (activeProcesses.contains(entry.sourcePid)
          && ((src.equals("AndroidRuntime") && sev == Severity.Error)
              || src.equals("System.out") || src.equals("System.err"))) {
        if (sev.useErrorStream) {
          System.err.println(msg);
        } else {
          System.out.println(msg);
        }
      }
      //System.err.println(entry.source + "/" + entry.message);
    }
  }

  public void initialize() throws IOException, InterruptedException {
    new ProcessHelper(generateAdbCommand("logcat", "-c")).execute();
    logcat = Runtime.getRuntime().exec(generateAdbCommand("logcat"));
    new StreamPump(logcat.getInputStream()).addTarget(new LogLineProcessor())
        .start();
    new StreamPump(logcat.getErrorStream()).addTarget(System.err).start();
  }

  public void shutdown() {
    if (logcat != null) {
      logcat.destroy();
    }
    env.deviceRemoved(this);
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
  }

  private String[] generateAdbCommand(final String... cmd) {
    final String[] adbCmd = new String[3 + cmd.length];
    adbCmd[0] = "adb";
    adbCmd[1] = "-s";
    adbCmd[2] = getId();
    System.arraycopy(cmd, 0, adbCmd, 3, cmd.length);
    return adbCmd;
  }

  @Override
  public String toString() {
    return "[AndroidDevice " + getId() + "]";
  }

}
