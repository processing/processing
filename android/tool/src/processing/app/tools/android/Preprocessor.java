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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

import processing.app.Base;
import processing.app.Preferences;
import processing.app.Sketch;
import processing.app.debug.RunnerException;
import processing.app.preproc.PdePreprocessor;
import processing.app.preproc.PreprocessResult;
import processing.core.PApplet;
import antlr.RecognitionException;
import antlr.TokenStreamException;


public class Preprocessor extends PdePreprocessor {
  Sketch sketch;
  String packageName;
  
  String sizeStatement;
  String sketchWidth; 
  String sketchHeight;
  String sketchRenderer;


  public Preprocessor(final Sketch sketch, 
                      final String packageName) throws IOException {
    super(sketch.getName());
    this.sketch = sketch;
    this.packageName = packageName;
  }

  
  // TODO this needs to be a generic function inside Sketch or elsewhere

  protected boolean parseSketchSize() {
    // This matches against any uses of the size() function, whether numbers
    // or variables or whatever. This way, no warning is shown if size() isn't
    // actually used in the applet, which is the case especially for anyone
    // who is cutting/pasting from the reference.

    String scrubbed = Sketch.scrubComments(sketch.getCode(0).getProgram());
    String[] matches = PApplet.match(scrubbed, Sketch.SIZE_REGEX);
//    PApplet.println("matches: " + Sketch.SIZE_REGEX);
//    PApplet.println(matches);

    if (matches != null) {
      boolean badSize = false;
      
      if (!matches[1].equals("screenWidth") &&
          !matches[1].equals("screenHeight") &&
          PApplet.parseInt(matches[1], -1) == -1) {
        badSize = true;
      }
      if (!matches[2].equals("screenWidth") &&
          !matches[2].equals("screenHeight") &&
          PApplet.parseInt(matches[2], -1) == -1) {
        badSize = true;
      }

      if (badSize) {
        // found a reference to size, but it didn't seem to contain numbers
        final String message = 
          "The size of this applet could not automatically be determined\n" +
          "from your code. Use only numeric values (not variables) for the\n" +
          "size() command. See the size() reference for more information.";
        Base.showWarning("Could not find sketch size", message, null);
        System.out.println("More about the size() command on Android can be");
        System.out.println("found here: http://wiki.processing.org/w/Android");
        return false;
      }

//      PApplet.println(matches);
      sizeStatement = matches[0];  // the full method to be removed from the source
      sketchWidth = matches[1];
      sketchHeight = matches[2];
      sketchRenderer = matches[3].trim();
      if (sketchRenderer.length() == 0) {
        sketchRenderer = null;
      }
    } else {
      sizeStatement = null;
      sketchWidth = null;
      sketchHeight = null;
      sketchRenderer = null;
    }
    return true;
  }


  public PreprocessResult write(Writer out, String program, String codeFolderPackages[])
  throws RunnerException, RecognitionException, TokenStreamException {
    if (sizeStatement != null) {
      int start = program.indexOf(sizeStatement);
      program = program.substring(0, start) + 
      program.substring(start + sizeStatement.length());
    }
    //      String[] found = PApplet.match(program, "import\\s+processing.opengl.*\\s*");
    //      if (found != null) {
    //      }
    program = program.replaceAll("import\\s+processing\\.opengl\\.\\S+;", "");
    //      PApplet.println(program);
    return super.write(out, program, codeFolderPackages);
  }


  @Override
  protected int writeImports(final PrintWriter out,
                             final List<String> programImports,
                             final List<String> codeFolderImports) {
    out.println("package " + packageName + ";");
    out.println();
    // add two lines for the package above
    return 2 + super.writeImports(out, programImports, codeFolderImports);
  }


  protected void writeFooter(PrintWriter out, String className) {
    if (mode == Mode.STATIC) {
      // close off draw() definition
      out.println("noLoop();");
      out.println(indent + "}");
    }

    if ((mode == Mode.STATIC) || (mode == Mode.ACTIVE)) {
      out.println();
      if (sketchWidth != null) {
        out.println(indent + "public int sketchWidth() { return " + sketchWidth + "; }");
      }
      if (sketchHeight != null) {
        out.println(indent + "public int sketchHeight() { return " + sketchHeight + "; }");
      }
      if (sketchRenderer != null) {
        out.println(indent + "public String sketchRenderer() { return " + sketchRenderer + "; }");
      }

      // close off the class definition
      out.println("}");
    }
  }


  @Override
  public String[] getCoreImports() {
    return new String[] { 
      "processing.core.*", 
      "processing.xml.*" 
    };
  }


  @Override
  public String[] getDefaultImports() {
    final String prefsLine = Preferences.get("android.preproc.imports.list");
    if (prefsLine != null) {
      return PApplet.splitTokens(prefsLine, ", ");
    }

    // In the future, this may include standard classes for phone or
    // accelerometer access within the Android APIs. This is currently living
    // in code rather than preferences.txt because Android mode needs to
    // maintain its independence from the rest of processing.app.
    final String[] androidImports = new String[] {
      "android.view.MotionEvent", "android.view.KeyEvent",
      "android.graphics.Bitmap", //"java.awt.Image",
      "java.io.*", // for BufferedReader, InputStream, etc
      //"java.net.*", "java.text.*", // leaving otu for now
      "java.util.*" // for ArrayList and friends
      //"java.util.zip.*", "java.util.regex.*" // not necessary w/ newer i/o
    };

    Preferences.set("android.preproc.imports.list", 
                    PApplet.join(androidImports, ","));

    return androidImports;
  }
}