/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeCompilerJavac - compiler interface to kjc.. someday this will go away
  Part of the Processing project - http://Proce55ing.net

  Except where noted, code is written by Ben Fry and
  Copyright (c) 2001-03 Massachusetts Institute of Technology

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

import java.io.*;


// this class is incomplete. it was added in an attempt to support
// mac os 9, but the javac for jdk 1.1 doesn't seem to like the 
// file format of the classes produced by jikes, which is revision
// 57 code instead of rev 55. 

// using jikes -target 1.1 doesn't seem to help. the next attempt 
// was to use javac from the 1.1 jdk, which i attempted to hack
// into the bagel build scripts, but javac actually crashed (not
// just and exception, but an application crash) whenever used
// (especially if called on long paths/from another directory).

// there are plenty of ways to address this, but for the time being,
// it's not worth the time so we'll just suspend macos9 support.

public class PdeCompilerJavac extends PdeCompiler {

  public PdeCompilerJavac(String buildPath, String className, 
                          File includeFolder, PdeEditor editor) {
    super(buildPath, className, includeFolder, editor);
  }

  public boolean compileJava(PrintStream leechErr) {

    // probably not needed with javac
    //System.setErr(leechErr); // redirect stderr to our leech filter

    int argc = 0;
    String args[] = new String[4];
    args[argc++] = "-d";
    args[argc++] = buildPath;
    args[argc++] = "-nowarn";
    args[argc++] = buildPath + File.separator + className + ".java";
    //System.out.println("args = " + args[0] + " " + args[1]);

#ifdef JAVAC
    System.out.println("compiling with javac");
    return new sun.tools.javac.Main(leechErr, "javac").compile(args);
#else
    System.out.println("compile fell thru");
    return false;
#endif

    // probably not needed with javac
    //System.setErr(PdeEditorConsole.consoleErr);

    //System.err.println("success = " + success);
    //return success;
  }

  // part of the PdeMessageConsumer interface
  //
  public void message(String s) {
    System.out.println("javac msg: " + s);

    // as in: lib\build\Temporary_5476_6442.java:88: caution:Assignment of an expression to itself [KOPI]
    if (s.indexOf("caution") != -1) return;

    //
    //System.out.println("leech2: " + new String(b, offset, length));
    //String s = new String(b, offset, length);
    //if (s.indexOf(tempFilename) == 0) {
    String fullTempFilename = buildPath + File.separator + className + ".java";
    if (s.indexOf(fullTempFilename) == 0) {
      String s1 = s.substring(fullTempFilename.length() + 1);
      int colon = s1.indexOf(':');
      int lineNumber = Integer.parseInt(s1.substring(0, colon));
      //System.out.println("pde / line number: " + lineNumber);

      //String s2 = s1.substring(colon + 2);
      int err = s1.indexOf("error:");
      if (err != -1) {
        //err += "error:".length();
        String description = s1.substring(err + "error:".length());
        description = description.trim();
        
        // as in: ...error:Constructor setup must be named Temporary_5362_2548 [JL1 8.6]
        if(description.indexOf("Constructor setup must be named") != -1) {
          description = "Missing function return type, or constructor does not match class name";
        }
        //exception = new PdeException(description, lineNumber-2);
        exception = new PdeException(description, lineNumber-1);
        editor.error(exception);

      } else {
        System.err.println("i suck: " + s);
      }

    } else {
      //System.err.println("don't understand: " + s);
      exception = new PdeException(SUPER_BADNESS);
      editor.error(exception);
    }
  }
}
