package processing.app.tools.android;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import processing.app.Base;
import processing.app.exec.ProcessHelper;
import processing.app.exec.ProcessResult;

public class AVD {
  // Tempting to switch to WVGA854 (854x480), the same aspect ratio
  // (with rounding), as 1920x1080, or 16:9.
//  static final int DEFAULT_WIDTH = 320;
//  static final int DEFAULT_HEIGHT = 480;
//  static final int DEFAULT_WIDTH = 480;
//  static final int DEFAULT_HEIGHT = 800;
  static final String DEFAULT_SKIN = "WVGA800";

  /** Name of this avd. */
  protected String name;

  /** "android-7" or "Google Inc.:Google APIs:7" */
  protected String target;

  /**
   * Default virtual device used by Processing.
   */
  public static final AVD defaultAVD = 
    new AVD("Processing-Android-" + Build.sdkVersion,
            "Google Inc.:Google APIs:" + Build.sdkVersion);

  public static boolean ensureEclairAVD(final AndroidSDK sdk) {
    try {
      if (defaultAVD.exists(sdk)) {
//        System.out.println("the avd exists");
        return true;
      }
      if (defaultAVD.create(sdk)) {
//        System.out.println("the avd was created");
        return true;
      }
      Base.showWarning("Android Error", AVD_CREATE_ERROR, null);
    } catch (final Exception e) {
      Base.showWarning("Android Error", AVD_CREATE_ERROR, e);
    }
    return false;
  }


  public AVD(final String name, final String target) {
    this.name = name;
    this.target = target;
  }


  private static final Pattern AVD_ROW = Pattern.compile("\\s+Name:\\s+(\\S+)");

  protected boolean exists(final AndroidSDK sdk) throws IOException {
    try {
      ProcessResult listResult = 
        new ProcessHelper(sdk.getAndroidToolPath(), "list", "avds").execute();
      if (listResult.succeeded()) {
        for (String line : listResult) {
          final Matcher m = AVD_ROW.matcher(line);
          if (m.matches() && m.group(1).equals(name)) {
            return true;
          }
        }
      } else {
        System.err.println("Unhappy inside exists()");
        System.err.println(listResult);
      }
    } catch (final InterruptedException ie) {
    }
    return false;
  }


  protected boolean create(final AndroidSDK sdk) throws IOException {
    final String[] params = {
      sdk.getAndroidToolPath(),
      "create", "avd", 
      "-n", name, "-t", target, 
      "-c", "64M",
      "-s", DEFAULT_SKIN
//      "-s", DEFAULT_WIDTH + "x" + DEFAULT_HEIGHT
    };
    final ProcessHelper p = new ProcessHelper(params); 
    try {
      final ProcessResult createAvdResult = p.execute();
      if (createAvdResult.succeeded()) {
        return true;
      }
      System.err.println(createAvdResult);
    } catch (final InterruptedException ie) {
    }
    return false;
  }

  private static final String AVD_CREATE_ERROR = "An error occurred while running “android create avd”\n"
      + "to set up the default Android emulator. Make sure that the\n"
      + "Android SDK is installed properly, and that the Android\n"
      + "and Google APIs are installed for level " + Build.sdkVersion + ".";

}
