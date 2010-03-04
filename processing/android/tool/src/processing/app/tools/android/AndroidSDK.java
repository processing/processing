package processing.app.tools.android;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;
import processing.app.Base;
import processing.app.Platform;
import processing.app.Preferences;

class AndroidSDK {
  private final File sdk;
  private final File tools;
  private final File androidTool;

  public AndroidSDK(final String sdkPath) throws BadSDKException, IOException {
    sdk = new File(sdkPath);
    if (!sdk.exists()) {
      throw new BadSDKException(sdk + " does not exist");
    }

    tools = new File(sdk, "tools");
    if (!tools.exists()) {
      throw new BadSDKException("There is no tools folder in " + sdk);
    }

    androidTool = findAndroidTool(tools);

    final Platform p = Base.getPlatform();

    String path = p.getenv("PATH");

    p.setenv("ANDROID_SDK", sdk.getCanonicalPath());
    path = tools.getCanonicalPath() + File.pathSeparator + path;

    final String javaHomeProp = System.getProperty("java.home");
    if (javaHomeProp == null) {
      throw new RuntimeException(
                                 "I don't know how to deal with a null java.home proprty, to be quite frank.");
    }
    final File javaHome = new File(javaHomeProp).getCanonicalFile();
    p.setenv("JAVA_HOME", javaHome.getCanonicalPath());

    path = new File(javaHome, "bin").getCanonicalPath() + File.pathSeparator
        + path;

    p.setenv("PATH", path);
  }

  public File getAndroidTool() {
    return androidTool;
  }

  public File getSdk() {
    return sdk;
  }

  public File getTools() {
    return tools;
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
   * Check for the ANDROID_SDK environment variable. If the variable is set, and refers
   * to a legitimate android SDK, then use that, and save the preference.
   * 
   * Check for a previously set android.sdk.path preference. If the pref is set, and refers
   * to a legitimate android SDK, then use that.
   * 
   * Prompt the user to select an android SDK. If the user selects a legitimate
   * android SDK, then use that, and save the preference.
   * 
   * @return an AndroidSDK
   * @throws BadSDKException
   * @throws IOException 
   */
  public static AndroidSDK find(final Frame window) throws BadSDKException,
      IOException {
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
      throw new BadSDKException("User cancelled attempt to find SDK.");
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

  private static final String ANDROID_SDK_PRIMARY = "Is the Android SDK installed?";

  private static final String ANDROID_SDK_SECONDARY = "The Android SDK does not appear to be installed, <br>"
      + "because the ANDROID_SDK variable is not set. <br>"
      + "If it is installed, click “Yes” to select the <br>"
      + "location of the SDK, or “No” to visit the SDK<br>"
      + "download site at http://developer.android.com/sdk.";

  private static final String SELECT_ANDROID_SDK_FOLDER = "Choose the location of the Android SDK";

  private static final String NOT_ANDROID_SDK = "The selected folder does not appear to contain an Android SDK.";

  private static final String ANDROID_SDK_URL = "http://developer.android.com/sdk/";
}
