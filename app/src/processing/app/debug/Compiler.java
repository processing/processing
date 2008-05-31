/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Compiler - default compiler class that connects to jikes
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-06 Ben Fry and Casey Reas
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
import processing.app.Preferences;
import processing.app.Sketch;
import processing.core.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;


public class Compiler implements MessageConsumer {
  static final String BUGS_URL =
    "http://processing.org/bugs/";
  static final String SUPER_BADNESS =
    "Compiler error, please submit this code to " + BUGS_URL;

  Sketch sketch;
  String buildPath;

  //String buildPath;
  //String className;
  //File includeFolder;
  RunnerException exception;
  //Editor editor;

  /*
  public Compiler(String buildPath, String className,
                     File includeFolder, Editor editor) {
    this.buildPath = buildPath;
    this.includeFolder = includeFolder;
    this.className = className;
    this.editor = editor;
  }


  public boolean compile(PrintStream leechErr) {
  */

  public Compiler() { }  // consider this a warning, you werkin soon.


  /**
   * Fire up 'ole javac based on 
   * <a href="http://java.sun.com/j2se/1.5.0/docs/tooldocs/solaris/javac.html#proginterface">this interface</a>.
   *
   * @param sketch
   * @param buildPath
   * @return
   * @throws RunnerException
   */
  public boolean compile(Sketch sketch, 
                         String buildPath) throws RunnerException {
//    com.sun.tools.javac.Main javac = new com.sun.tools.javac.Main();
    
    this.sketch = sketch;
    this.buildPath = buildPath;    
      
    String baseCommand[] = new String[] {
          // this doesn't help much.. also java 1.4 seems to not support
          // -source 1.1 for javac, and jikes seems to also have dropped it.
          // for versions of jikes that don't complain, "final int" inside
          // a function doesn't throw an error, so it could just be a
          // ms jvm error that this sort of thing doesn't work. blech.
          //"-source",
          //"1.1",

          // necessary to make output classes compatible with 1.1
          // i.e. so that exported applets can work with ms jvm on the web
//          "-target",
//          Preferences.get("preproc.jdk_version"),  //"1.1",
          // let the incompatibility headache begin
//        for javac, we'll shut these off, and hope for the best so that ppl
//        can use 1.5 code inside .java tabs

          // used when run without a vm ("expert" mode)
//          "-bootclasspath",
//          calcBootClassPath(),
          // with javac, let's try and trust the system to do the right thing

          "-classpath", sketch.getClassPath(),

          "-nowarn", // we're not currently interested in warnings
          
          //"+E", // output errors in machine-parsable format
//          @#$)(*@#$ why doesn't javac have this option?
          
          "-d", buildPath // output the classes in the buildPath
          //buildPath + File.separator + className + ".java" // file to compile
    };
//  PApplet.println(baseCommand);

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

    firstErrorFound = false;  // haven't found any errors yet
    secondErrorFound = false;

    PipedWriter pipedWriter = new PipedWriter();
    PrintWriter writer = new PrintWriter(pipedWriter);
    PipedReader pipedReader = new PipedReader();
    BufferedReader reader = new BufferedReader(pipedReader);
    int result = 0;
    
    try {
      pipedWriter.connect(pipedReader);    
      //result = javac.compile(command, writer);
      result = com.sun.tools.javac.Main.compile(command, writer);

      while (pipedReader.ready()) {
        System.out.println("got line " + reader.readLine());
      }
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    
      // an error was queued up by message(), barf this back to build()
    // which will barf it back to Editor. if you're having trouble
    // discerning the imagery, consider how cows regurgitate their food
    // to digest it, and the fact that they have five stomaches.
    //
    //System.out.println("throwing up " + exception);
    if (exception != null) throw exception;

    // if the result isn't a known, expected value it means that something
    // is fairly wrong, one possibility is that jikes has crashed.
    //
//    if (result != 0 && result != 1 ) {
//      Base.openURL(BUGS_URL);
//      throw new RunnerException(SUPER_BADNESS);
//    }

    // success would mean that 'result' is set to zero
    return (result == 0); // ? true : false;
  }


  public boolean compileJikes(Sketch sketch, String buildPath)
    throws RunnerException {

    this.sketch = sketch;
    this.buildPath = buildPath;

    // the pms object isn't used for anything but storage
    /*MessageStream pms =*/ //new MessageStream(this);

    String baseCommand[] = new String[] {
      // user.dir is folder containing P5 (and therefore jikes)
      // macosx needs the extra path info. linux doesn't like it, though
      // windows doesn't seem to care. write once, headache anywhere.
      ((!Base.isMacOS()) ? "jikes" :
       System.getProperty("user.dir") + File.separator + "jikes"),

      // this doesn't help much.. also java 1.4 seems to not support
      // -source 1.1 for javac, and jikes seems to also have dropped it.
      // for versions of jikes that don't complain, "final int" inside
      // a function doesn't throw an error, so it could just be a
      // ms jvm error that this sort of thing doesn't work. blech.
      //"-source",
      //"1.1",

      // necessary to make output classes compatible with 1.1
      // i.e. so that exported applets can work with ms jvm on the web
      "-target",
      Preferences.get("preproc.jdk_version"),  //"1.1",
      // let the incompatability headache begin

      // used when run without a vm ("expert" mode)
      "-bootclasspath",
      calcBootClassPath(),

      // needed for macosx so that the classpath is set properly
      // also for windows because qtjava will most likely be here
      // and for linux, it just doesn't hurt
      "-classpath",
    //calcClassPath(includeFolder),  // removed sometime after 135?
      sketch.getClassPath(),
      //sketch.getClassPath() + File.pathSeparator + sketch.getLibraryPath(),

      "-nowarn", // we're not currently interested in warnings
      "+E", // output errors in machine-parsable format
      "-d", buildPath // output the classes in the buildPath
      //buildPath + File.separator + className + ".java" // file to compile
    };
//    PApplet.println(baseCommand);

    // make list of code files that need to be compiled
    // (some files are skipped if they contain no class)
    String preprocNames[] = new String[sketch.getCodeCount()];
    int preprocCount = 0;
    for (int i = 0; i < sketch.getCodeCount(); i++) {
      if (sketch.getCode(i).preprocName != null) {
        preprocNames[preprocCount++] = sketch.getCode(i).preprocName;
      }
    }
    String command[] = new String[baseCommand.length + preprocCount];
    System.arraycopy(baseCommand, 0, command, 0, baseCommand.length);
    // append each of the files to the command string
    for (int i = 0; i < preprocCount; i++) {
      command[baseCommand.length + i] =
        buildPath + File.separator + preprocNames[i];
    }
    //PApplet.println(command);

    /*
    String command[] = new String[baseCommand.length + sketch.codeCount];
    System.arraycopy(baseCommand, 0, command, 0, baseCommand.length);
    // append each of the files to the command string
    for (int i = 0; i < sketch.codeCount; i++) {
      command[baseCommand.length + i] =
        buildPath + File.separator + sketch.code[i].preprocName;
    }
    */

    //for (int i = 0; i < command.length; i++) {
      //System.out.println("cmd " + i + "  " + command[i]);
    //}

    firstErrorFound = false;  // haven't found any errors yet
    secondErrorFound = false;

    int result = 0; // pre-initialized to quiet a bogus warning from jikes
    try {
      // execute the compiler, and create threads to deal
      // with the input and error streams
      //
      Process process = Runtime.getRuntime().exec(command);
      MessageSiphon msi = new MessageSiphon(process.getInputStream(), this);
      MessageSiphon mse = new MessageSiphon(process.getErrorStream(), this);
      msi.thread.start();  // this is getting reeeally ugly
      mse.thread.start();

      // wait for the process to finish.  if interrupted
      // before waitFor returns, continue waiting
      //
      boolean compiling = true;
      while (compiling) {
        try {
          result = process.waitFor();
          //System.out.println("result is " + result);
          compiling = false;
        } catch (InterruptedException ignored) { }
      }

    } catch (Exception e) {
      String msg = e.getMessage();
      if ((msg != null) && (msg.indexOf("jikes: not found") != -1)) {
        //System.err.println("jikes is missing");
        Base.showWarning("Compiler error",
                            "Could not find the compiler.\n" +
                            "jikes is missing from your PATH,\n" +
                            "see the troubleshooting page for help.", null);
        return false;

      } else {
        e.printStackTrace();
        result = -1;
      }
    }

    // an error was queued up by message(), barf this back to build()
    // which will barf it back to Editor. if you're having trouble
    // discerning the imagery, consider how cows regurgitate their food
    // to digest it, and the fact that they have five stomaches.
    //
    //System.out.println("throwing up " + exception);
    if (exception != null) throw exception;

    // if the result isn't a known, expected value it means that something
    // is fairly wrong, one possibility is that jikes has crashed.
    //
    if (result != 0 && result != 1 ) {
      //exception = new RunnerException(SUPER_BADNESS);
      //editor.error(exception);  // this will instead be thrown
      Base.openURL(BUGS_URL);
      throw new RunnerException(SUPER_BADNESS);
    }

    // success would mean that 'result' is set to zero
    return (result == 0); // ? true : false;
  }


  boolean firstErrorFound;
  boolean secondErrorFound;

  /**
   * Part of the MessageConsumer interface, this is called
   * whenever a piece (usually a line) of error message is spewed
   * out from the compiler. The errors are parsed for their contents
   * and line number, which is then reported back to Editor.
   */
  public void message(String s) {
    // This receives messages as full lines, so a newline needs
    // to be added as they're printed to the console.
    System.err.println(s);

    // ignore cautions
    if (s.indexOf("Caution") != -1) return;

    // jikes always uses a forward slash character as its separator,
    // so replace any platform-specific separator characters before
    // attemping to compare
    //
    String buildPathSubst = buildPath.replace(File.separatorChar, '/') + "/";

    String partialTempPath = null;
    int partialStartIndex = -1; //s.indexOf(partialTempPath);
    int fileIndex = -1;  // use this to build a better exception

    // iterate through the project files to see who's causing the trouble
    for (int i = 0; i < sketch.getCodeCount(); i++) {
      if (sketch.getCode(i).preprocName == null) continue;

      partialTempPath = buildPathSubst + sketch.getCode(i).preprocName;
      partialStartIndex = s.indexOf(partialTempPath);
      if (partialStartIndex != -1) {
        fileIndex = i;
        //System.out.println("fileIndex is " + fileIndex);
        break;
      }
    }
    //+ className + ".java";

    // if the partial temp path appears in the error message...
    //
    //int partialStartIndex = s.indexOf(partialTempPath);
    if (partialStartIndex != -1) {

      // skip past the path and parse the int after the first colon
      //
      String s1 = s.substring(partialStartIndex +
                              partialTempPath.length() + 1);
      int colon = s1.indexOf(':');
      int lineNumber = Integer.parseInt(s1.substring(0, colon));
      //System.out.println("pde / line number: " + lineNumber);

      if (fileIndex == 0) {  // main class, figure out which tab
        for (int i = 1; i < sketch.getCodeCount(); i++) {
          if (sketch.getCode(i).flavor == Sketch.PDE) {
            if (sketch.getCode(i).preprocOffset < lineNumber) {
              fileIndex = i;
              //System.out.println("i'm thinkin file " + i);
            }
          }
        }
        if (fileIndex != 0) {  // if found another culprit
          lineNumber -= sketch.getCode(fileIndex).preprocOffset;
          //System.out.println("i'm sayin line " + lineNumber);
        }
      }

      //String s2 = s1.substring(colon + 2);
      int err = s1.indexOf("Error:");
      if (err != -1) {
        // if the first error has already been found, then this must be
        // (at least) the second error found
        if (firstErrorFound) {
          secondErrorFound = true;
          return;
        }

        // if executing at this point, this is *at least* the first error
        firstErrorFound = true;

        //err += "error:".length();
        String description = s1.substring(err + "Error:".length());
        description = description.trim();

        /*
        String hasLoop = "The method \"void loop();\" with default access";
        if (description.indexOf(hasLoop) != -1) {
          description =
            "Rename loop() to draw() in Processing 0070 and higher";
        }
        */

        String[] oldCodeMessages = new String[] {
          "Type \"BFont\" was not found",
          "Type \"BGraphics\" was not found",
          "Type \"BImage\" was not found",
          "No method named \"framerate\"",
          "No method named \"push\"",
          "No accessible field named \"LINE_LOOP\"",
          "No accessible field named \"LINE_STRIP\""
        };

        for (int i = 0; i < oldCodeMessages.length; i++) {
          if (description.indexOf(oldCodeMessages[i]) != -1) {
            description = "This code needs to be updated, " +
              "please read the changes reference.";
            Base.showReference("changes.html");
            // only complain once, and break
            break;
          }
        }

        String constructorProblem =
          "No applicable overload was found for a constructor of type";
        if (description.indexOf(constructorProblem) != -1) {
          //"simong.particles.ParticleSystem". Perhaps you wanted the overloaded version "ParticleSystem();" instead?
          int nextSentence = description.indexOf("\".") + 3;
          description = description.substring(nextSentence);
        }

        String overloadProblem = "No applicable overload";
        if (description.indexOf(overloadProblem) != -1) {
          int nextSentence = description.indexOf("\".") + 3;
          description = description.substring(nextSentence);
        }

        // c:/fry/processing/build/windows/work/lib/build/Temporary_6858_2476.java:1:34:1:41: Semantic Error: You need to modify your classpath, sourcepath, bootclasspath, and/or extdirs setup. Package "poo/shoe" could not be found in:
        String classpathProblem = "You need to modify your classpath";
        if (description.indexOf(classpathProblem) != -1) {
          if (description.indexOf("quicktime/std") != -1) {
            // special case for the quicktime libraries
            description =
              "To run sketches that use the Processing video library, " +
              "you must first install QuickTime for Java.";

          } else {
            // modified for 0136, why was this different? jikes msgs changed?
            //int nextSentence = description.indexOf(". Package") + 2;
            int nextSentence = description.indexOf("could not find ") + 1;
            description = "C" +
              //description.substring(nextSentence, description.indexOf(':')) +
              description.substring(nextSentence, description.lastIndexOf('\"') + 1) +
              " in the code folder or in any libraries.";
          }
        }

        if ((description.indexOf("\";\" inserted " +
                                 "to complete BlockStatement") != -1) ||
            (description.indexOf("; expected instead of this token") != -1)) {
          System.err.println(description);
          description = "Compiler error, maybe a missing semicolon?";
        }

        //System.out.println("description = " + description);
        //System.out.println("creating exception " + exception);
        exception =
          new RunnerException(description, fileIndex, lineNumber-1, -1);

        // NOTE!! major change here, this exception will be queued
        // here to be thrown by the compile() function
        //editor.error(exception);

      } else {
        System.err.println("i suck: " + s);
      }

    } else {
      // this isn't the start of an error line, so don't attempt to parse
      // a line number out of it.

      // if the second error hasn't been discovered yet, these lines
      // are probably associated with the first error message,
      // which is already in the status bar, and are likely to be
      // of interest to the user, so spit them to the console.
      //
      if (!secondErrorFound) {
        System.err.println(s);
      }
    }
  }


  static String bootClassPath;

  static public String calcBootClassPath() {
    if (bootClassPath == null) {
      String additional = "";
      if (Base.isMacOS()) {
        additional =
          contentsToClassPath(new File("/System/Library/Java/Extensions/"));
      }
      bootClassPath =  System.getProperty("sun.boot.class.path") + additional;
    }
    return bootClassPath;
  }


  ///


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
    //return importCount;
  }

  /*
  static public int magicImportsRecursive(File dir, String sofar,
                                          Hashtable table) {
                                          //String imports[],
                                          //int importCount) {
    System.err.println("checking dir '" + dir + "'");
    String files[] = dir.list();
    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") || files[i].equals("..")) continue;

      File sub = new File(dir, files[i]);
      if (sub.isDirectory()) {
        String nowfar = (sofar == null) ?
          files[i] : (sofar + "." + files[i]);
        //System.out.println(nowfar);
        imports[importCount++] = nowfar;

        importCount = magicImportsRecursive(sub, nowfar,
                                            imports, importCount);
      }
    }
    return importCount;
  }
  */
}
