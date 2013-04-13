/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2008-12 Ben Fry and Casey Reas

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package processing.mode.android;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import processing.app.Base;
import processing.app.Preferences;
import processing.app.RunnerListener;
import processing.app.Sketch;
import processing.app.SketchException;
import processing.app.contrib.ModeContribution;

/**
 * Class to handle running Android mode of Processing from the command line.
 * 
 * @author ostap.andrusiv
 */
public class Commander implements RunnerListener {
  static final String helpArg = "--help";
  static final String buildArg = "--build";
  static final String runArg = "--run";
  static final String runArg_DEVICE = "d";
  static final String runArg_EMULATOR = "e";
  static final String targetArg = "--target";
  static final String targetArg_DEBUG = "debug";
  static final String targetArg_RELEASE = "release";
  static final String sketchArg = "--sketch=";
  static final String forceArg = "--force";
  static final String outputArg = "--output=";
  static final String exportApplicationArg = "--export";

  static final int HELP = -1;
  static final int BUILD = 1;
  static final int RUN = 2;
  static final int EXPORT = 4;

  private AndroidMode androidMode = null;

  private int task = HELP;

  private Sketch sketch;
  private PrintStream systemOut;
  private PrintStream systemErr;

  private String sketchPath = null;
  private File sketchFolder = null;
  private String pdePath = null; // path to the .pde file

  private String outputPath = null;
  private File outputFolder = null;

  private boolean force = false; // replace that no good output folder
  private String device = runArg_DEVICE;
  private String target = targetArg_DEBUG;

  static public void main(String[] args) {
    // Do this early so that error messages go to the console
    Base.setCommandLine();
    // init the platform so that prefs and other native code is ready to go
    Base.initPlatform();
    // make sure a full JDK is installed
    Base.initRequirements();

    // launch command line handler
    Commander commander = new Commander(args);
    commander.execute();
  }

  public Commander(String[] args) {
    System.out.println("WOOOHOOOO ANDROOOOOOOOOOID CLI BUILD!");
    System.out.println(Arrays.toString(args));
    // Turns out the output goes as MacRoman or something else useless.
    // http://code.google.com/p/processing/issues/detail?id=1418
    try {
      systemOut = new PrintStream(System.out, true, "UTF-8");
      systemErr = new PrintStream(System.err, true, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      System.exit(1);
    }

    parseArgs(args);

    initValues();
  }

  private void parseArgs(String[] args) {
    for (String arg : args) {
      if (arg.length() == 0) {
        // ignore it, just the crappy shell script
      } else if (arg.equals(helpArg)) {
        // mode already set to HELP
      } else if (arg.startsWith(targetArg)) {
        target = extractValue(arg, targetArg, targetArg_DEBUG);
      } else if (arg.equals(buildArg)) {
        task = BUILD;
      } else if (arg.startsWith(runArg)) {
        task = RUN;
        device = extractValue(arg, runArg, runArg_DEVICE);
      } else if (arg.equals(exportApplicationArg)) {
        task = EXPORT;
      } else if (arg.startsWith(sketchArg)) {
        sketchPath = arg.substring(sketchArg.length());
        sketchFolder = new File(sketchPath);
        checkOrQuit(sketchFolder.exists(), sketchFolder + " does not exist.", false);

        File pdeFile = new File(sketchFolder, sketchFolder.getName() + ".pde");
        checkOrQuit(pdeFile.exists(), "Not a valid sketch folder. " + pdeFile + " does not exist.", true);

        pdePath = pdeFile.getAbsolutePath();
      } else if (arg.startsWith(outputArg)) {
        outputPath = arg.substring(outputArg.length());
      } else if (arg.equals(forceArg)) {
        force = true;
      } else {
        complainAndQuit("I don't know anything about " + arg + ".", true);
      }
    }
  }

  /**
   * <pre>
   * extractValue("--target=release", "--target", "debug") ==> "release"
   * extractValue("--target=",        "--target", "debug") ==> ""
   * extractValue("--target",         "--target", "debug") ==> "debug"
   * </pre>
   * 
   * @param arg
   * @param template
   * @param def
   * @return
   */
  private static String extractValue(String arg, String template, String def) {
    String result = def;
    String withEq = arg.substring(template.length());
    if (withEq.startsWith("=")) {
      result = withEq.substring(1);
    }
    return result;
  }

  private void initValues() {
    checkOrQuit(outputPath != null, "An output path must be specified.", true);
    outputFolder = new File(outputPath);
    if (outputFolder.exists()) {
      if (force) {
        Base.removeDir(outputFolder);
      } else {
        complainAndQuit("The output folder already exists. " + "Use --force to remove it.", false);
      }
    }

    Preferences.init();
    Base.locateSketchbookFolder();

    checkOrQuit(sketchPath != null, "No sketch path specified.", true);
    checkOrQuit(!outputPath.equals(sketchPath), "The sketch path and output path cannot be identical.", false);

    androidMode = (AndroidMode) ModeContribution.load(null, Base.getContentFile("modes/android"),
        "processing.mode.android.AndroidMode").getMode();
    androidMode.checkSDK(null);
  }

  private void execute() {
    if (processing.app.Base.DEBUG) {
      systemOut.println("Build status: ");
      systemOut.println("Sketch:   " + sketchPath);
      systemOut.println("Output:   " + outputPath);
      systemOut.println("Force:    " + force);
      systemOut.println("Target:   " + target);
      systemOut.println("==== Task ====");
      systemOut.println("--build:  " + (task == BUILD));
      systemOut.println("--run:    " + (task == RUN));
      systemOut.println("--export: " + (task == EXPORT));
      systemOut.println();
    }    
    if (task == HELP) {
      printCommandLine(systemOut);
      System.exit(0);
    }

    checkOrQuit(outputFolder.mkdirs(), "Could not create the output folder.", false);

    boolean success = false;

    try {
      sketch = new Sketch(pdePath, androidMode);
      if (task == BUILD || task == RUN) {
        AndroidBuild build = new AndroidBuild(sketch, androidMode);
        build.build(target);

        if (task == RUN) {
          AndroidRunner runner = new AndroidRunner(build, this);
          runner.launch(runArg_EMULATOR.equals(device) ? 
              Devices.getInstance().getEmulator() : 
              Devices.getInstance().getHardware());
        }

        success = true;
      
      } else if (task == EXPORT) {
        AndroidBuild build = new AndroidBuild(sketch, androidMode);
        build.exportProject();
        
        success = true;
      }
      
      if (!success) { // error already printed
        System.exit(1);
      }
      
      systemOut.println("Finished.");
      System.exit(0);
    } catch (SketchException re) {
      statusError(re);

    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  public void statusNotice(String message) {
    systemErr.println(message);
  }

  public void statusError(String message) {
    systemErr.println(message);
  }

  public void statusError(Exception exception) {
    if (exception instanceof SketchException) {
      SketchException re = (SketchException) exception;

      int codeIndex = re.getCodeIndex();
      if (codeIndex != -1) {
        // format the runner exception like emacs
        // blah.java:2:10:2:13: Syntax Error: This is a big error message
        String filename = sketch.getCode(codeIndex).getFileName();
        int line = re.getCodeLine() + 1;
        int column = re.getCodeColumn() + 1;
        // if (column == -1) column = 0;
        // TODO if column not specified, should just select the whole line.
        systemErr.println(filename + ":" + line + ":" + column + ":" + line + ":" + column + ":" + " "
            + re.getMessage());

      } else { // no line number, pass the trace along to the user
        exception.printStackTrace();
      }
    } else {
      exception.printStackTrace();
    }
  }

  private void checkOrQuit(boolean condition, String lastWords, boolean schoolEmFirst) {
    if (!condition) {
      complainAndQuit(lastWords, schoolEmFirst);
    }
  }

  void complainAndQuit(String lastWords, boolean schoolEmFirst) {
    if (schoolEmFirst) {
      printCommandLine(systemErr);
    }
    systemErr.println(lastWords);
    System.exit(1);
  }

  static void printCommandLine(PrintStream out) {
    out.println();
    out.println("Command line edition for Processing " + Base.VERSION_NAME + " (Android Mode)");
    out.println();
    out.println("--help               Show this help text. Congratulations.");
    out.println();
    out.println("--sketch=<name>      Specify the sketch folder (required)");
    out.println("--output=<name>      Specify the output folder (required and");
    out.println("                     cannot be the same as the sketch folder.)");
    out.println();
    out.println("--force              The sketch will not build if the output");
    out.println("                     folder already exists, because the contents");
    out.println("                     will be replaced. This option erases the");
    out.println("                     folder first. Use with extreme caution!");
    out.println();
    out.println("--target=<target>    \"debug\" or \"release\" target.");
    out.println("                     \"debug\" by default.");
    out.println("--build              Preprocess and compile a sketch into .apk file.");
    out.println("--run=<d|e>          Preprocess, compile, and run a sketch on device");
    out.println("                     or emulator. Device will be used by default.");
    out.println();
    out.println("--export             Export an application.");
    out.println();
  }

  @Override
  public void startIndeterminate() {
  }

  @Override
  public void stopIndeterminate() {
  }

  @Override
  public void statusHalt() {
  }

  @Override
  public boolean isHalted() {
    return false;
  }
}