/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Compiler - default compiler class that connects to jikes
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-08 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

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

package processing.app.debug;

import processing.app.Base;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.core.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;


public class Compiler {

  /**
   * Fire up 'ole javac based on <a href="http://java.sun.com/j2se/1.5.0/docs/tooldocs/solaris/javac.html#proginterface">this interface</a>.
   *
   * @param sketch Sketch object to be compiled.
   * @param buildPath Where the temporary files live and will be built from.
   * @return
   * @throws RunnerException Only if there's a problem. Only then.
   */
  public boolean compile(Sketch sketch,
                         String buildPath) throws RunnerException {
    // This will be filled in if anyone gets angry
    RunnerException exception = null;

    String baseCommand[] = new String[] {
      "-source", "1.5",
      "-target", "1.5",
      "-classpath", sketch.getClassPath(),
      "-nowarn", // we're not currently interested in warnings (ignored?)
      "-d", buildPath // output the classes in the buildPath
    };
    //PApplet.println(baseCommand);

    // make list of code files that need to be compiled
    // (some files are skipped if they contain no class)
    String[] preprocNames = new String[sketch.getCodeCount()];
    int preprocCount = 0;
    for (int i = 0; i < sketch.getCodeCount(); i++) {
      if (sketch.getCode(i).preprocName != null) {
        preprocNames[preprocCount++] = sketch.getCode(i).preprocName;
      }
    }
    String[] command = new String[baseCommand.length + preprocCount];
    System.arraycopy(baseCommand, 0, command, 0, baseCommand.length);
    // append each of the files to the command string
    for (int i = 0; i < preprocCount; i++) {
      command[baseCommand.length + i] =
        buildPath + File.separator + preprocNames[i];
    }
    //PApplet.println(command);

    int result = -1;  // needs to be set bad by default, in case hits IOE below

    try {
      // Load errors into a local StringBuffer
      final StringBuffer errorBuffer = new StringBuffer();

      // Create single method dummy writer class to slurp errors from javac
      Writer internalWriter = new Writer() {
          public void write(char[] buf, int off, int len) {
            errorBuffer.append(buf, off, len);
          }

          public void flush() { }

          public void close() { }
        };
      // Wrap as a PrintWriter since that's what compile() wants
      PrintWriter writer = new PrintWriter(internalWriter);

      result = com.sun.tools.javac.Main.compile(command, writer);

      // Close out the stream for good measure
      writer.flush();
      writer.close();

      BufferedReader reader =
        new BufferedReader(new StringReader(errorBuffer.toString()));
      System.out.println(errorBuffer.toString());

      String line = null;
      while ((line = reader.readLine()) != null) {
        //System.out.println("got line " + line);  // debug

        // Check to see if this is the last line.
        if ((PApplet.match(line, "\\d+ error[s]?") != null) ||
            (PApplet.match(line, "\\d+ warning[s]?") != null)) {
          break;
        }

        // Hide these because people are getting confused
        // http://dev.processing.org/bugs/show_bug.cgi?id=817
        // com/sun/tools/javac/resources/compiler.properties
        if (line.startsWith("Note: ")) {
          // if you mention serialVersionUID one more time, i'm kickin' you out
          if (line.indexOf("serialVersionUID") != -1) continue;
          // {0} uses unchecked or unsafe operations.
          // Some input files use unchecked or unsafe operations.
          if (line.indexOf("or unsafe operations") != -1) continue;
          // {0} uses or overrides a deprecated API.
          // Some input files use or override a deprecated API.
          if (line.indexOf("or override") != -1) continue;
          // Recompile with -Xlint:deprecation for details.
          // Recompile with -Xlint:unchecked for details.
          if (line.indexOf("Recompile with -Xlint:") != -1) continue;
          System.err.println(line);
          continue;
        }

        String errorFormat = "([\\w\\d_]+.java):(\\d+):\\s*(.*)\\s*";
        String[] pieces = PApplet.match(line, errorFormat);
        if (pieces == null) {
          exception = new RunnerException("Cannot parse error text: " + line);
          exception.hideStackTrace();
          // Send out the rest of the error message to the console.
          System.err.println(line);
          while ((line = reader.readLine()) != null) {
            System.err.println(line);
          }
          break;
        }
        String dotJavaFilename = pieces[0];
        // Line numbers are 1-indexed from javac
        int dotJavaLineIndex = PApplet.parseInt(pieces[1]) - 1;
        String errorMessage = pieces[2];

        int codeIndex = -1;
        int codeLine = -1;
        for (int i = 0; i < sketch.getCodeCount(); i++) {
          String name = sketch.getCode(i).preprocName;
          if ((name != null) && dotJavaFilename.equals(name)) {
            codeIndex = i;
          }
        }
        //System.out.println("code index/line are " + codeIndex + " " + codeLine);
        //System.out.println("java line number " + dotJavaLineIndex + " from " + dotJavaFilename);

        if (codeIndex == 0) {  // main class, figure out which tab
          for (int i = 1; i < sketch.getCodeCount(); i++) {
            SketchCode code = sketch.getCode(i);

            if (code.flavor == Sketch.PDE) {
              if (code.preprocOffset <= dotJavaLineIndex) {
                codeIndex = i;
                //System.out.println("i'm thinkin file " + i);
              }
            }
          }
        }
        //System.out.println("preproc offset is " + sketch.getCode(codeIndex).preprocOffset);
        codeLine = dotJavaLineIndex - sketch.getCode(codeIndex).preprocOffset;
        //System.out.println("code line now " + codeLine);
        exception = new RunnerException(errorMessage, codeIndex, codeLine, -1);
        exception.hideStackTrace();

        if (errorMessage.equals("cannot find symbol")) {
          handleCannotFindSymbol(reader, exception);
          
        } else if (errorMessage.indexOf("is already defined") != -1) {
          reader.readLine();  // repeats the line of code w/ error
          int codeColumn = caretColumn(reader.readLine());
          exception = new RunnerException(errorMessage, 
                                          codeIndex, codeLine, codeColumn);

        } else if (errorMessage.startsWith("package") &&
                   errorMessage.endsWith("does not exist")) {
          // Because imports are stripped out and re-added to the 0th line of
          // the preprocessed code, codeLine will always be wrong for imports.
          exception = new RunnerException("P" + errorMessage.substring(1) +
                                          ". You might be missing a library.");
        } else {
          exception = new RunnerException(errorMessage);
        }
        if (exception != null) {
          // The stack trace just shows that this happened inside the compiler,
          // which is a red herring. Don't ever show it for compiler stuff.
          exception.hideStackTrace();
          break;
        }
      }
    } catch (IOException e) {
      String bigSigh = "Error while compiling. (" + e.getMessage() + ")";
      exception = new RunnerException(bigSigh);
      e.printStackTrace();
      result = 1;
    }
    // In case there was something else.
    if (exception != null) throw exception;

    // Success means that 'result' is set to zero
    return (result == 0);
  }


  // Tell-tale signs of old code copied and pasted from the web.
  // Detect classes BFont, BGraphics, BImage; methods framerate, push;
  // and variables LINE_LOOP and LINE_STRIP.
  static HashMap crusties = new HashMap();
  static {
    crusties.put("BFont", new Object());
    crusties.put("BGraphics", new Object());
    crusties.put("BImage", new Object());
    crusties.put("framerate", new Object());
    crusties.put("push", new Object());
    crusties.put("LINE_LOOP", new Object());
    crusties.put("LINE_STRIP", new Object());
  }


  void handleCannotFindSymbol(BufferedReader reader,
                              RunnerException rex) throws IOException {
    String symbolLine = reader.readLine();
    /*String locationLine =*/ reader.readLine();
    /*String codeLine =*/ reader.readLine();
    String caretLine = reader.readLine();
    rex.setColumn(caretColumn(caretLine));

    String[] pieces =
      PApplet.match(symbolLine, "symbol\\s*:\\s*(\\w+)\\s+(.*)");
    if (pieces != null) {
      if (pieces[0].equals("class") ||
          pieces[0].equals("variable")) {
        rex.setMessage("Cannot find a " + pieces[0] + " " +
                       "named \u201C" + pieces[1] + "\u201D");
        if (crusties.get(pieces[1]) != null) {
          handleCrustyCode(rex);
        }

      } else if (pieces[0].equals("method")) {
        int leftParen = pieces[1].indexOf("(");
        int rightParen = pieces[1].indexOf(")");

        String methodName = pieces[1].substring(0, leftParen);
        String methodParams = pieces[1].substring(leftParen + 1, rightParen);

        String message =
          "Cannot find a function named \u201C" + methodName + "\u201D";
        if (methodParams.length() > 0) {
          if (methodParams.indexOf(',') != -1) {
            message += " with parameters ";
          } else {
            message += " with parameter ";
          }
          message += methodParams;
        }
        message += ".";
        rex.setMessage(message);

        // On second thought, make sure this isn't just some alpha/beta code
        if (crusties.get(methodName) != null) {
          handleCrustyCode(rex);
        }

      } else {
        System.out.println(symbolLine);
      }
    }
  }


  void handleCrustyCode(RunnerException rex) {
    rex.setMessage("This code needs to be updated, " +
                   "please read the \u201Cchanges\u201D reference.");
    Base.showReference("changes.html");
  }


  protected int caretColumn(String caretLine) {
    return caretLine.indexOf("^");
  }


  /////////////////////////////////////////////////////////////////////////////


  /**
   * Given a folder, return a list of absolute paths to all jar or zip files
   * inside that folder, separated by pathSeparatorChar.
   *
   * This will prepend a colon (or whatever the path separator is)
   * so that it can be directly appended to another path string.
   *
   * As of 0136, this will no longer add the root folder as well.
   *
   * This function doesn't bother checking to see if there are any .class
   * files in the folder or within a subfolder.
   */
  static public String contentsToClassPath(File folder) {
    if (folder == null) return "";

    StringBuffer abuffer = new StringBuffer();
    String sep = System.getProperty("path.separator");

    try {
      String path = folder.getCanonicalPath();

//    disabled as of 0136
      // add the folder itself in case any unzipped files
//      abuffer.append(sep);
//      abuffer.append(path);
//
      // When getting the name of this folder, make sure it has a slash
      // after it, so that the names of sub-items can be added.
      if (!path.endsWith(File.separator)) {
        path += File.separator;
      }

      String list[] = folder.list();
      for (int i = 0; i < list.length; i++) {
        // Skip . and ._ files. Prior to 0125p3, .jar files that had
        // OS X AppleDouble files associated would cause trouble.
        if (list[i].startsWith(".")) continue;

        if (list[i].toLowerCase().endsWith(".jar") ||
            list[i].toLowerCase().endsWith(".zip")) {
          abuffer.append(sep);
          abuffer.append(path);
          abuffer.append(list[i]);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();  // this would be odd
    }
    //System.out.println("included path is " + abuffer.toString());
    //packageListFromClassPath(abuffer.toString());  // WHY?
    return abuffer.toString();
  }


  /**
   * A classpath, separated by the path separator, will contain
   * a series of .jar/.zip files or directories containing .class
   * files, or containing subdirectories that have .class files.
   *
   * @param path the input classpath
   * @return array of possible package names
   */
  static public String[] packageListFromClassPath(String path) {
    Hashtable table = new Hashtable();
    String pieces[] =
      PApplet.split(path, File.pathSeparatorChar);

    for (int i = 0; i < pieces.length; i++) {
      //System.out.println("checking piece '" + pieces[i] + "'");
      if (pieces[i].length() == 0) continue;

      if (pieces[i].toLowerCase().endsWith(".jar") ||
          pieces[i].toLowerCase().endsWith(".zip")) {
        //System.out.println("checking " + pieces[i]);
        packageListFromZip(pieces[i], table);

      } else {  // it's another type of file or directory
        File dir = new File(pieces[i]);
        if (dir.exists() && dir.isDirectory()) {
          packageListFromFolder(dir, null, table);
          //importCount = magicImportsRecursive(dir, null,
          //                                  table);
                                              //imports, importCount);
        }
      }
    }
    int tableCount = table.size();
    String output[] = new String[tableCount];
    int index = 0;
    Enumeration e = table.keys();
    while (e.hasMoreElements()) {
      output[index++] = ((String) e.nextElement()).replace('/', '.');
    }
    //System.arraycopy(imports, 0, output, 0, importCount);
    //PApplet.printarr(output);
    return output;
  }


  static private void packageListFromZip(String filename, Hashtable table) {
    try {
      ZipFile file = new ZipFile(filename);
      Enumeration entries = file.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = (ZipEntry) entries.nextElement();

        if (!entry.isDirectory()) {
          String name = entry.getName();

          if (name.endsWith(".class")) {
            int slash = name.lastIndexOf('/');
            if (slash == -1) continue;

            String pname = name.substring(0, slash);
            if (table.get(pname) == null) {
              table.put(pname, new Object());
            }
          }
        }
      }
    } catch (IOException e) {
      System.err.println("Ignoring " + filename + " (" + e.getMessage() + ")");
      //e.printStackTrace();
    }
  }


  /**
   * Make list of package names by traversing a directory hierarchy.
   * Each time a class is found in a folder, add its containing set
   * of folders to the package list. If another folder is found,
   * walk down into that folder and continue.
   */
  static private void packageListFromFolder(File dir, String sofar,
                                            Hashtable table) {
                                          //String imports[],
                                          //int importCount) {
    //System.err.println("checking dir '" + dir + "'");
    boolean foundClass = false;
    String files[] = dir.list();

    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") || files[i].equals("..")) continue;

      File sub = new File(dir, files[i]);
      if (sub.isDirectory()) {
        String nowfar =
          (sofar == null) ? files[i] : (sofar + "." + files[i]);
        packageListFromFolder(sub, nowfar, table);
        //System.out.println(nowfar);
        //imports[importCount++] = nowfar;
        //importCount = magicImportsRecursive(sub, nowfar,
        //                                  imports, importCount);
      } else if (!foundClass) {  // if no classes found in this folder yet
        if (files[i].endsWith(".class")) {
          //System.out.println("unique class: " + files[i] + " for " + sofar);
          table.put(sofar, new Object());
          foundClass = true;
        }
      }
    }
  }
}
