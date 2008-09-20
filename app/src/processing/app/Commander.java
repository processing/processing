/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2008 Ben Fry and Casey Reas

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

package processing.app;


import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import processing.core.PApplet;
import processing.core.PConstants;

import processing.app.debug.RunnerException;


public class Commander {
  static final String helpArg = "--help";
  static final String buildArg = "--build";
  static final String preprocArg = "--preprocess";
  static final String sketchArg = "--sketch=";
  static final String outputArg = "--output=";
  static final String exportAppletArg = "--export-applet";
  static final String exportApplicationArg = "--export-application";
  static final String platformArg = "--platform=";
  static final String preferencesArg = "--preferences=";

  static final int HELP = -1;
  static final int PREPROCESS = 0;
  static final int BUILD = 1;
  static final int EXPORT_APPLET = 2;
  static final int EXPORT_APPLICATION = 3;


  static public void main(String[] args) {
    // init the platform so that prefs and other native code is ready to go
    Base.initPlatform();
    // make sure a full JDK is installed
    Base.initRequirements();
    // run static initialization that grabs all the prefs
    //Preferences.init(null);
    // launch command line handler
    new Commander(args);
  }


  public Commander(String[] args) {
    String sketchPath = null;
    String outputPath = null;
    String preferencesPath = null;
    //int[] platforms = null;
    String[] platforms = null;
    int mode = HELP;

    for (String arg : args) {
      if (arg.equals(helpArg)) {
        // mode already set to HELP

      } else if (arg.equals(buildArg)) {
        mode = BUILD;

      } else if (arg.equals(preprocArg)) {
        mode = PREPROCESS;

      } else if (arg.equals(exportAppletArg)) {
        mode = EXPORT_APPLET;

      } else if (arg.equals(exportApplicationArg)) {
        mode = EXPORT_APPLICATION;
        
      } else if (arg.equals(platformArg)) {
        String platformStr = arg.substring(platformArg.length());
//        String[] pieces = PApplet.splitTokens(platformStr, ", ");
//        platforms = new int[pieces.length];
//        for (int i = 0; i < platforms.length; i++) {
//          if (pieces[i].equals("macosx")) {
//            platforms[i] = PConstants.MACOSX;
//          } else if (pieces[i].equals("windows")) {
//            platforms[i] = PConstants.WINDOWS;
//          } else if (pieces[i].equals("linux")) {
//            platforms[i] = PConstants.LINUX;
//          } else {
//            complainAndQuit("Platform \"" + pieces[i] + "\" does not exist.");
//          }
//        }
        platforms = PApplet.splitTokens(platformStr, ", ");
        
      } else if (arg.startsWith(sketchArg)) {
        sketchPath = arg.substring(sketchArg.length());

      } else if (arg.startsWith(outputArg)) {
        outputPath = arg.substring(outputArg.length());
      }
    }

    // run static initialization that grabs all the prefs
    // (also pass in a prefs path if that was specified)
    Preferences.init(preferencesPath);

    if (platforms == null) {
      platforms = new String[] { "macosx", "windows", "linux" };
    }

    if (mode == HELP) {
      printCommandLine(System.out);
      System.exit(0);

    } else if (sketchPath == null) {
      complainAndQuit("No sketch path specified.");
      
    } else if (outputPath == null) {
      complainAndQuit("No output path specified.");

    } else if (outputPath.equals(sketchPath)) {
      complainAndQuit("The sketch path and output path cannot be identical.");
      
    } else if (!sketchPath.toLowerCase().endsWith(".pde")) {
      complainAndQuit("Sketch path must point to the main .pde file.");
      
    } else {
      Sketch sketch = null; 
      boolean success = false;

      try {
        sketch = new Sketch(null, sketchPath);
        if (mode == PREPROCESS) {
          success = sketch.preprocess(outputPath) != null;

        } else if (mode == BUILD) {
          success = sketch.build(outputPath) != null;

        } else if (mode == EXPORT_APPLET) {
          success = sketch.exportApplet(outputPath);

        } else if (mode == EXPORT_APPLICATION) {
          if (platforms.length == 1) {
            if (platforms[0].equals("macosx")) {
              success = sketch.exportApplication(outputPath, PConstants.MACOSX);
            } else if (platforms[0].equals("windows")) {
              success = sketch.exportApplication(outputPath, PConstants.WINDOWS);
            } else if (platforms[0].equals("linux")) {
              success = sketch.exportApplication(outputPath, PConstants.LINUX);
            } else {
              complainAndQuit("Platform \"" + platforms[0] + "\" does not exist.");
            }
          } else {
            for (String platform : platforms) {
              File folder = new File(outputPath, "application." + platform);
              String path = folder.getAbsolutePath();
              if (platform.equals("macosx")) {
                success = sketch.exportApplication(path, PConstants.MACOSX);
              } else if (platform.equals("windows")) {
                success = sketch.exportApplication(path, PConstants.WINDOWS);
              } else if (platform.equals("linux")) {
                success = sketch.exportApplication(path, PConstants.LINUX);
              } else {
                complainAndQuit("Platform \"" + platform + "\" does not exist.");
              }
              if (!success) break;
            }
          }
        }
        System.exit(success ? 0 : 1);

      } catch (RunnerException re) {
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

      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
  }


  static void complainAndQuit(String lastWords) {
    printCommandLine(System.err);
    System.err.println(lastWords);
    System.exit(1);
  }


  static void printCommandLine(PrintStream out) {
    out.println("Processing rocks the console.");
    out.println();
//  out.println("./processing )
  }
}