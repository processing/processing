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

import processing.app.Preferences;
import processing.app.debug.RunnerException;
import processing.app.preproc.PdePreprocessor;
import processing.app.preproc.PreprocessResult;
import processing.core.PApplet;
import antlr.RecognitionException;
import antlr.TokenStreamException;


public class Preprocessor extends PdePreprocessor {
  Build build;


  public Preprocessor(final String sketchName, final Build build) throws IOException {
    super(sketchName);
    this.build = build;
  }


  public PreprocessResult write(Writer out, String program, String codeFolderPackages[])
  throws RunnerException, RecognitionException, TokenStreamException {
    if (build.sizeStatement != null) {
      int start = program.indexOf(build.sizeStatement);
      program = program.substring(0, start) + 
      program.substring(start + build.sizeStatement.length());
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
    out.println("package " + build.getPackageName() + ";");
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
      if (build.sketchWidth != null) {
        out.println(indent + "public int sketchWidth() { return " + build.sketchWidth + "; }");
      }
      if (build.sketchHeight != null) {
        out.println(indent + "public int sketchHeight() { return " + build.sketchHeight + "; }");
      }
      if (build.sketchRenderer != null) {
        out.println(indent + "public String sketchRenderer() { return " + build.sketchRenderer + "; }");
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