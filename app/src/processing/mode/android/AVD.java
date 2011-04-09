package processing.mode.android;

import java.io.IOException;
import java.util.ArrayList;

import processing.app.Base;
import processing.app.exec.ProcessHelper;
import processing.app.exec.ProcessResult;
import processing.core.PApplet;


public class AVD {
  private static final String AVD_CREATE_ERROR =
    "An error occurred while running “android create avd”\n" +
    "to set up the default Android emulator. Make sure that the\n" +
    "Android SDK is installed properly, and that the Android\n" +
    "and Google APIs are installed for level " + AndroidBuild.sdkVersion + ".\n" +
    "(Between you and me, occasionally, this error is a red herring,\n" + 
    "and your sketch may be launching shortly.)";

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
//  static ArrayList<String> skinList;


  public AVD(final String name, final String target) {
    this.name = name;
    this.target = target;
  }


  static protected void list(final AndroidSDK sdk) throws IOException {
    try {
      avdList = new ArrayList<String>();
      ProcessResult listResult =
        new ProcessHelper(sdk.getAndroidToolPath(), "list", "avds").execute();
      if (listResult.succeeded()) {
        for (String line : listResult) {
          String[] m = PApplet.match(line, "\\s+Name\\:\\s+(\\S+)");
          if (m != null) {
//            System.out.println("Found AVD " + m[1]);
            avdList.add(m[1]);
//            if (m[1].equals(name)) {
//              return true;
//            }
          }
          // "The following Android Virtual Devices could not be loaded:"
          if (line.contains("could not be loaded:")) {
//            System.err.println("Could not list AVDs:");
            System.err.println(listResult);
            break;
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


  protected boolean create(final AndroidSDK sdk) throws IOException {
    final String[] params = {
      sdk.getAndroidToolPath(),
      "create", "avd",
      "-n", name, "-t", target,
      "-c", "64M",
      "-s", DEFAULT_SKIN
//      "-s", DEFAULT_WIDTH + "x" + DEFAULT_HEIGHT
    };
    
//    throw new RuntimeException("avd.create() not currently working");
//    final StringWriter outWriter = new StringWriter();
//    final StringWriter errWriter = new StringWriter();
//    final long startTime = System.currentTimeMillis();
//
//    final String prettyCommand = toString();
//    //    System.err.println("ProcessHelper: >>>>> " + Thread.currentThread().getId()
//    //        + " " + prettyCommand);
//    final Process process = Runtime.getRuntime().exec(cmd);
//    ProcessRegistry.watch(process);
//    try {
//      String title = PApplet.join(cmd, ' '); 
//      new StreamPump(process.getInputStream(), "out: " + title).addTarget(outWriter).start();
//      new StreamPump(process.getErrorStream(), "err: " + title).addTarget(errWriter).start();
//      try {
//        final int result = process.waitFor();
//        final long time = System.currentTimeMillis() - startTime;
//        //        System.err.println("ProcessHelper: <<<<< "
//        //            + Thread.currentThread().getId() + " " + cmd[0] + " (" + time
//        //            + "ms)");
//        return new ProcessResult(prettyCommand, result, outWriter.toString(),
//                                 errWriter.toString(), time);
//      } catch (final InterruptedException e) {
//        System.err.println("Interrupted: " + prettyCommand);
//        throw e;
//      }
//    } finally {
//      process.destroy();
//      ProcessRegistry.unwatch(process);
//    }
    
    final ProcessHelper p = new ProcessHelper(params);
    try {
      final ProcessResult createAvdResult = p.execute();
      if (createAvdResult.succeeded()) {
        return true;
      }
      System.err.println(createAvdResult);
    } catch (final InterruptedException ie) { }

    return false;
  }


  static public boolean ensureEclairAVD(final AndroidSDK sdk) {
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
}
