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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import processing.app.Base;
import processing.app.Editor;
import processing.app.Platform;
import processing.app.Preferences;
import processing.app.Sketch;
import processing.app.StreamRedirectThread;
import processing.app.tools.Tool;
import processing.core.PApplet;

// http://dl.google.com/android/repository/repository.xml
// http://dl.google.com/android/android-sdk_r3-mac.zip
// http://dl.google.com/android/repository/tools_r03-macosx.zip

public class AndroidTool implements Tool {
  private static final ProcessHelper KILL_SERVER_COMMAND = new ProcessHelper(
                                                                             "adb",
                                                                             "kill-server");

  private static final ProcessHelper START_SERVER_CMD = new ProcessHelper(
                                                                          "adb",
                                                                          "start-server");

  static String sdkPath;

  static String toolName = "android";

  // static String toolsPath;

  private Editor editor;

  Build build;

  Process emulatorProcess;

  static final String ANDROID_SDK_PRIMARY = "Is the Android SDK installed?";

  static final String ANDROID_SDK_SECONDARY = "The Android SDK does not appear to be installed, <br>"
      + "because the ANDROID_SDK variable is not set. <br>"
      + "If it is installed, click “Yes” to select the <br>"
      + "location of the SDK, or “No” to visit the SDK<br>"
      + "download site at http://developer.android.com/sdk.";

  static final String SELECT_ANDROID_SDK_FOLDER = "Choose the location of the Android SDK";

  static final String NOT_ANDROID_SDK = "The selected folder does not appear to contain an Android SDK.";

  static final String ANDROID_SDK_URL = "http://developer.android.com/sdk/";

  static final String ADB_SOCKET_PORT = "29892";

  static final String ANDROID_CORE_URL = "http://dev.processing.org/source/index.cgi/*checkout*"
      + "/tags/processing-" + Base.VERSION_NAME + "/android/core.zip";

  static final String ANDROID_CORE_FILENAME = "processing-android-core-"
      + Base.VERSION_NAME + ".zip";

  public String getMenuTitle() {
    return "Android Mode";
  }

  public void init(final Editor parent) {
    this.editor = parent;
  }

  public void run() {
    // System.out.println("being called like so:");
    // new Exception().printStackTrace();

    editor.statusNotice("Loading Android tools.");

    boolean success = checkPath();
    if (!success) {
      editor.statusNotice("Android mode canceled.");
      return;
    }

    // Make sure things are going to behave properly.
    checkServer();

    success = Device.checkDefaults();
    if (!success) {
      editor.statusError("Could not load Android tools.");
      return;
    }

    // Make sure that the processing.android.core.* classes are available
    checkCore();

    editor.setHandlers(new RunHandler(), new PresentHandler(),
      new StopHandler(), new ExportHandler(), new ExportAppHandler());
    build = new Build(editor);
    editor.statusNotice("Done loading Android tools.");
  }

  private static final Pattern quotedPathElement = Pattern
      .compile("^\"([^\"]*)\"$");

  protected boolean checkPath() {
    final Platform platform = Base.getPlatform();

    // check for ANDROID_SDK environment variable
    sdkPath = platform.getenv("ANDROID_SDK");
    if (sdkPath == null) {
      Base
          .showWarning("Android Tools Error",
            "Before using Android mode, you must first set the\n"
                + "ANDROID_SDK environment variable, and restart Processing.",
            null);
      return false;
    }

    // check to make sure the ANDROID_SDK variable is legit
    final File toolsFolder = new File(sdkPath, "tools");
    if (!toolsFolder.exists()) {
      Base.showWarning("Android Tools Error",
        "The ANDROID_SDK environment variable is set incorrectly,\n"
            + "or the directory no longer exists. No tools folder was\n"
            + "found in " + toolsFolder.getAbsolutePath() + ".\n"
            + "Please fix the location and restart Processing.", null);
      return false;
    }

    // make sure that $ANDROID_SDK/tools has been added to the PATH
    try {
      final String canonicalTools = toolsFolder.getCanonicalPath();
      final String envPath = platform.getenv("PATH");
      final String[] entries = PApplet.split(envPath, File.pathSeparatorChar);
      for (String entry : entries) {
        entry = entry.trim();
        if (entry.length() == 0) {
          continue;
        }
        final Matcher m = quotedPathElement.matcher(entry);
        if (m.matches()) {
          entry = m.group(1); // unquote
        }
        final String canonicalEntry;
        try {
          canonicalEntry = new File(entry).getCanonicalPath();
        } catch (final IOException unexpected) {
          System.err.println(unexpected);
          continue;
        }
        if (canonicalEntry.equals(canonicalTools)) {
          if (Base.isWindows()) {
            if (new File(toolsFolder, "android.bat").exists()) {
              toolName = "android.bat";
            } else if (new File(toolsFolder, "android.exe").exists()) {
              toolName = "android.exe";
            }
          }
          return true;
        }
      }
      Base.showWarning("Android Tools Error",
        "You need to add the tools folder of the Android SDK\n"
            + "to your PATH environment variable and restart Processing.\n"
            + "The folder is: " + toolsFolder.getAbsolutePath() + ".", null);
    } catch (final IOException e) {
      Base.showWarning("Android Tools Error",
        "Error while trying to check the PATH for the Android tools.", e);
    }
    return false;
  }

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

  /**
   * Checks a path to see if there's a tools/android file inside, a rough check
   * for the SDK installation. Also figures out the name of android/android.bat
   * so that it can be called explicitly.
   */
  String findAndroidTool(final String path) {
    if (path == null) {
      return null; // Definitely not legit
    }

    final File folder = new File(path);

    if (Base.isWindows()) {
      if (new File(folder, "tools/android.exe").exists()) {
        toolName = "android.exe";
      } else if (new File(folder, "tools/android.bat").exists()) {
        toolName = "android.bat";
      }
    } else if (new File(folder, "tools/android").exists()) {
      toolName = "android";
    }
    return toolName != null ? path : null;
  }

  /**
   * <s>And by "check" server, I mean kill it and start it again. For now, the
   * debug bridge seems to get into a bad state frequently, and it's not clear
   * how to properly query whether that's the case.</s>
   * <p/>
   * On second thought, that's been scratched because the forced start/stop
   * seems to cause even more instability.
   */
  protected boolean checkServer() {
    // when "adb get-state" returns "unknown", that means that the SocketAttach
    // will probably fail, however it happily returns "device" in other cases
    // when the debug bridge is clearly unhappy.
    /*
     * try { System.out.print("adb get state: "); ProcessHelper p = new
     * ProcessHelper(new String[] { "adb", "get-state" }); p.waitFor();
     * p.printLines(); } catch (Exception e) { e.printStackTrace(); }
     */
    return true;
  }

  static protected File getCoreZipFile() {
    // for debugging only, check to see if this is an svn checkout
    File debugFile = new File("../../../android/core.zip");
    if (!debugFile.exists() && Base.isMacOS()) {
      // current path might be inside Processing.app, so need to go much higher
      debugFile = new File("../../../../../../../android/core.zip");
    }
    if (debugFile.exists()) {
      System.out.println("Using version of core.zip from local SVN checkout.");
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

  /**
   * The debug bridge seems to get into a bad state frequently, and it's not
   * clear how to properly query whether that's the case.
   * <p/>
   * For instance, when loading the Android tools and checking installed AVDs,
   * the check will commonly return no entries, even though the defaults have
   * been created. Then the code tries to create the AVDs again, only to return
   * an error.
   * <p/>
   * In other cases, launching connector.attach() from AndroidRunner will simply
   * hang, rather than returning an error or (ever) timing out. (Even if the
   * timeout arg is set for SocketAttach.) There doesn't seem to be a good way
   * to query whether this is going to happen before it happens.
   */
  static protected boolean resetServer(final Editor editor) {
    try {
      final ProcessResult killResult = KILL_SERVER_COMMAND.execute();
      Thread.sleep(1000); // just take a quick break so that the server can die

      // ...we only care about whether it was able to start successfully.
      final ProcessResult startResult = START_SERVER_CMD.execute();
      if (startResult.succeeded()) {
        return true;
      }
      System.err.println(killResult.getStderr()); // okay maybe now we care about these
      System.err.println(startResult.getStderr()); // something to confuse the user a bit
      editor.statusError("Could not start Android debug server.");
    } catch (final IOException e) {
      editor.statusError(e);
    } catch (final InterruptedException e) {
    }
    return false;
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

  /**
   * Launch an emulator if not already running.
   * 
   * @return The name of the emulator.
   */
  public String findEmulator() {
    if (emulatorProcess != null) {
      // see if it's done yet
      try {
        // int result = emulatorProcess.exitValue();
        // emulatorProcess.destroy();
        emulatorProcess.exitValue();
        emulatorProcess = null;
      } catch (final IllegalThreadStateException itse) {
        // not done yet, let's continue
      }
    }

    String portString = Preferences.get("android.emulator.port");
    if (portString == null) {
      portString = "5566";
      Preferences.set("android.emulator.port", portString);
    }
    // int port = Integer.parseInt(portString);
    final String name = "emulator-" + portString;

    if (emulatorProcess != null) { // this must be it, if we're running
      System.out.println("Found a perfectly good emulator. Trying that.");
      return name;
    }

    try {
      final String[] devices = Device.list();
      for (final String s : devices) {
        if (s.equals(name)) {
          return name;
        }
      }
    } catch (final IOException e) {
      // e.printStackTrace();
      editor.statusError(e);
      return null;
    }

    // # starts and uses port 5554 for communication (but not logs)
    // emulator -avd gee1 -port 5554
    // # only informative messages and up (emulator -help-logcat for more info)
    // emulator -avd gee1 -logcat '*:i'
    // # faster boot
    // emulator -avd gee1 -logcat '*:i' -no-boot-anim
    // # only get System.out and System.err
    // emulator -avd gee1 -logcat 'System.*:i' -no-boot-anim
    // # though lots of messages aren't through System.*, so that's not great
    // # need to instead use the adb interface

    // launch emulator because it's not running yet
    try {
      editor.statusNotice("Starting new Android emulator.");
      final String[] cmd = new String[] {
        "emulator", "-avd", getDefaultDevice(), "-port", portString,
        // "-logcat", "'*:i'",
        "-no-boot-anim" };
      emulatorProcess = Runtime.getRuntime().exec(cmd);
      System.out.println(PApplet.join(cmd, " "));
      // "emulator: ERROR: the user data image is used by another emulator. aborting"
      // make sure that the streams are drained properly
      new StreamRedirectThread("android-emulator-out", emulatorProcess
          .getInputStream(), System.out).start();
      new StreamRedirectThread("android-emulator-err", emulatorProcess
          .getErrorStream(), System.err).start();
      return name;

    } catch (final IOException e) {
      // e.printStackTrace();
      editor.statusError(e);
      return null;
    }
  }

  public String getDefaultDevice() {
    // return Device.avdDonut.name;
    return Device.avdEclair.name;
  }

  /** Find connected devices */
  public String findDevice() {
    String[] devices;
    try {
      devices = Device.list();
    } catch (final IOException e) {
      editor.statusError(e);
      // e.printStackTrace();
      return null;
    }
    // ArrayList<String> available = new ArrayList<String>();
    // for (String s : devices) {
    // }
    // just go with the first non-emulator device
    for (final String name : devices) {
      if (!name.startsWith("emulator-")) {
        return name;
      }
    }
    return null;
  }

  // adb -s emulator-5556 install helloWorld.apk

  // : adb -s HT91MLC00031 install bin/Brightness-debug.apk
  // 532 KB/s (190588 bytes in 0.349s)
  // pkg: /data/local/tmp/Brightness-debug.apk
  // Failure [INSTALL_FAILED_ALREADY_EXISTS]

  // : adb -s HT91MLC00031 install -r bin/Brightness-debug.apk
  // 1151 KB/s (190588 bytes in 0.161s)
  // pkg: /data/local/tmp/Brightness-debug.apk
  // Success

  // safe to just always include the -r (reinstall) flag

  // if user asks for 480x320, 320x480, 854x480 etc, then launch like that
  // though would need to query the emulator to see if it can do that

  public void installAndRun(final String target, final String device) {
    boolean success;

    // //adb get-state
    // try {
    // System.out.print("(installAndRun) adb get state: ");
    // ProcessHelper p = new ProcessHelper(new String[] { "adb", "get-state" });
    // p.waitFor();
    // p.printLines();
    // } catch (Exception e) {
    // e.printStackTrace();
    // }

    // Simply reset the debug bridge, since it seems so prone to getting
    // into bad states and not producing error messages.
    // resetServer();

    final Build build = getBuilder();
    success = build.createProject();
    if (!success) {
      return;
    }

    /*
     * <!-- Compile this project's .java files into .class files. --> <target
     * name="compile" depends="resource-src, aidl"> <javac encoding="ascii"
     * target="1.5" debug="true" extdirs="" destdir="${out-classes}"
     * bootclasspathref="android.target.classpath"> <src path="${source-folder}"
     * /> <src path="${gen-folder}" /> <classpath> <fileset
     * dir="${external-libs-folder}" includes="*.jar"/> <pathelement
     * path="${main-out-classes}"/> </classpath> </javac> </target>
     */
    // first build the code (.java -> .class) separately
    // success = build.antBuild("compile");
    // PApplet.println("umm compile");
    // success = build.execAntCompile();
    // PApplet.println("umm yeah compile");
    // now run the ant debug or release version
    success = build.antBuild("debug");
    // System.out.println("ant build complete " + success);
    if (!success) {
      return;
    }

    success = waitUntilReady(device);
    if (!success) {
      return;
    }

    success = installSketch(device);
    if (!success) {
      return;
    }

    // Returns the last JDWP port that in use before launching
    final String prevPort = startSketch(device);
    if (prevPort == null) {
      return;
    }
    new AndroidRunner(editor, editor.getSketch()).launch(ADB_SOCKET_PORT);
    //success = debugSketch(device, prevPort);
  }

  protected boolean waitUntilReady(final String device) {
    System.err.println("Waiting until " + device + " is ready.");
    final long timeout = System.currentTimeMillis() + 30 * 1000;
    try {
      while (System.currentTimeMillis() < timeout) {
        // adb -s emulator-5566 jdwp
        // prints a list of connections that can be made to the device
        // the final port will be the entry of the most recently started
        // application
        // when not running, will say 'error: device not found'

        // System.out.print("Checking for JDWP connection: ");
        final ProcessHelper p = new ProcessHelper("adb", "-s", device, "jdwp");
        final ProcessResult jdwpResult = p.execute();
        if (jdwpResult.succeeded()) {
          System.err.println(device + " is seemingly ready.");
          return true;
        }
        // while launching, will say 'error: device offline'
        if (!jdwpResult.getStderr().contains("device offline")) {
          System.err.println(jdwpResult);
        }
        Thread.sleep(1000);
      }
    } catch (final InterruptedException e) {
      System.err.println("waitUntilReady interrupted");
    } catch (final Exception e) {
      e.printStackTrace();
    }
    System.err.println("Timed out waiting for " + device + ".");
    return false;
  }

  //  protected String getJdwpPort(final String device) {
  //    final long timeout = System.currentTimeMillis() + 30 * 1000;
  //
  //    try {
  //      while (System.currentTimeMillis() < timeout) {
  //        // adb -s emulator-5566 jdwp
  //        // prints a list of connections that can be made to the device
  //        // the final port will be the entry of the most recently started
  //        // application
  //        // while launching, will say 'error: device offline'
  //        // when not running, will say 'error: device not found'
  //
  //        final ProcessHelper p = new ProcessHelper("adb", "-s", device, "jdwp");
  //        if (p.execute() == 0) {
  //          final String[] lines = p.getStdout().split("\n");
  //          for (int i = lines.length - 1; i >= 0; --i) {
  //            final String s = lines[i].trim();
  //            if (s.length() != 0) {
  //              return s;
  //            }
  //          }
  //          return null;
  //        }
  //        p.dump();
  //        try {
  //          Thread.sleep(1000);
  //        } catch (final InterruptedException ie) {
  //        }
  //      }
  //    } catch (final Exception e) {
  //      e.printStackTrace();
  //    }
  //    return null;
  //  }

  boolean installSketch(final String device) {
    // install the new package into the emulator
    // System.out.println("installing onto emulator/device");
    final boolean emu = device.startsWith("emulator");
    editor.statusNotice("Sending sketch to the " + (emu ? "emulator" : "phone")
        + ".");
    try {
      final ProcessHelper p = new ProcessHelper("adb", "-s", device, // "wait-for-device",
                                                "install", "-r", // safe to always use -r switch
                                                build.getPathForAPK("debug"));

      final ProcessResult installResult = p.execute();
      if (!installResult.succeeded()) {
        editor.statusError("Could not install the sketch.");
        System.err.println(installResult);
        return false;

      }
      String errorMsg = null;
      for (final String out : installResult) {
        if (out.startsWith("Failure")) {
          errorMsg = out.substring(8);
          System.err.println(out);
        } else {
          System.out.println(out);
        }
      }
      if (errorMsg == null) {
        editor.statusNotice("Done installing.");
        return true;
      }
      editor.statusError("Error while installing " + errorMsg);
    } catch (final IOException e) {
      editor.statusError(e);
    } catch (final InterruptedException e) {
      System.err.println("installSketch interrupted");
    }
    return false;
  }

  // better version that actually runs through JDI:
  // http://asantoso.wordpress.com/2009/09/26/using-jdb-with-adb-to-debugging-of-android-app-on-a-real-device/
  String startSketch(final String device) {
    try {
      //      final String lastPort = getJdwpPort(device);

      // "am start -a android.intent.action.MAIN -n com.android.browser/.BrowserActivity"
      final ProcessResult result = new ProcessHelper(
                                                     "adb",
                                                     "-s",
                                                     device,
                                                     // "-d", // this is for a single USB device
                                                     "shell",
                                                     "am",
                                                     "start", // kick things off
                                                     // -D causes a hang with
                                                     // "waiting for the debugger to attach"
                                                     // "-D", // debug
                                                     "-e",
                                                     "debug",
                                                     "true",
                                                     "-a",
                                                     "android.intent.action.MAIN",
                                                     "-c",
                                                     "android.intent.category.LAUNCHER",
                                                     "-n", build
                                                         .getPackageName()
                                                         + "/."
                                                         + build.getClassName())
          .execute();
      if (result.succeeded()) {
        final boolean emu = device.startsWith("emulator");
        editor.statusNotice("Sketch started on the "
            + (emu ? "emulator" : "phone") + ".");
        return "";//lastPort;
      }
      editor.statusError("Could not start the sketch.");
      System.err.println(result);
    } catch (final IOException e) {
      editor.statusError(e);
    } catch (final InterruptedException e) {
      e.printStackTrace();
    }
    return null;
  }

  //  boolean debugSketch(final String device, final String prevPort) {
  //    try {
  //      String port = null;
  //      final long timeout = System.currentTimeMillis() + 15 * 1000;
  //      // while (port == null || port.equals(prevPort)) {
  //      while (System.currentTimeMillis() < timeout) {
  //        if (port != null) {
  //          System.out.println("Waiting a half second "
  //              + "for the application to launch...");
  //          try {
  //            Thread.sleep(500);
  //          } catch (final InterruptedException ie) {
  //          }
  //        }
  //        port = getJdwpPort(device);
  //        if (!port.equals(prevPort)) {
  //          // System.out.println("I'm digging port " + port +
  //          // " instead of " + prevPort + ".");
  //          break;
  //        }
  //      }
  //      System.out.println("Found application on port " + port + ".");
  //
  //      // Originally based on helpful notes by Agus Santoso (http://j.mp/7zV69M)
  //
  //      // adb -s emulator-5566 jdwp
  //      // prints a list of connections that can be made to the device
  //      // the final port will be the entry of the most recently started
  //      // application
  //      // while launching, will say 'error: device offline'
  //      // when not running, will say 'error: device not found'
  //
  //      // PApplet.println(cmd);
  //      final ProcessHelper fwd = new ProcessHelper("adb", "-s", device,
  //                                                  "forward", "tcp:"
  //                                                      + ADB_SOCKET_PORT,
  //                                                  "jdwp:" + port);
  //
  //      // System.out.println("waiting for forward");
  //      System.err.println(fwd.getCommand());
  //      if (fwd.execute(true) != 0) {
  //        editor.statusError("Could not connect for debugging.");
  //        return false;
  //      }
  //
  //      System.err.println("creating runner");
  //      // System.out.println("editor from Android is " + editor);
  //      final AndroidRunner ar = new AndroidRunner(editor, editor.getSketch());
  //      // System.out.println("launching vm");
  //      return ar.launch(ADB_SOCKET_PORT);
  //      // System.out.println("vm launched");
  //
  //    } catch (final IOException e) {
  //      editor.statusError(e);
  //    } catch (final InterruptedException e) {
  //    }
  //
  //    return false;
  //  }

  /**
   * Build the sketch and run it inside an emulator with the debugger.
   */
  class RunHandler implements Runnable {
    public void run() {
      // installAndRun("debug", findEmulator());
      checkServer();
      final String device = findEmulator();
      boolean success;

      final Build build = getBuilder();
      success = build.createProject();
      if (!success) {
        return;
      }

      // now run the ant debug or release version
      success = build.antBuild("debug");
      // System.out.println("ant build complete " + success);
      if (!success) {
        return;
      }

      success = waitUntilReady(device);
      if (!success) {
        return;
      }

      success = installSketch(device);
      if (!success) {
        return;
      }

      // Returns the last JDWP port that in use before launching
      final String prevPort = startSketch(device);
      if (prevPort == null) {
        return;
      }
      new AndroidRunner(editor, editor.getSketch()).launch(ADB_SOCKET_PORT);
      //success = debugSketch(device, prevPort);
    }
  }

  /**
   * Build the sketch and run it on a device with the debugger connected.
   */
  class PresentHandler implements Runnable {
    public void run() {
      checkServer();
      final String device = findDevice();
      if (device == null) {
        editor.statusError("No device found.");
      } else {
        // installAndRun("debug", device);
        boolean success;

        final Build build = getBuilder();
        success = build.createProject();
        if (!success) {
          return;
        }

        // now run the ant debug or release version
        success = build.antBuild("debug");
        // System.out.println("ant build complete " + success);
        if (!success) {
          return;
        }

        success = waitUntilReady(device);
        if (!success) {
          return;
        }

        success = installSketch(device);
        if (!success) {
          return;
        }

        // Returns the last JDWP port that in use before launching
        final String prevPort = startSketch(device);
        if (prevPort == null) {
          return;
        }

        //success = debugSketch(device, prevPort);
      }
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