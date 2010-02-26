package processing.app.tools.android;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import processing.app.tools.android.LogEntry.Severity;

public class AndroidDevice implements AndroidDeviceProperties {
  private final AndroidEnvironment env;
  private final String id;
  private final AndroidProcesses processes;
  private final Map<String, List<ProcessOutputListener>> outputListeners = new HashMap<String, List<ProcessOutputListener>>();

  private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  // mutable state
  private Process logcat;

  public AndroidDevice(final AndroidEnvironment env, final String id) {
    this.env = env;
    this.id = id;
    this.processes = new AndroidProcesses(this);
  }

  //  adb shell am start -n com.android.launcher2/.Launcher

  private class LogLineProcessor implements LineProcessor {
    public void processLine(final String line) {
      final LogEntry entry = new LogEntry(line);

      final String src = entry.source;
      final String msg = entry.message;
      final Severity sev = entry.severity;
      if (src.equals("ActivityManager") && msg.startsWith("Start proc")) {
        handleStartProcEntry(entry);
      } else {
        if ((src.equals("AndroidRuntime") && sev == Severity.Error)
            || src.equals("System.out") || src.equals("System.err")) {
          final List<ProcessOutputListener> listeners = getListeners(entry.sourcePid);
          if (listeners != null) {
            for (final ProcessOutputListener listener : listeners) {
              if (sev == Severity.Warning || sev == Severity.Error
                  || sev == Severity.Fatal) {
                listener.handleStderr(msg);
              } else {
                listener.handleStdout(msg);
              }
            }
          }
        }
      }
      //System.err.println(entry.source + "/" + entry.message);
    }
  }

  public void initialize() throws IOException, InterruptedException {
    processes.refresh();
    new ProcessHelper(generateAdbCommand("logcat", "-c")).execute();
    logcat = Runtime.getRuntime().exec(generateAdbCommand("logcat"));
    new StreamPump(logcat.getInputStream()).addTarget(new LogLineProcessor())
        .start();
    new StreamPump(logcat.getErrorStream()).addTarget(System.err).start();
  }

  public void shutdown() {
    for (final PropertyChangeListener pcl : Arrays.asList(pcs
        .getPropertyChangeListeners())) {
      pcs.removePropertyChangeListener(pcl);
    }
    outputListeners.clear();
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

  public List<AndroidProcess> ps() {
    return processes.getProcesses();
  }

  public void addOutputListener(final String processName,
                                final ProcessOutputListener listener) {
    if (!outputListeners.containsKey(processName)) {
      outputListeners.put(processName, new ArrayList<ProcessOutputListener>());
    }
    outputListeners.get(processName).add(listener);
  }

  public void removeOutputListener(final String processName,
                                   final ProcessOutputListener listener) {
    final List<ProcessOutputListener> listeners = outputListeners
        .get(processName);
    if (listeners != null) {
      listeners.remove(listener);
    }
  }

  private static final Pattern START_PROC = Pattern
      .compile("^Start proc (\\S+) for \\S+ [^:]+: pid=(\\d+).+$");

  protected List<ProcessOutputListener> getListeners(final String pid) {
    final AndroidProcess process = processes.byPid(pid);
    if (process == null) {
      System.err.println("I don't recognize process id " + pid + "!");
      return null;
    }
    return outputListeners.get(process.name);
  }

  private void handleStartProcEntry(final LogEntry entry) {
    final Matcher m = START_PROC.matcher(entry.message);
    if (m.matches()) {
      startProc(m.group(1), m.group(2));
    } else {
      System.err.println("I don't recognize this start proc message:\n"
          + entry.message);
    }
  }

  private void startProc(final String name, final String pid) {
    final AndroidProcess proc = new AndroidProcess(pid, name);
    processes.refresh();
    firePropertyChange(APP_STARTED, null, proc);
  }

  private void endProc(final String pid) {
    final AndroidProcess proc = processes.byPid(pid);
    if (proc == null) {
      System.err.println("Process " + pid
          + " ended, but I hadn't known about it.");
    } else {
      processes.refresh();
      firePropertyChange(APP_ENDED, proc, null);
    }
  }

  String[] generateAdbCommand(final String... cmd) {
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

  public void addPropertyChangeListener(final PropertyChangeListener listener) {
    pcs.addPropertyChangeListener(listener);
  }

  private void firePropertyChange(final String propertyName,
                                  final Object oldValue, final Object newValue) {
    pcs.firePropertyChange(propertyName, oldValue, newValue);
  }

  public void removePropertyChangeListener(final PropertyChangeListener listener) {
    pcs.removePropertyChangeListener(listener);
  }

}
