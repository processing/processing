/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeCompiler - default compiler class that connects to jikes
  Part of the Processing project - http://Proce55ing.net

  Copyright (c) 2001-03 
  Ben Fry, Massachusetts Institute of Technology and 
  Casey Reas, Interaction Design Institute Ivrea

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


public class PdeCompiler implements PdeMessageConsumer{
  static final String SUPER_BADNESS = 
    "Strange error while compiling, " + 
    "please send this code to processing@media.mit.edu";

  String buildPath;
  String className;
  PdeException exception;
  PdeEditor editor;

  public PdeCompiler(String buildPath, String className, PdeEditor editor) {
    this.buildPath = buildPath;
    this.className = className;
    this.editor = editor;
  }

  public boolean compileJava(PrintStream leechErr) {
    String command[] = new String[] { 
      "jikes",
      "-nowarn", // we're not currently interested in warnings
      "+E", // output errors in machine-parsable format
      "-d", buildPath, // output the classes in the buildPath
      buildPath + File.separator + className + ".java" // file to compile
    };

    firstErrorFound = false;  // haven't found any errors yet
    secondErrorFound = false;

    int result=0; // pre-initialized to quiet a bogus warning from jikes
    try { 

      // execute the compiler, and create threads to deal with the input
      // and error streams
      //
      Process process = Runtime.getRuntime().exec(command);
      new PdeMessageSiphon(process.getInputStream(), this);
      new PdeMessageSiphon(process.getErrorStream(), this);
      
      // wait for the process to finish.  if we get interrupted before waitFor
      // returns, continue waiting
      //
      boolean compiling = true;
      while (compiling) {
        try {
          result = process.waitFor();
          compiling = false;
        } catch (InterruptedException intExc) {
        }
      }

    } catch (Exception e) {
      result = -1;
    }
    //System.err.println("result = " + result);

    // if we hit either of these conditions, it means that something is 
    // fairly wrong, one possibility is that jikes has crashed.
    //
    if (result != 0 && result != 1 ) {
      exception = new PdeException(SUPER_BADNESS);
      editor.error(exception);
    }

    return result == 0 ? true : false;
  }

  boolean firstErrorFound;
  boolean secondErrorFound;

  // part of the PdeMessageConsumer interface
  //
  public void message(String s) {

    // ignore cautions
    if (s.indexOf("Caution") != -1) return;

    // jikes always uses a forward slash character as its separator, so 
    // we need to replace any platform-specific separator characters before
    // attemping to compare
    //
    String partialTempPath = buildPath.replace(File.separatorChar, '/') 
      + "/" + className + ".java";

    // if the partial temp path appears in the error message...
    //
    int partialStartIndex = s.indexOf(partialTempPath);
    if (partialStartIndex != -1) {

      // skip past the path and parse the int after the first colon
      //
      String s1 = s.substring(partialStartIndex + partialTempPath.length()
                              + 1);
      int colon = s1.indexOf(':');
      int lineNumber = Integer.parseInt(s1.substring(0, colon));
      //System.out.println("pde / line number: " + lineNumber);

      //String s2 = s1.substring(colon + 2);
      int err = s1.indexOf("Error:");
      if (err != -1) {

        // if the first error has already been found, then this must be
        // (at least) the second error found
        if (firstErrorFound) {
          secondErrorFound = true;
          return;
        }

        // if we're here at all, this is at least the first error
        firstErrorFound = true;

        //err += "error:".length();
        String description = s1.substring(err + "Error:".length());
        description = description.trim();
        //System.out.println("description = " + description);
        exception = new PdeException(description, lineNumber-1);
        editor.error(exception);

      } else {
        System.err.println("i suck: " + s);
      }

    } else {

      // this isn't the start of an error line, so don't attempt to parse
      // a line number out of it.

      // if we're not yet at the second error, these lines are probably 
      // associated with the first error message, which is already in the 
      // status bar, and are likely to be of interest to the user, so
      // spit them to the console.
      //
      if (!secondErrorFound) {
        System.err.println(s);
      }
    }
  }
}
