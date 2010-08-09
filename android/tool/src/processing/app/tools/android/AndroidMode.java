/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2009-10 Ben Fry and Casey Reas

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

package processing.app.tools.android;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import processing.app.*;
import processing.app.debug.*;

import processing.core.PApplet;

// http://dl.google.com/android/repository/repository.xml
// http://dl.google.com/android/android-sdk_r3-mac.zip
// http://dl.google.com/android/repository/tools_r03-macosx.zip

public class AndroidMode implements DeviceListener {
  private AndroidSDK sdk;
  private Editor editor;
  private Build build;
  
  static public boolean DEBUG = true;

  private static final String ANDROID_CORE_FILENAME =
    "processing-android-core-" + Base.VERSION_NAME + ".zip";

  private static final String ANDROID_CORE_URL =
    "http://processing.googlecode.com/files/" + ANDROID_CORE_FILENAME;
//  private static final String ANDROID_CORE_URL =
//    "http://processing.googlecode.com/svn" +
//    "/tags/processing-" + Base.VERSION_NAME + "/android/core.zip";

//  public String getMenuTitle() {
//    return "Android Mode";
//  }

//  public void init(final Editor parent) {
//    this.editor = parent;
//  }
  
  JCheckBoxMenuItem toggleItem;
  
  public void init(final Editor parent, final JMenuBar menubar) {
    this.editor = parent;
    

    JMenu menu = new JMenu("Android");    
    JMenuItem item;
    
    toggleItem = Base.newJCheckBoxMenuItem("Android Mode", 'D');
    toggleItem.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        updateMode();
      } 
    });
    menu.add(toggleItem);
    
    item = new JMenuItem("Guide");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Base.openURL("http://wiki.processing.org/w/Android");
      }
    });
    menu.add(item);
    
    menu.addSeparator();

    item = new JMenuItem("Sketch Options");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        //new Permissions(editor);
      }
    });
    menu.add(item);

    item = new JMenuItem("Sketch Permissions");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        new Permissions(editor);
      }
    });
    menu.add(item);    
    
    menu.addSeparator();

    item = new JMenuItem("Signing Key Setup");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        new Keys(editor);
      }
    });
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
        AndroidEnvironment.killAdbServer();
      }
    });
    menu.add(item);    
    
    menubar.add(menu);
  }
  
  
  protected void updateMode() {
    // When the selection is made, the menu will update itself
    boolean active = toggleItem.isSelected();
    if (active) {
      boolean rolling = true;
      if (sdk == null) {
        rolling = loadAndroid();
      }
      if (rolling) {
        editor.setHandlers(new RunHandler(), new PresentHandler(), 
                           new StopHandler(), 
                           new ExportHandler(),  new ExportAppHandler());
        build = new Build(editor, sdk);
        editor.statusNotice("Android mode enabled for this editor window.");
      }
    } else {
      editor.resetHandlers();
      editor.statusNotice("Android mode disabled.");
    }
  }


  protected boolean loadAndroid() {
    editor.statusNotice("Loading Android tools.");

    try {
      sdk = AndroidSDK.find((editor instanceof Frame) ? (Frame) editor : null);
    } catch (final Exception e) {
      Base.showWarning("Android Tools Error", e.getMessage(), null);
      editor.statusNotice("Android mode canceled.");
      return false;
    }

    // Make sure that the processing.android.core.* classes are available
    if (!checkCore()) {
      editor.statusNotice("Android mode canceled.");
      return false;
    }

    editor.statusNotice("Done loading Android tools.");
    return true;
  }


  static private File coreZipLocation;

  static protected File getCoreZipLocation() {
    if (coreZipLocation == null) {
      coreZipLocation = checkCoreZipLocation();
    }
    return coreZipLocation;
  }


  static protected File checkCoreZipLocation() {
    // for debugging only, check to see if this is an svn checkout
    File debugFile = new File("../../../android/core.zip");
    if (!debugFile.exists() && Base.isMacOS()) {
      // current path might be inside Processing.app, so need to go much higher
      debugFile = new File("../../../../../../../android/core.zip");
    }
    if (debugFile.exists()) {
      System.out.println("Using version of core.zip from local SVN checkout.");
      return debugFile;
    }

    // otherwise do the usual
    return new File(Base.getSketchbookFolder(), ANDROID_CORE_FILENAME);
  }


  private boolean checkCore() {
    final File target = getCoreZipLocation();
    if (!target.exists()) {
      try {
        final URL url = new URL(ANDROID_CORE_URL);
        PApplet.saveStream(target, url.openStream());
      } catch (final Exception e) {
        Base.showWarning("Download Error",
          "Could not download Android core.zip", e);
        return false;
      }
    }
    return true;
  }
  
  
  static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd.HHmm");
  
  static public String getDateStamp() {
    return dateFormat.format(new Date());
  }

  static public String getDateStamp(long stamp) {
    return dateFormat.format(new Date(stamp));
  }

  
//  public Editor getEditor() {
//    return editor;
//  }

//  public Sketch getSketch() {
//    return editor.getSketch();
//  }

//  public Build getBuilder() {
//    return build;
//  }

  // if user asks for 480x320, 320x480, 854x480 etc, then launch like that
  // though would need to query the emulator to see if it can do that

  private boolean startSketch(final AndroidDevice device) {
    final String packageName = build.getPackageName();
    final String className = build.getClassName();
    try {
      if (device.launchApp(packageName, className)) {
        return true;
      }
    } catch (final Exception e) {
      e.printStackTrace(System.err);
    }
    return false;
  }

  private AndroidDevice waitForDevice(final Future<AndroidDevice> deviceFuture,
                                      final IndeterminateProgressMonitor monitor)
      throws MonitorCanceled {
    for (int i = 0; i < 120; i++) {
      if (monitor.isCanceled()) {
        deviceFuture.cancel(true);
        throw new MonitorCanceled();
      }
      try {
        return deviceFuture.get(1, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        editor.statusError("Interrupted.");
        return null;
      } catch (final ExecutionException e) {
        editor.statusError(e);
        return null;
      } catch (final TimeoutException expected) {
      }
    }
    editor.statusError("No, on second thought, I'm giving up " +
                       "on waiting for that device to show up.");
    return null;
  }


  private volatile AndroidDevice lastRunDevice = null;

  /**
   * @param target "debug" or "release"
   */
  private void runSketchOnDevice(final Future<AndroidDevice> deviceFuture,
                                 final String target) throws MonitorCanceled {
    final IndeterminateProgressMonitor monitor =
      new IndeterminateProgressMonitor(editor,
                                       "Building and launching...",
                                       "Creating project...");
    try {
      if (build.createProject(target) == null) {
        return;
      }
      try {
        if (monitor.isCanceled()) {
          throw new MonitorCanceled();
        }
        monitor.setNote("Building...");
        if (!build.antBuild(target)) {
          return;
        }

        if (monitor.isCanceled()) {
          throw new MonitorCanceled();
        }
        monitor.setNote("Waiting for device to become available...");
        final AndroidDevice device = waitForDevice(deviceFuture, monitor);
        if (device == null || !device.isAlive()) {
          editor.statusError("Device killed or disconnected.");
          return;
        }

        device.addListener(this);

        if (monitor.isCanceled()) {
          throw new MonitorCanceled();
        }
        monitor.setNote("Installing sketch on " + device.getId());
        if (!device.installApp(build.getPathForAPK(target), editor)) {
          editor.statusError("Device killed or disconnected.");
          return;
        }

        if (monitor.isCanceled()) {
          throw new MonitorCanceled();
        }
        monitor.setNote("Starting sketch on " + device.getId());
        if (startSketch(device)) {
          editor.statusNotice("Sketch launched on the "
              + (device.isEmulator() ? "emulator" : "phone") + ".");
        } else {
          editor.statusError("Could not start the sketch.");
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
      new IndeterminateProgressMonitor(editor,
                                       "Building and exporting...",
                                       "Creating project...");
    try {
      File tempFolder = build.createProject(target);
      if (tempFolder == null) {
        return;
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
          editor.statusNotice("Done with export.");
          Base.openFolder(exportFolder);
        } else {
          editor.statusError("Could not copy files to export folder.");
        }
      } catch (IOException e) {
        editor.statusError(e);

      } finally {
        build.cleanup();
      }
    } finally {
      monitor.close();
    }
  }


  /*
  private void buildReleaseForDevice(final Future<AndroidDevice> deviceFuture) throws Cancelled {
    final IndeterminateProgressMonitor monitor =
      new IndeterminateProgressMonitor(editor,
                                       "Building and running...",
                                       "Creating project...");
    try {
      if (build.createProject() == null) {
        return;
      }
      try {
        if (monitor.isCanceled()) {
          throw new Cancelled();
        }
        monitor.setNote("Building...");
        if (!build.antBuild("release")) {
          return;
        }

        if (monitor.isCanceled()) {
          throw new Cancelled();
        }

        monitor.setNote("Waiting for device to become available...");
        final AndroidDevice device = waitForDevice(deviceFuture, monitor);
        if (device == null || !device.isAlive()) {
          editor.statusError("Device killed or disconnected.");
          return;
        }

        device.addListener(this);

        if (monitor.isCanceled()) {
          throw new Cancelled();
        }
        monitor.setNote("Installing sketch on " + device.getId());
        if (!device.installApp(build.getPathForAPK("release"), editor)) {
          editor.statusError("Device killed or disconnected.");
          return;
        }

        if (monitor.isCanceled()) {
          throw new Cancelled();
        }
        monitor.setNote("Starting sketch on " + device.getId());
        if (startSketch(device)) {
          editor.statusNotice("Release version of sketch launched on the "
              + (device.isEmulator() ? "emulator" : "phone") + ".");
        } else {
          editor.statusError("Could not start the sketch.");
        }
        lastRunDevice = device;

      } finally {
        build.cleanup();
      }
    } finally {
      monitor.close();
    }
  }
  */


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
      editor.statusError("Unknown exception");
      return;
    }
    final String exceptionClass = m.group(1);
    if (Runner.handleCommonErrors(exceptionClass, exceptionLine, editor)) {
      return;
    }

    while (frames.hasNext()) {
      final String line = frames.next();
      if (line.contains("processing.android")) {
        final Matcher lm = LOCATION.matcher(line);
        if (lm.find()) {
          final String filename = lm.group(1);
          final int lineNumber = Integer.parseInt(lm.group(2)) - 1;
          final RunnerException rex = editor.getSketch().placeException(
            exceptionLine, filename, lineNumber);
          editor.statusError(rex == null ? new RunnerException(exceptionLine,
                                                               false) : rex);
          return;
        }
      }

    }
  }

  public void sketchStopped() {
    editor.deactivateRun();
    editor.statusEmpty();
  }

  /**
   * Build the sketch and run it inside an emulator with the debugger.
   */
  class RunHandler implements Runnable {
    public void run() {
      AVD.ensureEclairAVD(sdk);
      try {
        runSketchOnDevice(AndroidEnvironment.getInstance().getEmulator(), "debug");
      } catch (final MonitorCanceled ok) {
        sketchStopped();
        editor.statusNotice("Canceled.");
      }
    }
  }

  /**
   * Build the sketch and run it on a device with the debugger connected.
   */
  class PresentHandler implements Runnable {
    public void run() {
      try {
        runSketchOnDevice(AndroidEnvironment.getInstance().getHardware(), "debug");
      } catch (final MonitorCanceled ok) {
        sketchStopped();
        editor.statusNotice("Canceled.");
      }
    }
  }

  private class StopHandler implements Runnable {
    public void run() {
      if (lastRunDevice != null) {
        lastRunDevice.bringLauncherToFront();
      }
    }
  }

  /**
   * Create a release build of the sketch and have its apk files ready.
   * If users want a debug build, they can do that from the command line.
   */
  private class ExportHandler implements Runnable {
    public void run() {
      try {
        buildReleaseForExport("debug");
      } catch (final MonitorCanceled ok) {
        editor.statusNotice("Canceled.");
      } finally {
        editor.deactivateExport();
      }
    }
  }

  /**
   * Create a release build of the sketch and install its apk files on the
   * attached device.
   */
  private class ExportAppHandler implements Runnable {
    public void run() {
      //buildReleaseForExport("release");

      // Need to implement an entire signing setup first
      // http://dev.processing.org/bugs/show_bug.cgi?id=1430
      editor.statusError("Export application not yet implemented.");
      editor.deactivateExport();

//      try {
//        runSketchOnDevice(AndroidEnvironment.getInstance().getHardware(), "release");
//      } catch (final MonitorCanceled ok) {
//        editor.statusNotice("Canceled.");
//      } finally {
//        editor.deactivateExport();
//      }
    }
  }

  @SuppressWarnings("serial")
  private static class MonitorCanceled extends Exception {
  }

}