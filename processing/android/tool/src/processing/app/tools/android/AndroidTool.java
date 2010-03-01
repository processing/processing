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

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import processing.app.Base;
import processing.app.Editor;
import processing.app.Sketch;
import processing.app.tools.Tool;
import processing.core.PApplet;

// http://dl.google.com/android/repository/repository.xml
// http://dl.google.com/android/android-sdk_r3-mac.zip
// http://dl.google.com/android/repository/tools_r03-macosx.zip

public class AndroidTool implements Tool {
  private AndroidSDK sdk;
  private Editor editor;
  private Build build;

  private static final String ANDROID_CORE_URL = "http://dev.processing.org/source/index.cgi/*checkout*"
      + "/tags/processing-" + Base.VERSION_NAME + "/android/core.zip";

  private static final String ANDROID_CORE_FILENAME = "processing-android-core-"
      + Base.VERSION_NAME + ".zip";

  public String getMenuTitle() {
    return "Android Mode";
  }

  public void init(final Editor parent) {
    this.editor = parent;
  }

  public void run() {
    editor.statusNotice("Loading Android tools.");

    try {
      sdk = AndroidSDK.find(editor);
    } catch (final Exception e) {
      Base.showWarning("Android Tools Error", e.getMessage(), null);
      editor.statusNotice("Android mode canceled.");
      return;
    }

    // Make sure that the processing.android.core.* classes are available
    if (!checkCore()) {
      editor.statusNotice("Android mode canceled.");
      return;
    }

    editor.setHandlers(new RunHandler(), new PresentHandler(),
      new StopHandler(), new ExportHandler(), new ExportAppHandler());
    build = new Build(editor, sdk);
    editor.statusNotice("Done loading Android tools.");
  }

  static protected File getCoreZipFile() {
    // for debugging only, check to see if this is an svn checkout
    File debugFile = new File("../../../android/core.zip");
    if (!debugFile.exists() && Base.isMacOS()) {
      // current path might be inside Processing.app, so need to go much higher
      debugFile = new File("../../../../../../../android/core.zip");
    }
    if (debugFile.exists()) {
      System.err.println("Using version of core.zip from local SVN checkout.");
      return debugFile;
      // } else {
      // //System.out.println("no core.zip at " + debugFile.getAbsolutePath());
      // try {
      // System.out.println("no core.zip at " + debugFile.getCanonicalPath());
      // } catch (IOException e) {
      // e.printStackTrace();
      // }
    }

    // otherwise do the usual
    return new File(Base.getSketchbookFolder(), ANDROID_CORE_FILENAME);
  }

  private boolean checkCore() {
    // File target = new File(Base.getSketchbookFolder(),
    // ANDROID_CORE_FILENAME);
    final File target = getCoreZipFile();
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

  public Editor getEditor() {
    return editor;
  }

  public Sketch getSketch() {
    return editor.getSketch();
  }

  public Build getBuilder() {
    return build;
  }

  // if user asks for 480x320, 320x480, 854x480 etc, then launch like that
  // though would need to query the emulator to see if it can do that

  private void startSketch(final AndroidDevice device) {
    final String packageName = build.getPackageName();
    final String className = build.getClassName();
    try {
      if (device.launchApp(packageName, className)) {
        editor.statusNotice("Sketch started on the "
            + (device.isEmulator() ? "emulator" : "phone") + ".");
        return;
      }
    } catch (final Exception e) {
      e.printStackTrace(System.err);
      editor.statusError("Could not start the sketch.");
    }
  }

  private void runSketchOnDevice(final Future<AndroidDevice> deviceFuture) {
    final Build build = getBuilder();
    if (!build.createProject()) {
      return;
    }

    if (!build.antBuild("debug")) {
      return;
    }

    final AndroidDevice device;
    try {
      device = deviceFuture.get(30, TimeUnit.SECONDS);
    } catch (final InterruptedException e) {
      editor.statusError("Interrupted.");
      return;
    } catch (final ExecutionException e) {
      editor.statusError(e);
      return;
    } catch (final TimeoutException e) {
      editor.statusError("Giving up on launching the emulator.");
      return;
    }

    if (!device.installApp(build.getPathForAPK("debug"), editor)) {
      return;
    }

    startSketch(device);
  }

  /**
   * Build the sketch and run it inside an emulator with the debugger.
   */
  class RunHandler implements Runnable {
    public void run() {
      AVD.ensureEclairAVD(sdk);
      runSketchOnDevice(AndroidEnvironment.getInstance().getEmulator());
    }
  }

  /**
   * Build the sketch and run it on a device with the debugger connected.
   */
  class PresentHandler implements Runnable {
    public void run() {
      runSketchOnDevice(AndroidEnvironment.getInstance().getHardware());
    }
  }

  private static class StopHandler implements Runnable {
    public void run() {
    }
  }

  /**
   * Create a release build of the sketch and have its apk files ready.
   */
  private static class ExportHandler implements Runnable {
    public void run() {
    }
  }

  /**
   * Create a release build of the sketch and install its apk files on the
   * attached device.
   */
  private static class ExportAppHandler implements Runnable {
    public void run() {
    }
  }

}