/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2009-11 Ben Fry and Casey Reas

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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import processing.app.Base;
import processing.app.Mode;
import processing.app.SketchException;
import processing.core.PApplet;
import processing.mode.java.JavaEditor;
import processing.mode.java.runner.Runner;

// http://dl.google.com/android/repository/repository.xml
// http://dl.google.com/android/android-sdk_r3-mac.zip
// http://dl.google.com/android/repository/tools_r03-macosx.zip

// contains the android guts, the google specifics, and the usb driver for windows
// https://dl-ssl.google.com/android/repository/addon.xml

// need to lock to a specific sdk version and tools version anyway
// may as well do the auto-download thing.


public class AndroidEditor extends JavaEditor implements DeviceListener {  
  private AndroidSDK sdk;
  private AndroidBuild build;

//  private static final String ANDROID_CORE_FILENAME =
//    "processing-android-core-" + Base.VERSION_NAME + ".zip";

//  private static final String ANDROID_CORE_URL =
//    "http://processing.googlecode.com/files/" + ANDROID_CORE_FILENAME;
  
  AndroidMode amode;
  
  
  protected AndroidEditor(Base base, String path, int[] location, Mode mode) {
    super(base, path, location, mode);
    amode = (AndroidMode) mode;

    statusNotice("Loading Android tools.");

    if (sdk == null) {
      try {
        sdk = AndroidSDK.find(this);
        statusNotice("Done loading Android tools.");

      } catch (Exception e) {
        Base.showWarning("Android Tools Error", e.getMessage(), null);
        statusError("Android Mode is disabled.");
      }
    }

    // Make sure that the processing.android.core.* classes are available
    //  if (!checkCore()) {
    //    statusNotice("Android mode canceled.");
    //    return false;
    //  }

  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  public JMenu buildFileMenu() {
    String exportTitle = AndroidToolbar.getTitle(AndroidToolbar.EXPORT, false);
    JMenuItem exportProject = Base.newJMenuItem(exportTitle, 'E');
    exportProject.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleExportProject();
      }
    });
      
    exportTitle = AndroidToolbar.getTitle(AndroidToolbar.EXPORT, true);
    JMenuItem exportPackage = Base.newJMenuItemShift(exportTitle, 'E');
    exportPackage.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleExportApplication();
      }
    });
    return buildFileMenu(new JMenuItem[] { exportProject, exportPackage });
  }
  
  
  public JMenu buildSketchMenu() {
    JMenuItem runItem = Base.newJMenuItem(AndroidToolbar.getTitle(AndroidToolbar.RUN, false), 'R');
    runItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleRunEmulator();
        }
      });

    JMenuItem presentItem = Base.newJMenuItemShift(AndroidToolbar.getTitle(AndroidToolbar.RUN, true), 'R');
    presentItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleRunDevice();
        }
      });

    JMenuItem stopItem = new JMenuItem(AndroidToolbar.getTitle(AndroidToolbar.STOP, false));
    stopItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleStop();
        }
      });
    return buildSketchMenu(new JMenuItem[] { runItem, presentItem, stopItem });
  }


  public JMenu buildModeMenu() {
    JMenu menu = new JMenu("Android");    
    JMenuItem item;
    
    item = new JMenuItem("Sketch Permissions");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        new Permissions(sketch);
      }
    });
    menu.add(item);    
    
    menu.addSeparator();

    item = new JMenuItem("Signing Key Setup");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        new Keys(AndroidEditor.this);
      }
    });
    item.setEnabled(false);
    menu.add(item);
    
    item = new JMenuItem("Android SDK & AVD Manager");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        File file = sdk.getAndroidTool();
        PApplet.exec(new String[] { file.getAbsolutePath() });
      }
    });
    menu.add(item);

    item = new JMenuItem("Reset Connections");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
//        editor.statusNotice("Resetting the Android Debug Bridge server.");
        Environment.killAdbServer();
      }
    });
    menu.add(item);    
    
    return menu;
  }
  

  /**
   * Uses the main help menu, and adds a few extra options. If/when there's 
   * Android-specific documentation, we'll switch to that. 
   */
  public JMenu buildHelpMenu() {
    JMenu menu = super.buildHelpMenu();
    JMenuItem item;
    
    menu.addSeparator();
    
    item = new JMenuItem("Processing for Android Wiki");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Base.openURL("http://wiki.processing.org/w/Android");
      }
    });
    menu.add(item);
    
    
    item = new JMenuItem("Android Developers Site");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Base.openURL("http://developer.android.com/index.html");
      }
    });
    menu.add(item);

    return menu;
  }
  
  
//  protected void updateMode() {
//    // When the selection is made, the menu will update itself
//    boolean active = toggleItem.isSelected();
//    if (active) {
//      boolean rolling = true;
//      if (sdk == null) {
//        rolling = loadAndroid();
//      }
//      if (rolling) {
//        editor.setHandlers(new RunHandler(), new PresentHandler(), 
//                           new StopHandler(), 
//                           new ExportHandler(),  new ExportAppHandler());
//        build = new AndroidBuild(editor, sdk);
//        editor.statusNotice("Android mode enabled for this editor window.");
//      }
//    } else {
//      editor.resetHandlers();
//      editor.statusNotice("Android mode disabled.");
//    }
//  }


//  protected boolean loadAndroid() {
//    statusNotice("Loading Android tools.");
//
//    try {
//      sdk = AndroidSDK.find(this);
//    } catch (final Exception e) {
//      Base.showWarning("Android Tools Error", e.getMessage(), null);
//      statusNotice("Android mode canceled.");
//      return false;
//    }
//
//    // Make sure that the processing.android.core.* classes are available
//    if (!checkCore()) {
//      statusNotice("Android mode canceled.");
//      return false;
//    }
//
//    statusNotice("Done loading Android tools.");
//    return true;
//  }


//  static protected File getCoreZipLocation() {
//    if (coreZipLocation == null) {
//      coreZipLocation = checkCoreZipLocation();
//    }
//    return coreZipLocation;
//  }


//  private boolean checkCore() {
//    final File target = getCoreZipLocation();
//    if (!target.exists()) {
//      try {
//        final URL url = new URL(ANDROID_CORE_URL);
//        PApplet.saveStream(target, url.openStream());
//      } catch (final Exception e) {
//        Base.showWarning("Download Error",
//          "Could not download Android core.zip", e);
//        return false;
//      }
//    }
//    return true;
//  }
  
  
  // if user asks for 480x320, 320x480, 854x480 etc, then launch like that
  // though would need to query the emulator to see if it can do that

  private boolean startSketch(final Device device) {
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

  
  private Device waitForDevice(final Future<Device> deviceFuture, 
                               final IndeterminateProgressMonitor monitor) throws MonitorCanceled {
    for (int i = 0; i < 120; i++) {
      if (monitor.isCanceled()) {
        deviceFuture.cancel(true);
        throw new MonitorCanceled();
      }
      try {
        return deviceFuture.get(1, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        statusError("Interrupted.");
        return null;
      } catch (final ExecutionException e) {
        statusError(e);
        return null;
      } catch (final TimeoutException expected) {
      }
    }
    statusError("No, on second thought, I'm giving up " +
                "on waiting for that device to show up.");
    return null;
  }


  private volatile Device lastRunDevice = null;

  /**
   * @param target "debug" or "release"
   */
  private void runSketchOnDevice(final Future<Device> deviceFuture,
                                 final String target) throws MonitorCanceled {
    final IndeterminateProgressMonitor monitor =
      new IndeterminateProgressMonitor(this,
                                       "Building and launching...",
                                       "Creating project...");
    try {
      try {
        if (build.createProject(target, amode.getCoreZipLocation()) == null) {
          return;
        }
      } catch (SketchException se) {
        statusError(se);
      } catch (IOException e) {
        statusError(e);
      }
      try {
        if (monitor.isCanceled()) {
          throw new MonitorCanceled();
        }
        monitor.setNote("Building...");
        try {
          if (!build.antBuild(target)) {
            return;
          }
        } catch (SketchException se) {
          statusError(se);
        }

        if (monitor.isCanceled()) {
          throw new MonitorCanceled();
        }
        monitor.setNote("Waiting for device to become available...");
        final Device device = waitForDevice(deviceFuture, monitor);
        if (device == null || !device.isAlive()) {
          statusError("Device killed or disconnected.");
          return;
        }

        device.addListener(this);

        if (monitor.isCanceled()) {
          throw new MonitorCanceled();
        }
        monitor.setNote("Installing sketch on " + device.getId());
        if (!device.installApp(build.getPathForAPK(target), this)) {
          statusError("Device killed or disconnected.");
          return;
        }

        if (monitor.isCanceled()) {
          throw new MonitorCanceled();
        }
        monitor.setNote("Starting sketch on " + device.getId());
        if (startSketch(device)) {
          statusNotice("Sketch launched on the "
              + (device.isEmulator() ? "emulator" : "phone") + ".");
        } else {
          statusError("Could not start the sketch.");
        }

        lastRunDevice = device;
      } finally {
        build.cleanup();
      }
    } finally {
      monitor.close();
    }
  }


  private void buildReleaseForExport(String target) throws MonitorCanceled {
    final IndeterminateProgressMonitor monitor =
      new IndeterminateProgressMonitor(this,
                                       "Building and exporting...",
                                       "Creating project...");
    try {
      File tempFolder = null;
      try {
        tempFolder = build.createProject(target, amode.getCoreZipLocation());
        if (tempFolder == null) {
          return;
        }
      } catch (IOException e) {
        e.printStackTrace();
      } catch (SketchException se) {
        se.printStackTrace();
      }
      try {
        if (monitor.isCanceled()) {
          throw new MonitorCanceled();
        }
        monitor.setNote("Building release version...");
//        if (!build.antBuild("release")) {
//          return;
//        }

        if (monitor.isCanceled()) {
          throw new MonitorCanceled();
        }

        // If things built successfully, copy the contents to the export folder
        File exportFolder = build.createExportFolder();
        if (exportFolder != null) {
          Base.copyDir(tempFolder, exportFolder);
          statusNotice("Done with export.");
          Base.openFolder(exportFolder);
        } else {
          statusError("Could not copy files to export folder.");
        }
      } catch (IOException e) {
        statusError(e);

      } finally {
        build.cleanup();
      }
    } finally {
      monitor.close();
    }
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
      statusError("Unknown exception");
      return;
    }
    final String exceptionClass = m.group(1);
    if (Runner.handleCommonErrors(exceptionClass, exceptionLine, this)) {
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
          statusError(rex == null ? new SketchException(exceptionLine, false) : rex);
          return;
        }
      }

    }
  }


  public void sketchStopped() {
    deactivateRun();
    statusEmpty();
  }

  
  /**
   * Build the sketch and run it inside an emulator with the debugger.
   */
  public void handleRunEmulator() {
    AVD.ensureEclairAVD(sdk);
    try {
      runSketchOnDevice(Environment.getInstance().getEmulator(), "debug");
    } catch (final MonitorCanceled ok) {
      sketchStopped();
      statusNotice("Canceled.");
    }
  }

  /**
   * Build the sketch and run it on a device with the debugger connected.
   */
  public void handleRunDevice() {
    try {
      runSketchOnDevice(Environment.getInstance().getHardware(), "debug");
    } catch (final MonitorCanceled ok) {
      sketchStopped();
      statusNotice("Canceled.");
    }
  }


  public void handleStop() {
    if (lastRunDevice != null) {
      lastRunDevice.bringLauncherToFront();
    }
  }

  
  /**
   * Create a release build of the sketch and have its apk files ready.
   * If users want a debug build, they can do that from the command line.
   */
  public void handleExportProject() {
    try {
      buildReleaseForExport("debug");
    } catch (final MonitorCanceled ok) {
      statusNotice("Canceled.");
    } finally {
      deactivateExport();
    }
  }

  
  /**
   * Create a release build of the sketch and install its apk files on the
   * attached device.
   */
  public void handleExportPackage() {
    // Need to implement an entire signing setup first
    // http://dev.processing.org/bugs/show_bug.cgi?id=1430
    statusError("Export application not yet implemented.");
    deactivateExport();

    // make a release build
//    try {
//      buildReleaseForExport("release");
//    } catch (final MonitorCanceled ok) {
//      statusNotice("Canceled.");
//    } finally {
//      deactivateExport();
//    }
    
    // TODO now sign it... lots of fun signing code mess to go here. yay!

    // maybe even send it to the device? mmm?
//      try {
//        runSketchOnDevice(AndroidEnvironment.getInstance().getHardware(), "release");
//      } catch (final MonitorCanceled ok) {
//        editor.statusNotice("Canceled.");
//      } finally {
//        editor.deactivateExport();
//      }
  }


  @SuppressWarnings("serial")
  private static class MonitorCanceled extends Exception {
  }
}