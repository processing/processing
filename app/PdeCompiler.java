// -*- Mode: JDE; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 2 -*-

import java.io.*;

// This class will be the new default of Jikes.
//
public class PdeCompiler implements PdeMessageConsumer{

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
      exception = new PdeException("Error while compiling, " + 
                                   "please send code to bugs@proce55ing.net");
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
    String fullTempFilename = buildPath.replace(File.separatorChar, '/') 
      + "/" + className + ".java";

    if (s.indexOf(fullTempFilename) == 0) {
      String s1 = s.substring(fullTempFilename.length() + 1);
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
