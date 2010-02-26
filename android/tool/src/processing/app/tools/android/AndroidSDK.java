package processing.app.tools.android;

import java.io.File;

public class AndroidSDK {
  private final File sdk;
  private final File tools;
  private final File androidTool;

  public AndroidSDK(final String sdkPath) throws BadSDKException {
    sdk = new File(sdkPath);
    if (!sdk.exists()) {
      throw new BadSDKException(sdk + " does not exist");
    }

    tools = new File(sdk, "tools");
    if (!tools.exists()) {
      throw new BadSDKException("There is no tools folder in " + sdk);
    }

    androidTool = findAndroidTool(tools);
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
}
