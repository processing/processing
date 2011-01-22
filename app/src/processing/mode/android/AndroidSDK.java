package processing.mode.android;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;

import processing.app.Base;
import processing.app.Platform;
import processing.app.Preferences;
import processing.app.exec.ProcessHelper;
import processing.app.exec.ProcessResult;
import processing.core.PApplet;

class AndroidSDK {
  private final File folder;
  private final File tools;
  private final File platformTools;
  private final File androidTool;

  private static final String ANDROID_SDK_PRIMARY =
    "Is the Android SDK installed?";

  private static final String ANDROID_SDK_SECONDARY =
    "The Android SDK does not appear to be installed, <br>" +
    "because the ANDROID_SDK variable is not set. <br>" +
    "If it is installed, click “Yes” to select the <br>" +
    "location of the SDK, or “No” to visit the SDK<br>" +
    "download site at http://developer.android.com/sdk.";

  private static final String SELECT_ANDROID_SDK_FOLDER =
    "Choose the location of the Android SDK";

  private static final String NOT_ANDROID_SDK =
    "The selected folder does not appear to contain an Android SDK.";

  private static final String ANDROID_SDK_URL =
    "http://developer.android.com/sdk/";


  public AndroidSDK(final String sdkPath) throws BadSDKException, IOException {
    folder = new File(sdkPath);
    if (!folder.exists()) {
      throw new BadSDKException(folder + " does not exist");
    }

    tools = new File(folder, "tools");
    if (!tools.exists()) {
      throw new BadSDKException("There is no tools folder in " + folder);
    }

    platformTools = new File(folder, "platform-tools");
    if (!platformTools.exists()) {
      throw new BadSDKException("There is no platform-tools folder in " + folder);
    }

    androidTool = findAndroidTool(tools);

    final Platform p = Base.getPlatform();

    String path = p.getenv("PATH");

    p.setenv("ANDROID_SDK", folder.getCanonicalPath());
    path = platformTools.getCanonicalPath() + File.pathSeparator +
      tools.getCanonicalPath() + File.pathSeparator + path;

    final String javaHomeProp = System.getProperty("java.home");
    if (javaHomeProp == null) {
      throw new RuntimeException("I don't know how to deal with " +
                "a null java.home proprty, to be quite frank.");
    }
    final File javaHome = new File(javaHomeProp).getCanonicalFile();
    p.setenv("JAVA_HOME", javaHome.getCanonicalPath());

    path = new File(javaHome, "bin").getCanonicalPath() + File.pathSeparator + path;

    p.setenv("PATH", path);
  }


  public File getAndroidTool() {
    return androidTool;
  }


  public String getAndroidToolPath() {
    return androidTool.getAbsolutePath();
  }


  public File getSdkFolder() {
    return folder;
  }


  /*
  public File getToolsFolder() {
    return tools;
  }
  */


  public File getPlatformToolsFolder() {
    return platformTools;
  }


  /**
   * Checks a path to see if there's a tools/android file inside, a rough check
   * for the SDK installation. Also figures out the name of android/android.bat
   * so that it can be called explicitly.
   */
  private static File findAndroidTool(final File tools) throws BadSDKException {
    if (new File(tools, "android.exe").exists()) {
      return new File(tools, "android.exe");
    }
    if (new File(tools, "android.bat").exists()) {
      return new File(tools, "android.bat");
    }
    if (new File(tools, "android").exists()) {
      return new File(tools, "android");
    }
    throw new BadSDKException("Cannot find the android tool in " + tools);
  }


  /**
   * Check for the ANDROID_SDK environment variable. If the variable is set,
   * and refers to a legitimate Android SDK, then use that and save the pref.
   *
   * Check for a previously set android.sdk.path preference. If the pref
   * is set, and refers to a legitimate Android SDK, then use that.
   *
   * Prompt the user to select an Android SDK. If the user selects a
   * legitimate Android SDK, then use that, and save the preference.
   *
   * @return an AndroidSDK
   * @throws BadSDKException
   * @throws IOException
   */
  public static AndroidSDK find(final Frame window)
  throws BadSDKException, IOException {
    final Platform platform = Base.getPlatform();

    // The environment variable is king. The preferences.txt entry is a page.
    final String sdkEnvPath = platform.getenv("ANDROID_SDK");
    if (sdkEnvPath != null) {
      try {
        final AndroidSDK androidSDK = new AndroidSDK(sdkEnvPath);
        // Set this value in preferences.txt, in case ANDROID_SDK
        // gets knocked out later. For instance, by that pesky Eclipse,
        // which nukes all env variables when launching from the IDE.
        Preferences.set("android.sdk.path", sdkEnvPath);
        return androidSDK;
      } catch (final BadSDKException drop) {
      }
    }

    // If android.sdk.path exists as a preference, make sure that the folder
    // is not bogus, otherwise the SDK may have been removed or deleted.
    final String sdkPrefsPath = Preferences.get("android.sdk.path");
    if (sdkPrefsPath != null) {
      try {
        final AndroidSDK androidSDK = new AndroidSDK(sdkPrefsPath);
        // Set this value in preferences.txt, in case ANDROID_SDK
        // gets knocked out later. For instance, by that pesky Eclipse,
        // which nukes all env variables when launching from the IDE.
        Preferences.set("android.sdk.path", sdkPrefsPath);
        return androidSDK;
      } catch (final BadSDKException wellThatsThat) {
        Preferences.unset("android.sdk.path");
      }
    }

    final int result = Base.showYesNoQuestion(window, "Android SDK",
      ANDROID_SDK_PRIMARY, ANDROID_SDK_SECONDARY);
    if (result == JOptionPane.CANCEL_OPTION) {
      throw new BadSDKException("User canceled attempt to find SDK.");
    }
    if (result == JOptionPane.NO_OPTION) {
      // user admitted they don't have the SDK installed, and need help.
      Base.openURL(ANDROID_SDK_URL);
      throw new BadSDKException("No SDK installed.");
    }
    while (true) {
      final File folder = Base.selectFolder(SELECT_ANDROID_SDK_FOLDER, null,
        window);
      if (folder == null) {
        throw new BadSDKException("User cancelled attempt to find SDK.");
      }

      final String selectedPath = folder.getAbsolutePath();
      try {
        final AndroidSDK androidSDK = new AndroidSDK(selectedPath);
        Preferences.set("android.sdk.path", selectedPath);
        return androidSDK;
      } catch (final BadSDKException nope) {
        JOptionPane.showMessageDialog(window, NOT_ANDROID_SDK);
      }
    }
  }


  private static final String ADB_DAEMON_MSG_1 = "daemon not running";
  private static final String ADB_DAEMON_MSG_2 = "daemon started successfully";

  public static ProcessResult runADB(final String... cmd)
  throws InterruptedException, IOException {
    final String[] adbCmd;
    if (!cmd[0].equals("adb")) {
      adbCmd = PApplet.splice(cmd, "adb", 0);
    } else {
      adbCmd = cmd;
    }
    // printing this here to see if anyone else is killing the adb server
    if (processing.app.Base.DEBUG) {
      PApplet.println(adbCmd);
    }
//    try {
    ProcessResult adbResult = new ProcessHelper(adbCmd).execute();
    // Ignore messages about starting up an adb daemon
    String out = adbResult.getStdout();
    if (out.contains(ADB_DAEMON_MSG_1) && out.contains(ADB_DAEMON_MSG_2)) {
      StringBuilder sb = new StringBuilder();
      for (String line : out.split("\n")) {
        if (!out.contains(ADB_DAEMON_MSG_1) &&
            !out.contains(ADB_DAEMON_MSG_2)) {
          sb.append(line).append("\n");
        }
      }
      return new ProcessResult(adbResult.getCmd(),
                               adbResult.getResult(),
                               sb.toString(),
                               adbResult.getStderr(),
                               adbResult.getTime());
    }
    return adbResult;
//    } catch (IOException ioe) {
//      ioe.printStackTrace();
//      throw ioe;
//    }
  }
}
