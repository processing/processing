package processing.app.tools.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AndroidProcesses {
  private final List<AndroidProcess> processes = new ArrayList<AndroidProcess>();
  private final AndroidDevice device;

  public AndroidProcesses(final AndroidDevice device) {
    this.device = device;
  }

  void refresh() {
    processes.clear();
    try {
      for (final String line : new ProcessHelper(this.device
          .generateAdbCommand("shell", "ps")).execute()) {
        final String[] fields = line.split("\\s+");
        if (fields.length != 9 || fields[0].equals("USER")) {
          continue;
        }
        final String pid = fields[1];
        final String name = fields[8];
        processes.add(new AndroidProcess(pid, name));
      }
    } catch (final Exception e) {
      System.err.println("Unable to refresh processes on " + device + ":");
      e.printStackTrace(System.err);
    }
  }

  List<AndroidProcess> getProcesses() {
    return Collections.unmodifiableList(processes);
  }

  public AndroidProcess byPid(final String pid) {
    for (final AndroidProcess p : processes) {
      if (p.pid.equals(pid)) {
        return p;
      }
    }
    return null;
  }

  public AndroidProcess byName(final String name) {
    for (final AndroidProcess p : processes) {
      if (p.name.equals(name)) {
        return p;
      }
    }
    return null;
  }

  public void add(final AndroidProcess androidProcess) {
    processes.add(androidProcess);
  }
}
