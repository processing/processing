/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2011 Ben Fry and Casey Reas

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License version 2
 as published by the Free Software Foundation.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package processing.mode.android;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import processing.app.RunnerListener;
import processing.app.SketchException;
import processing.mode.java.runner.Runner;


public class AndroidRunner implements DeviceListener {
  AndroidBuild build;
  RunnerListener listener;
  
  
  public AndroidRunner(AndroidBuild build, RunnerListener listener) {
    this.build = build;
    this.listener = listener;
  }
  
  
  public void launch(Future<Device> deviceFuture) {
//    try {
//      runSketchOnDevice(Devices.getInstance().getEmulator(), "debug", AndroidEditor.this);
//    } catch (final MonitorCanceled ok) {
//      sketchStopped();
//      statusNotice("Canceled.");
//    }

    listener.statusNotice("Waiting for device to become available...");
//  final Device device = waitForDevice(deviceFuture, monitor);
    final Device device = waitForDevice(deviceFuture, listener);
    if (device == null || !device.isAlive()) {
      listener.statusError("Lost connection with device while launching. Try again.");
      // Reset the server, in case that's the problem. Sometimes when 
      // launching the emulator times out, the device list refuses to update.
      Devices.killAdbServer();
      return;
    }

    device.addListener(this);

//  if (listener.isHalted()) {
////  if (monitor.isCanceled()) {
//    throw new MonitorCanceled();
//  }

//  monitor.setNote("Installing sketch on " + device.getId());
    listener.statusNotice("Installing sketch on " + device.getId());
    if (!device.installApp(build.getPathForAPK(), listener)) {
      listener.statusError("Lost connection with device while installing. Try again.");
      Devices.killAdbServer();  // see above
      return;
    }

//  if (monitor.isCanceled()) {
//    throw new MonitorCanceled();
//  }
//  monitor.setNote("Starting sketch on " + device.getId());
    listener.statusNotice("Starting sketch on " + device.getId());
    if (startSketch(build, device)) {
      listener.statusNotice("Sketch launched on the "
                            + (device.isEmulator() ? "emulator" : "device") + ".");
    } else {
      listener.statusError("Could not start the sketch.");
    }
    listener.stopIndeterminate();
    lastRunDevice = device;
//} finally {
//  build.cleanup();
//}
//} finally {
////monitor.close();
//listener.stopIndeterminate();
//}
  }
  
  
  private volatile Device lastRunDevice = null;

  /**
   * @param target "debug" or "release"
   */
  /*
  private void runSketchOnDevice(Sketch sketch,
                                 Future<Device> deviceFuture,
                                 String target,
                                 RunnerListener listener) {
//    final IndeterminateProgressMonitor monitor =
//      new IndeterminateProgressMonitor(this,
//                                       "Building and launching...",
//                                       "Creating project...");

    
    AndroidBuild build = new AndroidBuild(sketch, listener);
    try {
      try {
        if (build.createProject(target) == null) {
          return;
        }
      } catch (SketchException se) {
        listener.statusError(se);
      } catch (IOException e) {
        listener.statusError(e);
      }
      try {
//        if (monitor.isCanceled()) {
//          throw new MonitorCanceled();
//        }
//        monitor.setNote("Building...");
        listener.statusNotice("Building...");
        try {
          if (!build.antBuild(target)) {
            return;
          }
        } catch (SketchException se) {
          listener.statusError(se);
        }

//        if (monitor.isCanceled()) {
//          throw new MonitorCanceled();
//        }
//        monitor.setNote("Waiting for device to become available...");
        listener.statusNotice("Waiting for device to become available...");
//        final Device device = waitForDevice(deviceFuture, monitor);
        final Device device = waitForDevice(deviceFuture, listener);
        if (device == null || !device.isAlive()) {
          listener.statusError("Device killed or disconnected.");
          return;
        }

        device.addListener(this);

//        if (listener.isHalted()) {
////        if (monitor.isCanceled()) {
//          throw new MonitorCanceled();
//        }

//        monitor.setNote("Installing sketch on " + device.getId());
        listener.statusNotice("Installing sketch on " + device.getId());
        if (!device.installApp(build.getPathForAPK(target), listener)) {
          listener.statusError("Device killed or disconnected.");
          return;
        }

//        if (monitor.isCanceled()) {
//          throw new MonitorCanceled();
//        }
//        monitor.setNote("Starting sketch on " + device.getId());
        listener.statusNotice("Starting sketch on " + device.getId());
        if (startSketch(build, device)) {
          listener.statusNotice("Sketch launched on the "
              + (device.isEmulator() ? "emulator" : "device") + ".");
        } else {
          listener.statusError("Could not start the sketch.");
        }

        lastRunDevice = device;
      } finally {
        build.cleanup();
      }
    } finally {
//      monitor.close();
      listener.stopIndeterminate();
    }
  }
  */

  
  // if user asks for 480x320, 320x480, 854x480 etc, then launch like that
  // though would need to query the emulator to see if it can do that

  private boolean startSketch(AndroidBuild build, final Device device) {
    final String packageName = build.getPackageName();
    final String className = build.getSketchClassName();
    try {
      if (device.launchApp(packageName, className)) {
        return true;
      }
    } catch (final Exception e) {
      e.printStackTrace(System.err);
    }
    return false;
  }


  private Device waitForDevice(Future<Device> deviceFuture, RunnerListener listener) {
    for (int i = 0; i < 120; i++) {
//      if (monitor.isCanceled()) {
      if (listener.isHalted()) {
        deviceFuture.cancel(true);
//        throw new MonitorCanceled();
        return null;
      }
      try {
        return deviceFuture.get(1, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        listener.statusError("Interrupted.");
        return null;
      } catch (final ExecutionException e) {
        listener.statusError(e);
        return null;
      } catch (final TimeoutException expected) {
      }
    }
    listener.statusError("No, on second thought, I'm giving up " +
                         "on waiting for that device to show up.");
    return null;
  }
  
  
  private static final Pattern LOCATION =
    Pattern.compile("\\(([^:]+):(\\d+)\\)");
  private static final Pattern EXCEPTION_PARSER =
    Pattern.compile("^\\s*([a-z]+(?:\\.[a-z]+)+)(?:: .+)?$",
                    Pattern.CASE_INSENSITIVE);

  /**
   * Currently figures out the first relevant stack trace line
   * by looking for the telltale presence of "processing.android"
   * in the package. If the packaging for droid sketches changes,
   * this method will have to change too.
   */
  public void stackTrace(final List<String> trace) {
    final Iterator<String> frames = trace.iterator();
    final String exceptionLine = frames.next();

    final Matcher m = EXCEPTION_PARSER.matcher(exceptionLine);
    if (!m.matches()) {
      System.err.println("Can't parse this exception line:");
      System.err.println(exceptionLine);
      listener.statusError("Unknown exception");
      return;
    }
    final String exceptionClass = m.group(1);
    if (Runner.handleCommonErrors(exceptionClass, exceptionLine, listener)) {
      return;
    }

    while (frames.hasNext()) {
      final String line = frames.next();
      if (line.contains("processing.android")) {
        final Matcher lm = LOCATION.matcher(line);
        if (lm.find()) {
          final String filename = lm.group(1);
          final int lineNumber = Integer.parseInt(lm.group(2)) - 1;
          final SketchException rex =
            build.placeException(exceptionLine, filename, lineNumber);
          listener.statusError(rex == null ? new SketchException(exceptionLine, false) : rex);
          return;
        }
      }
    }
  }


  // called by AndroidMode.handleStop()...
  public void close() {
    if (lastRunDevice != null) {
      lastRunDevice.bringLauncherToFront();
    }
  }


  // sketch stopped on the device 
  public void sketchStopped() {
    listener.stopIndeterminate();
    listener.statusHalt();
  }
}
