package processing.app.tools.android;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * <p>An AndroidEnvironment is an object that periodically polls for the existence
 * of running Android devices, both emulated and hardware. You can register to
 * be notified when devices are added to or removed from the environment. You
 * can also ask for an emulator or a hardware device specifically.
 * 
 * <pre> AndroidEnvironment env = new AndroidEnvironment();
 * env.addPropertyChangeListener(...);
 * env.initialize();
 * 
 * AndroidDevice n1 = env.getHardware();</pre>
 * 
 * @author Jonathan Feinberg &lt;jdf@pobox.com&gt;
 *
 */
public class AndroidEnvironment implements AndroidEnvironmentProperties {
  private final Map<String, AndroidDevice> devices = new ConcurrentHashMap<String, AndroidDevice>();
  private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
  private final Timer refreshTimer = new Timer("AndroidEnvironment refresher");

  public AndroidEnvironment() {
  }

  /**
   * Cause this model of the Android environment to begin discovering
   * devices. If you wish to be informed about device discovery, make
   * sure you register yourself as a listener before calling this
   * method.
   * 
   * This method starts up a thread that must be nuked via the 
   * shutdown() method in order for your app to exit cleanly.
   */
  public void initialize() {
    for (final String deviceId : listDevices()) {
      addDevice(new AndroidDevice(this, deviceId));
    }
    refreshTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        refresh();
      }
    }, TimeUnit.SECONDS.toMillis(2), TimeUnit.SECONDS.toMillis(2));
  }

  /**
   * Turn off the environment's device-discovery polling.
   */
  public void shutdown() {
    refreshTimer.cancel();
  }

  /**
   * @return the first Android emulator known to be running, or null if there are none.
   */
  public AndroidDevice getEmulator() {
    for (final AndroidDevice device : devices.values()) {
      if (device.getId().contains("emulator")) {
        return device;
      }
    }
    return null;
  }

  /**
   * @return the first Android hardware device known to be running, or null if there are none.
   */
  public AndroidDevice getHardware() {
    for (final AndroidDevice device : devices.values()) {
      if (!device.getId().contains("emulator")) {
        return device;
      }
    }
    return null;
  }

  private void refresh() {
    final List<String> activeDevices = listDevices();
    for (final String deviceId : activeDevices) {
      if (!devices.containsKey(deviceId)) {
        addDevice(new AndroidDevice(this, deviceId));
      }
    }
    for (final String deviceId : new ArrayList<String>(devices.keySet())) {
      if (!activeDevices.contains(deviceId)) {
        devices.get(deviceId).shutdown();
      }
    }
  }

  private void addDevice(final AndroidDevice device) {
    if (devices.put(device.getId(), device) != null) {
      throw new IllegalStateException("Adding " + device
          + ", which already exists!");
    }
    try {
      device.initialize();
    } catch (final Exception e) {
      System.err.println("Cannot initialize " + device + ": " + e);
      devices.remove(device.getId());
    }
    firePropertyChange(DEVICE_ADDED, null, device);
  }

  void deviceRemoved(final AndroidDevice device) {
    if (devices.remove(device.getId()) == null) {
      throw new IllegalStateException("I didn't know about device "
          + device.getId() + "!");
    }
    firePropertyChange(DEVICE_REMOVED, device, null);
  }

  public void addPropertyChangeListener(final PropertyChangeListener listener) {
    pcs.addPropertyChangeListener(listener);
  }

  public void firePropertyChange(final String propertyName,
                                 final Object oldValue, final Object newValue) {
    pcs.firePropertyChange(propertyName, oldValue, newValue);
  }

  public void removePropertyChangeListener(final PropertyChangeListener listener) {
    pcs.removePropertyChangeListener(listener);
  }

  static final String ADB_DEVICES_ERROR = "Received unfamiliar output from “adb devices”.\n"
      + "The device list may have errors.";

  /**
   *    <p>First line starts "List of devices"

        <p>When an emulator is started with a debug port, then it shows up
        in the list of devices.

        <p>List of devices attached
        <br>HT91MLC00031 device
        <br>emulator-5554 offline

        <p>List of devices attached
        <br>HT91MLC00031 device
        <br>emulator-5554 device

   * @return list of device identifiers
   * @throws IOException
   */
  private static List<String> listDevices() {
    ProcessResult result;
    try {
      result = new ProcessHelper("adb", "devices").execute();
    } catch (final InterruptedException e) {
      return Collections.emptyList();
    } catch (final IOException e) {
      System.err.println(e);
      return Collections.emptyList();
    }
    if (!result.succeeded()) {
      System.err.println(result);
      return Collections.emptyList();
    }

    // might read "List of devices attached"
    if (!result.getStdout().startsWith("List of devices")) {
      System.err.println(ADB_DEVICES_ERROR);
      return Collections.emptyList();
    }

    final List<String> devices = new ArrayList<String>();
    for (final String line : result) {
      if (!line.contains("\t")) {
        continue;
      }
      devices.add(line.split("\t")[0]);
    }
    return devices;
  }
}
