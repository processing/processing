package processing.mode.android;

import java.io.IOException;
import java.util.ArrayList;

import processing.app.Base;
import processing.app.exec.ProcessHelper;
import processing.app.exec.ProcessResult;
import processing.core.PApplet;


public class AVD {
  static private final String AVD_CREATE_ERROR =
    "An error occurred while running “android create avd”\n" +
    "to set up the default Android emulator. Make sure that the\n" +
    "Android SDK is installed properly, and that the Android\n" +
    "and Google APIs are installed for level " + AndroidBuild.sdkVersion + ".\n" +
    "(Between you and me, occasionally, this error is a red herring,\n" + 
    "and your sketch may be launching shortly.)";

  static private final String AVD_CANNOT_LOAD =
    "There is an error with the Processing AVD. This could mean that the\n" + 
    "Android tools need to be updated, or that the Processing AVD should\n" +
    "be deleted (it will automatically re-created the next time you run\n" +
    "Processing). Open the Android SDK manager to check for any errors.";
  
  static private final String AVD_TARGET_MISSING = 
    "The Google APIs are not installed properly, please re-read\n" +
    "the installation instructions for Android Processing from\n" + 
    "http://android.processing.org and try again.";

  static final String DEFAULT_SKIN = "WVGA800";

  /** Name of this avd. */
  protected String name;

  /** "android-7" or "Google Inc.:Google APIs:7" */
  protected String target;

  /** Default virtual device used by Processing. */
  static public final AVD defaultAVD =
    new AVD(//"Processing-Android-" + AndroidBuild.sdkVersion,
            "Processing-0" + Base.REVISION,
            "Google Inc.:Google APIs:" + AndroidBuild.sdkVersion);
            //AndroidBuild.sdkTarget);
  
  static ArrayList<String> avdList;
  static ArrayList<String> badList;
//  static ArrayList<String> skinList;


  public AVD(final String name, final String target) {
    this.name = name;
    this.target = target;
  }


  static protected void list(final AndroidSDK sdk) throws IOException {
    try {
      avdList = new ArrayList<String>();
      badList = new ArrayList<String>();
      ProcessResult listResult =
        new ProcessHelper(sdk.getAndroidToolPath(), "list", "avds").execute();
      if (listResult.succeeded()) {
        boolean badness = false;
        for (String line : listResult) {
          String[] m = PApplet.match(line, "\\s+Name\\:\\s+(\\S+)");
          if (m != null) {
            if (!badness) {
//              System.out.println("good: " + m[1]);
              avdList.add(m[1]);
            } else {
//              System.out.println("bad: " + m[1]);
              badList.add(m[1]);
            }
//          } else {
//            System.out.println("nope: " + line);
          }
          // "The following Android Virtual Devices could not be loaded:"
          if (line.contains("could not be loaded:")) {
//            System.out.println("starting the bad list");
//            System.err.println("Could not list AVDs:");
//            System.err.println(listResult);
            badness = true;
//            break;
          }
        }
      } else {
        System.err.println("Unhappy inside exists()");
        System.err.println(listResult);
      }
    } catch (final InterruptedException ie) { }
  }


  protected boolean exists(final AndroidSDK sdk) throws IOException {
    if (avdList == null) {
      list(sdk);
    }
    for (String avd : avdList) {
      if (avd.equals(name)) {
        return true;
      }
    }
    return false;
  }
  

  /** Return true if this AVD was on the bad list. */
  protected boolean badness() {
    for (String avd : badList) {
      if (avd.equals(name)) {
        return true;
      }
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
    };
    
    final ProcessHelper p = new ProcessHelper(params);
    try {
      final ProcessResult createAvdResult = p.execute();
      if (createAvdResult.succeeded()) {
        return true;
      }
      if (createAvdResult.toString().contains("Target id is not valid")) {
        // They didn't install the Google APIs
        Base.showWarning("Android Error", AVD_TARGET_MISSING, null);
//        throw new IOException("Missing required SDK components");
      } else {
        // Just generally not working
        Base.showWarning("Android Error", AVD_CREATE_ERROR, null);
//        throw new IOException("Error creating the AVD");
      }
      //System.err.println(createAvdResult);
    } catch (final InterruptedException ie) { }

    return false;
  }


  static public boolean ensureEclairAVD(final AndroidSDK sdk) {
    try {
      if (defaultAVD.exists(sdk)) {
//        System.out.println("the avd exists");
        return true;
      }
      if (defaultAVD.badness()) {
        Base.showWarning("Android Error", AVD_CANNOT_LOAD, null);
        return false;
      }
      if (defaultAVD.create(sdk)) {
//        System.out.println("the avd was created");
        return true;
      }
    } catch (final Exception e) {
      Base.showWarning("Android Error", AVD_CREATE_ERROR, e);
    }
    return false;
  }
}
