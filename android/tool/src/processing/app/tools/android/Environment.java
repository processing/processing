package processing.app.tools.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

//import processing.app.EditorConsole;
import processing.app.exec.ProcessResult;
import processing.app.tools.android.EmulatorController.State;

/**
 * <pre> AndroidEnvironment env = AndroidEnvironment.getInstance();
 * AndroidDevice n1 = env.getHardware();
 * AndroidDevice emu = env.getEmulator();</pre>
 * @author Jonathan Feinberg &lt;jdf@pobox.com&gt;
 *
 */
class Environment {
  private static final String ADB_DEVICES_ERROR =
    "Received unfamiliar output from “adb devices”.\n" +
    "The device list may have errors.";

  private static final Environment INSTANCE = new Environment();

  public static Environment getInstance() {
    return INSTANCE;
  }

  private final Map<String, Device> devices =
    new ConcurrentHashMap<String, Device>();
  private final ExecutorService deviceLaunchThread =
    Executors.newSingleThreadExecutor();


  public static void killAdbServer() {
    System.err.println("Shutting down any existing adb server...");
    System.err.flush();
    try {
      AndroidSDK.runADB("kill-server");
      //      System.err.println("OK.");
    } catch (final Exception e) {
      System.err.println("failed.");
      System.err.println();
      e.printStackTrace(System.err);
    }
  }


  private Environment() {
    if (processing.app.Base.DEBUG) {
      System.out.println("Starting up AndroidEnvironment");
    }
//    killAdbServer();
    Runtime.getRuntime().addShutdownHook(
      new Thread("AndroidEnvironment Shutdown") {
        @Override
        public void run() {
          shutdown();
        }
      });
  }


  private void shutdown() {
    System.out.println("Shutting down AndroidEnvironment");
    for (Device device : new ArrayList<Device>(devices.values())) {
      device.shutdown();
    }
    killAdbServer();
  }


  public Future<Device> getEmulator() {
    final Callable<Device> androidFinder = new Callable<Device>() {
      public Device call() throws Exception {
        return blockingGetEmulator();
      }
    };
    final FutureTask<Device> task =
      new FutureTask<Device>(androidFinder);
    deviceLaunchThread.execute(task);
    return task;
  }


  private final Device blockingGetEmulator() {
    Device emu = find(true);
    if (emu != null) {
      return emu;
    }

    final EmulatorController emuController = EmulatorController.getInstance();
    if (emuController.getState() == State.NOT_RUNNING) {
      try {
        emuController.launch(); // this blocks until emulator boots
      } catch (final IOException e) {
        e.printStackTrace(System.err);
        return null;
      }
    }
    while (!Thread.currentThread().isInterrupted()) {
      //      System.err.println("AndroidEnvironment: looking for emulator in loop.");
      //      System.err.println("AndroidEnvironment: emulatorcontroller state is "
      //          + emuController.getState());
      if (emuController.getState() == State.NOT_RUNNING) {
        System.err.println("Ouch. Emulator got killed, I think.");
        return null;
      }
      emu = find(true);
      if (emu != null) {
        //        System.err.println("AndroidEnvironment: returning " + emu.getId()
        //            + " from loop.");
        return emu;
      }
      try {
        Thread.sleep(2000);
      } catch (final InterruptedException e) {
        System.err.println("AndroidEnvironment: interrupted in loop.");
        return null;
      }
    }
    return null;
  }


  private Device find(final boolean wantEmulator) {
    refresh();
    synchronized (devices) {
      for (final Device device : devices.values()) {
        final boolean isEmulator = device.getId().contains("emulator");
        if ((isEmulator && wantEmulator) || (!isEmulator && !wantEmulator)) {
          return device;
        }
      }
    }
    return null;
  }


  /**
   * @return the first Android hardware device known to be running, or null if there are none.
   */
  public Future<Device> getHardware() {
    final Callable<Device> androidFinder = new Callable<Device>() {
      public Device call() throws Exception {
        return blockingGetHardware();
      }
    };
    final FutureTask<Device> task =
      new FutureTask<Device>(androidFinder);
    deviceLaunchThread.execute(task);
    return task;
  }


  private final Device blockingGetHardware() {
    Device hardware = find(false);
    if (hardware != null) {
      return hardware;
    }
    while (!Thread.currentThread().isInterrupted()) {
      try {
        Thread.sleep(2000);
      } catch (final InterruptedException e) {
        return null;
      }
      hardware = find(false);
      if (hardware != null) {
        return hardware;
      }
    }
    return null;
  }


  private void refresh() {
    final List<String> activeDevices = listDevices();
    for (final String deviceId : activeDevices) {
      if (!devices.containsKey(deviceId)) {
        addDevice(new Device(this, deviceId));
      }
    }
  }


  private void addDevice(final Device device) {
    //    System.err.println("AndroidEnvironment: adding " + device.getId());
    try {
      device.initialize();
      if (devices.put(device.getId(), device) != null) {
        throw new IllegalStateException("Adding " + device
            + ", which already exists!");
      }
    } catch (final Exception e) {
      System.err.println("While initializing " + device.getId() + ": " + e);
    }
  }


  void deviceRemoved(final Device device) {
    //    System.err.println("AndroidEnvironment: removing " + device.getId());
    if (devices.remove(device.getId()) == null) {
      throw new IllegalStateException("I didn't know about device "
          + device.getId() + "!");
    }
  }


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
  public static List<String> listDevices() {
    ProcessResult result;
    try {
//      System.out.println("listing devices 00");
      result = AndroidSDK.runADB("devices");
//      System.out.println("listing devices 05");
    } catch (InterruptedException e) {
      return Collections.emptyList();
    } catch (IOException e) {
      System.err.println("AndroidEnvironment.listDevices() did this to me");
      e.printStackTrace();
//      System.err.println(e);
//      System.err.println("checking devices");
//      e.printStackTrace(EditorConsole.systemErr);
      return Collections.emptyList();
    }
//    System.out.println("listing devices 10");
    if (!result.succeeded()) {
      if (result.getStderr().contains("protocol fault (no status)")) {
        System.err.println("bleh: " + result);  // THIS IS WORKING
      } else {
        System.err.println("nope: " + result);
      }
      return Collections.emptyList();
    }
//    System.out.println("listing devices 20");

    // might read "List of devices attached"
    final String stdout = result.getStdout();
    if (!(stdout.startsWith("List of devices") || stdout.trim().length() == 0)) {
      System.err.println(ADB_DEVICES_ERROR);
      return Collections.emptyList();
    }

//    System.out.println("listing devices 30");
    final List<String> devices = new ArrayList<String>();
    for (final String line : result) {
      if (!line.contains("\t")) {
        continue;
      }
      final String[] fields = line.split("\t");
      if (fields[1].equals("device")) {
        devices.add(fields[0]);
      }
    }
    return devices;
  }
}
