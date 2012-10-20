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

package processing.mode.java;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import processing.app.Base;
import processing.app.Library;
import processing.app.Preferences;
import processing.app.SketchException;
import processing.app.RunnerListener;
import processing.app.Sketch;
import processing.core.PApplet;
import processing.mode.java.runner.*;


/**
 * Class to handle running Processing from the command line.
 *
 * @author fry
 */
public class Commander implements RunnerListener {
  static final String helpArg = "--help";
//  static final String preprocArg = "--preprocess";
  static final String buildArg = "--build";
  static final String runArg = "--run";
  static final String presentArg = "--present";
  static final String sketchArg = "--sketch=";
  static final String forceArg = "--force";
  static final String outputArg = "--output=";
  static final String exportApplicationArg = "--export";
  static final String platformArg = "--platform=";
  static final String bitsArg = "--bits=";
//  static final String preferencesArg = "--preferences=";

  static final int HELP = -1;
  static final int PREPROCESS = 0;
  static final int BUILD = 1;
  static final int RUN = 2;
  static final int PRESENT = 3;
//  static final int EXPORT_APPLET = 4;
  static final int EXPORT = 4;

  Sketch sketch;


  static public void main(String[] args) {
    /*
    if (args == null || args.length == 0) {
//      System.out.println(System.getProperty("user.dir"));
      args = new String[] {
//        "--export",
//        "--build",
        "--run",
//        "--present",
//        "--force",
//        "--platform=windows",
        "--platform=macosx",
        "--bits=64",
        "--sketch=/Users/fry/coconut/processing/java/examples/Basics/Lights/Directional",
        "--output=/Users/fry/Desktop/test-build"
      };
    }
    */

    // Do this early so that error messages go to the console
    Base.setCommandLine();
    // init the platform so that prefs and other native code is ready to go
    Base.initPlatform();
    // make sure a full JDK is installed
    Base.initRequirements();
    // launch command line handler
    new Commander(args);
  }


  public Commander(String[] args) {
    String sketchPath = null;
    File sketchFolder = null;
    String pdePath = null;  // path to the .pde file
    String outputPath = null;
    File outputFolder = null;
    boolean force = false;  // replace that no good output folder
//    String preferencesPath = null;
    int platform = PApplet.platform; // default to this platform
    int platformBits = 0;
    int task = HELP;

    for (String arg : args) {
      if (arg.length() == 0) {
        // ignore it, just the crappy shell script

      } else if (arg.equals(helpArg)) {
        // mode already set to HELP

//      } else if (arg.equals(preprocArg)) {
//        task = PREPROCESS;

      } else if (arg.equals(buildArg)) {
        task = BUILD;

      } else if (arg.equals(runArg)) {
        task = RUN;

      } else if (arg.equals(presentArg)) {
        task = PRESENT;

//      } else if (arg.equals(exportAppletArg)) {
//        task = EXPORT_APPLET;

      } else if (arg.equals(exportApplicationArg)) {
        task = EXPORT;

      } else if (arg.startsWith(platformArg)) {
        String platformStr = arg.substring(platformArg.length());
        platform = Base.getPlatformIndex(platformStr);
        if (platform == -1) {
          complainAndQuit(platformStr + " should instead be " +
                          "'windows', 'macosx', or 'linux'.", true);
        }

      } else if (arg.startsWith(bitsArg)) {
        String bitsStr = arg.substring(bitsArg.length());
        if (bitsStr.equals("32")) {
          platformBits = 32;
        } else if (bitsStr.equals("64")) {
          platformBits = 64;
        } else {
          complainAndQuit("Bits should be either 32 or 64, not " + bitsStr, true);
        }

      } else if (arg.startsWith(sketchArg)) {
        sketchPath = arg.substring(sketchArg.length());
        sketchFolder = new File(sketchPath);
        if (!sketchFolder.exists()) {
          complainAndQuit(sketchFolder + " does not exist.", false);
        }
        File pdeFile = new File(sketchFolder, sketchFolder.getName() + ".pde");
        if (!pdeFile.exists()) {
          complainAndQuit("Not a valid sketch folder. " + pdeFile + " does not exist.", true);
        }
        pdePath = pdeFile.getAbsolutePath();

//      } else if (arg.startsWith(preferencesArg)) {
//        preferencesPath = arg.substring(preferencesArg.length());

      } else if (arg.startsWith(outputArg)) {
        outputPath = arg.substring(outputArg.length());

      } else if (arg.equals(forceArg)) {
        force = true;

      } else {
        complainAndQuit("I don't know anything about " + arg + ".", true);
      }
    }

//    if ((outputPath == null) &&
//        (task == PREPROCESS || task == BUILD ||
//         task == RUN || task == PRESENT)) {
//      complainAndQuit("An output path must be specified when using " +
//                      preprocArg + ", " + buildArg + ", " +
//                      runArg + ", or " + presentArg + ".");
//    }
    if (task == HELP) {
      printCommandLine(System.out);
      System.exit(0);
    }

    if (outputPath == null) {
      complainAndQuit("An output path must be specified.", true);
    }

    outputFolder = new File(outputPath);
    if (outputFolder.exists()) {
      if (force) {
        Base.removeDir(outputFolder);
      } else {
        complainAndQuit("The output folder already exists. " +
        		            "Use --force to remove it.", false);
      }
    }

    if (!outputFolder.mkdirs()) {
      complainAndQuit("Could not create the output folder.", false);
    }

//    // run static initialization that grabs all the prefs
//    // (also pass in a prefs path if that was specified)
//    if (preferencesPath != null) {
//      Preferences.init(preferencesPath);
//    }

    Preferences.init(null);
    Base.locateSketchbookFolder();

    if (sketchPath == null) {
      complainAndQuit("No sketch path specified.", true);

    } else if (outputPath.equals(sketchPath)) {
      complainAndQuit("The sketch path and output path cannot be identical.", false);

//    } else if (!pdePath.toLowerCase().endsWith(".pde")) {
//      complainAndQuit("Sketch path must point to the main .pde file.", false);

    } else {
      boolean success = false;

      JavaMode javaMode =
        new JavaMode(null, Base.getContentFile("modes/java"));
      try {
        sketch = new Sketch(pdePath, javaMode);
        if (task == BUILD || task == RUN || task == PRESENT) {
          JavaBuild build = new JavaBuild(sketch);
          File srcFolder = new File(outputFolder, "source");
          String className = build.build(srcFolder, outputFolder, true);
//          String className = build.build(sketchFolder, outputFolder, true);
          if (className != null) {
            success = true;
            if (task == RUN || task == PRESENT) {
              Runner runner = new Runner(build, this);
              runner.launch(task == PRESENT);
            }
          } else {
            success = false;
          }

        } else if (task == EXPORT) {
          if (outputPath == null) {
            javaMode.handleExportApplication(sketch);
          } else {
            JavaBuild build = new JavaBuild(sketch);
            build.build(true);
            if (build != null) {
//              if (platformBits == 0) {
//                platformBits = Base.getNativeBits();
//              }
              if (platformBits == 0 &&
                Library.hasMultipleArch(platform, build.getImportedLibraries())) {
                complainAndQuit("This sketch can be exported for 32- or 64-bit, please specify one.", true);
              }
              success = build.exportApplication(outputFolder, platform, platformBits);
            }
          }
        }
        if (!success) {  // error already printed
          System.exit(1);
        }
        System.out.println("Finished.");
        System.exit(0);

      } catch (SketchException re) {
        statusError(re);

      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
  }


  public void statusNotice(String message) {
    System.err.println(message);
  }


  public void statusError(String message) {
    System.err.println(message);
  }


  public void statusError(Exception exception) {
    if (exception instanceof SketchException) {
      SketchException re = (SketchException) exception;

      // format the runner exception like emacs
      //blah.java:2:10:2:13: Syntax Error: This is a big error message
      String filename = sketch.getCode(re.getCodeIndex()).getFileName();
      int line = re.getCodeLine();
      int column = re.getCodeColumn();
      if (column == -1) column = 0;
      // TODO if column not specified, should just select the whole line.
      System.err.println(filename + ":" +
                         line + ":" + column + ":" +
                         line + ":" + column + ":" + " " + re.getMessage());
    } else {
      exception.printStackTrace();
    }
  }


  static void complainAndQuit(String lastWords, boolean schoolEmFirst) {
    if (schoolEmFirst) {
      printCommandLine(System.err);
    }
    System.err.println(lastWords);
    System.exit(1);
  }


  static void printCommandLine(PrintStream out) {
    out.println("Command line edition for Processing " + Base.VERSION_NAME + " (Java Mode");
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
    out.println("--build              Preprocess and compile a sketch into .class files.");
    out.println("--run                Preprocess, compile, and run a sketch.");
    out.println("--present            Preprocess, compile, and run a sketch full screen.");
    out.println();
    out.println("--export             Export an application.");
    out.println("--platform           Specify the platform (export to application only).");
    out.println("                     Should be one of 'windows', 'macosx', or 'linux'.");
    out.println("--bits               Must be specified if libraries are used that are");
    out.println("                     32- or 64-bit specific such as the OpenGL library.");
    out.println("                     Otherwise specify 0 or leave it out.");
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