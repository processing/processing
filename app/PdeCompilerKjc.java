// -*- Mode: JDE; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 2 -*-

// The existing Kjc code lives here, and can someday go away.

import java.io.*;


public class PdeCompilerKjc extends PdeCompiler {

  public PdeCompilerKjc(String buildPath, String className, PdeEditor editor) {
    super(buildPath, className, editor);
  }

  public boolean compileJava(PrintStream leechErr) {

    System.setErr(leechErr); // redirect stderr to our leech filter

    String args[] = new String[2];
    args[0] = "-d" + buildPath;
    args[1] = buildPath + File.separator + className + ".java";
    //System.out.println("args = " + args[0] + " " + args[1]);

    boolean success = at.dms.kjc.Main.compile(args);

    System.setErr(PdeEditorConsole.consoleErr);

    //System.err.println("success = " + success);
    return success;
  }

  // part of the PdeMessageConsumer interface
  //
  public void message(String s) {
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
        //exception = new PdeException(description, lineNumber-2);
        exception = new PdeException(description, lineNumber-1);
        editor.error(exception);

      } else {
        System.err.println("i suck: " + s);
      }

    } else {
      //System.err.println("don't understand: " + s);
      exception = new PdeException("Error while compiling, " + 
                                   "please send code to bugs@proce55ing.net");
      editor.error(exception);
    }
  }
}
