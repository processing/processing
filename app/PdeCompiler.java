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
      "+E", // output errors in machine-parsable format
      "-d", buildPath, // output the classes in the buildPath
      buildPath + File.separator + className + ".java" // file to compile
    };

    int result;
    // XXXdmose try/catch should be separate
    try { 
      Process process = Runtime.getRuntime().exec(command);
      // XXXdmose race condition?
      new PdeMessageSiphon(process.getInputStream(),
                           process.getErrorStream(),
                           this);

      result = process.waitFor();
    } catch (Exception e) {
      result = -1;
    }
    //System.err.println("result = " + result);

    return result == 0 ? true : false;
  }

  // part of the PdeMessageConsumer interface
  //
  public void message(String s) {

    // as in: lib\build\Temporary_5476_6442.java:88: caution:Assignment of an expression to itself [KOPI]
    if (s.indexOf("caution") != -1) return;

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
        //err += "error:".length();
        String description = s1.substring(err + "Error:".length());
        description = description.trim();
        System.out.println("description = " + description);
        exception = new PdeException(description, lineNumber-1);
        editor.error(exception);

      } else {
        System.err.println("i suck: " + s);
      }

    } else {
      // this isn't the start of an error line, so don't attempt to parse
      // a line number out of it
    }
  }
}
