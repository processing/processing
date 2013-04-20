package processing.mode.android;

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
import processing.mode.android.EmulatorController.State;

/**
 * <pre> AndroidEnvironment env = AndroidEnvironment.getInstance();
 * AndroidDevice n1 = env.getHardware();
 * AndroidDevice emu = env.getEmulator();</pre>
 * @author Jonathan Feinberg &lt;jdf@pobox.com&gt;
 *
 */
class Devices {
  private static final String ADB_DEVICES_ERROR =
    "Received unfamiliar output from “adb devices”.\n" +
    "The device list may have errors.";

  private static final Devices INSTANCE = new Devices();

  public static Devices getInstance() {
    return INSTANCE;
  }

  private final Map<String, Device> devices =
    new ConcurrentHashMap<String, Device>();
  private final ExecutorService deviceLaunchThread =
    Executors.newSingleThreadExecutor();


  public static void killAdbServer() {
    System.out.println("Shutting down any existing adb server...");
    System.out.flush();
    try {
      AndroidSDK.runADB("kill-server");
    } catch (final Exception e) {
      System.err.println("Devices.killAdbServer() failed.");
      e.printStackTrace();
    }
  }


  private Devices() {
    if (processing.app.Base.DEBUG) {
      System.out.println("Starting up Devices");
    }
//    killAdbServer();
    Runtime.getRuntime().addShutdownHook(
      new Thread("processing.mode.android.Devices Shutdown") {
        public void run() {
          //System.out.println("Shutting down Devices");
          //System.out.flush();
          for (Device device : new ArrayList<Device>(devices.values())) {
            device.shutdown();
          }
          // Don't do this, it'll just make Eclipse and others freak out.
          //killAdbServer();
        }
      });
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
//    System.out.println("going looking for emulator");
    Device emu = find(true);
    if (emu != null) {
//      System.out.println("found emu " + emu);
      return emu;
    }
//    System.out.println("no emu found");

    EmulatorController emuController = EmulatorController.getInstance();
//    System.out.println("checking emulator state");
    if (emuController.getState() == State.NOT_RUNNING) {
      try {
//        System.out.println("not running, gonna launch");
        emuController.launch(); // this blocks until emulator boots
//        System.out.println("not just gonna, we've done the launch");
      } catch (final IOException e) {
        System.err.println("Problem while launching emulator.");
        e.printStackTrace(System.err);
        return null;
      }
    } else {
      System.out.println("Emulator is " + emuController.getState() +
                         ", which is not expected.");
    }
//    System.out.println("and now we're out");

//    System.out.println("Devices.blockingGet thread is " + Thread.currentThread());
    while (!Thread.currentThread().isInterrupted()) {
      //      System.err.println("AndroidEnvironment: looking for emulator in loop.");
      //      System.err.println("AndroidEnvironment: emulatorcontroller state is "
      //          + emuController.getState());
      if (emuController.getState() == State.NOT_RUNNING) {
        System.err.println("Error while starting the emulator. (" +
                           emuController.getState() + ")");
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
        System.err.println("Devices: interrupted in loop.");
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
    final List<String> activeDevices = list();
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
   * <p>First line starts "List of devices"
   *
   * <p>When an emulator is started with a debug port, then it shows up
   * in the list of devices.
   *
   * <p>List of devices attached
   * <br>HT91MLC00031 device
   * <br>emulator-5554 offline
   *
   * <p>List of devices attached
   * <br>HT91MLC00031 device
   * <br>emulator-5554 device
   *
   * @return list of device identifiers
   * @throws IOException
   */
  public static List<String> list() {
    ProcessResult result;
    try {
//      System.out.println("listing devices 00");
      result = AndroidSDK.runADB("devices");
//      System.out.println("listing devices 05");
    } catch (InterruptedException e) {
      return Collections.emptyList();
    } catch (IOException e) {
      System.err.println("Problem inside Devices.list()");
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
      System.err.println("Output was “" + stdout + "”");
      return Collections.emptyList();
    }

//    System.out.println("listing devices 30");
    final List<String> devices = new ArrayList<String>();
    for (final String line : result) {
      if (line.contains("\t")) {
        final String[] fields = line.split("\t");
        if (fields[1].equals("device")) {
          devices.add(fields[0]);
        }
      }
    }
    return devices;
  }
}
