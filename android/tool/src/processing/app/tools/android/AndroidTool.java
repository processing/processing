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
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import processing.app.Base;
import processing.app.Editor;
import processing.app.Platform;
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

    final Platform platform = Base.getPlatform();

    // check for ANDROID_SDK environment variable
    final String sdkPath = platform.getenv("ANDROID_SDK");
    if (sdkPath == null) {
      Base
          .showWarning("Android Tools Error",
            "Before using Android mode, you must first set the\n"
                + "ANDROID_SDK environment variable, and restart Processing.",
            null);
      editor.statusNotice("Android mode canceled.");
      return;
    }
    try {
      sdk = new AndroidSDK(sdkPath);
    } catch (final BadSDKException e) {
      Base.showWarning("Android Tools Error", e.getMessage(), null);
      editor.statusNotice("Android mode canceled.");
      return;
    }

    if (!checkPath()) {
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

  private static final Pattern quotedPathElement = Pattern
      .compile("^\"([^\"]*)\"$");

  // make sure that $ANDROID_SDK/tools has been added to the PATH
  protected boolean checkPath() {
    final String canonicalTools;
    try {
      canonicalTools = sdk.getTools().getCanonicalPath();
    } catch (final IOException unexpected) {
      Base.showWarning("Android Tools Error", "Unexpected internal error.",
        unexpected);
      return false;
    }

    final String envPath = Base.getPlatform().getenv("PATH");
    for (String entry : PApplet.split(envPath, File.pathSeparatorChar)) {
      entry = entry.trim();
      if (entry.length() == 0) {
        continue;
      }
      final Matcher m = quotedPathElement.matcher(entry);
      if (m.matches()) {
        entry = m.group(1); // unquote
      }
      try {
        if (canonicalTools.equals(new File(entry).getCanonicalPath())) {
          return true;
        }
      } catch (final IOException unexpected) {
        System.err.println(unexpected);
      }
    }

    Base.showWarning("Android Tools Error",
      "You need to add the tools folder of the Android SDK\n"
          + "to your PATH environment variable and restart Processing.\n"
          + "The folder is: " + sdk.getTools().getAbsolutePath() + ".", null);
    return false;
  }

  /*
  protected boolean checkPath_orig() {
    // If android.sdk.path exists as a preference, make sure that the folder
    // exists, otherwise the SDK may have been removed or deleted.
    final String oldPath = Preferences.get("android.sdk.path");
    if (oldPath != null) {
      final File oldFolder = new File(oldPath);
      if (!oldFolder.exists()) {
        // Clear the preference so that it's updated below
        Preferences.unset("android.sdk.path");
      }
    }

    // The environment variable is king. The preferences.txt entry is a page.
    final Platform platform = Base.getPlatform();
    sdkPath = findAndroidTool(platform.getenv("ANDROID_SDK"));
    if (sdkPath != null) {
      // Set this value in preferences.txt, in case ANDROID_SDK
      // gets knocked out later. For instance, by that pesky Eclipse,
      // which nukes all env variables when launching from the IDE.
      Preferences.set("android.sdk.path", sdkPath);
    } else {
      // See if the path was set earlier
      sdkPath = findAndroidTool(Preferences.get("android.sdk.path"));

      if (sdkPath == null) {
        final int result = Base.showYesNoQuestion(editor, "Android SDK",
          ANDROID_SDK_PRIMARY, ANDROID_SDK_SECONDARY);
        if (result == JOptionPane.YES_OPTION) {
          final File folder = Base.selectFolder(SELECT_ANDROID_SDK_FOLDER,
            null, editor);
          if (folder != null) {
            sdkPath = findAndroidTool(folder.getAbsolutePath());
            if (sdkPath != null) {
              Preferences.set("android.sdk.path", sdkPath);
            } else {
              // tools/android not found in the selected folder
              System.err
                  .println("Could not find the android executable inside "
                      + folder.getAbsolutePath() + "/tools/");
              JOptionPane.showMessageDialog(editor, NOT_ANDROID_SDK);
              return false;
            }
          }
        } else if (result == JOptionPane.NO_OPTION) {
          // user admitted they don't have the SDK installed, and need help.
          Base.openURL(ANDROID_SDK_URL);
        }
      }
    }
    if (sdkPath == null) {
      return false;
    }
    // if (envPath == null) {
    platform.setenv("ANDROID_SDK", sdkPath);
    // }

    // platform.setenv("ANDROID_SDK", "/opt/android");
    // sdkPath = platform.getenv("ANDROID_SDK");
    // System.out.println("sdk path is " + sdkPath);
    // sdkPath = "/opt/android";

    // // Make sure that the tools are in the PATH
    // String toolsPath = sdkPath + File.separator + "tools";
    // String path = platform.getenv("PATH");
    // System.out.println("path before set is " + path);
    // platform.setenv("PATH", path + File.pathSeparator + toolsPath);
    // System.out.println("path after set is " +
    // Base.getPlatform().getenv("PATH"));
    //
    // try {
    // PApplet.println(Device.list());
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    //
    // String[] cmd = { "echo", "$PATH" };
    // try {
    // ProcessHelper p = new ProcessHelper(cmd);
    // int result = p.waitFor();
    // if (result == 0) {
    // PApplet.println(p.getOutputLines());
    // }
    // } catch (Exception e) {
    // e.printStackTrace();
    // }

    return true;
  }
  */
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

  protected boolean checkCore() {
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
    final String id = build.getPackageName() + "/." + build.getClassName();
    try {
      if (device.launchApp(id)) {
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

  /*
  private static final String ANDROID_SDK_PRIMARY = "Is the Android SDK installed?";

  private static final String ANDROID_SDK_SECONDARY = "The Android SDK does not appear to be installed, <br>"
      + "because the ANDROID_SDK variable is not set. <br>"
      + "If it is installed, click “Yes” to select the <br>"
      + "location of the SDK, or “No” to visit the SDK<br>"
      + "download site at http://developer.android.com/sdk.";

  private static final String SELECT_ANDROID_SDK_FOLDER = "Choose the location of the Android SDK";

  private static final String NOT_ANDROID_SDK = "The selected folder does not appear to contain an Android SDK.";

  private static final String ANDROID_SDK_URL = "http://developer.android.com/sdk/";

  private static final String ADB_SOCKET_PORT = "29892";
  */

}