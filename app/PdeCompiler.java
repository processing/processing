/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeCompiler - default compiler class that connects to jikes
  Part of the Processing project - http://processing.org

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
import java.util.*;
import java.util.zip.*;
import javax.swing.*;

public class PdeCompiler implements PdeMessageConsumer {
  static final String BUGS_URL = 
    "http://processing.org/bugs/";
  static final String SUPER_BADNESS = 
    "Compiler error, please submit this code to " + BUGS_URL;

  PdeSketch sketch;
  String buildPath;

  //String buildPath;
  //String className;
  //File includeFolder;
  PdeException exception;
  //PdeEditor editor;

  /*
  public PdeCompiler(String buildPath, String className, 
                     File includeFolder, PdeEditor editor) {
    this.buildPath = buildPath;
    this.includeFolder = includeFolder;
    this.className = className;
    this.editor = editor;
  }


  public boolean compile(PrintStream leechErr) {
  */

  public PdeCompiler() { }  // consider this a warning, you werkin soon.


  public boolean compile(PdeSketch sketch, String buildPath) 
    throws PdeException {

    this.sketch = sketch;
    this.buildPath = buildPath;
    
    // the pms object isn't used for anything but storage
    PdeMessageStream pms = new PdeMessageStream(this);

    String baseCommand[] = new String[] {
      // user.dir is folder containing P5 (and therefore jikes)
      // macosx needs the extra path info. linux doesn't like it, though
      // windows doesn't seem to care. write once, headache anywhere.
      ((PdeBase.platform != PdeBase.MACOSX) ? "jikes" :
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
      PdePreferences.get("compiler.jdk_version"),  //"1.1",
      // let the incompatability headache begin

      // used when run without a vm ("expert" mode)
      "-bootclasspath",
      calcBootClassPath(),

      // needed for macosx so that the classpath is set properly
      // also for windows because qtjava will most likely be here
      // and for linux, it just doesn't hurt
      "-classpath",
      sketch.classPath, //calcClassPath(includeFolder),

      "-nowarn", // we're not currently interested in warnings
      "+E", // output errors in machine-parsable format
      "-d", buildPath // output the classes in the buildPath
      //buildPath + File.separator + className + ".java" // file to compile
    };

    // make list of code files that need to be compiled
    // (some files are skipped if they contain no class)
    String preprocNames[] = new String[sketch.codeCount];
    int preprocCount = 0;
    for (int i = 0; i < sketch.codeCount; i++) {
      if (sketch.code[i].preprocName != null) {
        preprocNames[preprocCount++] = sketch.code[i].preprocName;
      }
    }
    String command[] = new String[baseCommand.length + preprocCount];
    System.arraycopy(baseCommand, 0, command, 0, baseCommand.length);
    // append each of the files to the command string
    for (int i = 0; i < preprocCount; i++) {
      command[baseCommand.length + i] = 
        buildPath + File.separator + preprocNames[i];
    }

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
      new PdeMessageSiphon(process.getInputStream(), this);
      new PdeMessageSiphon(process.getErrorStream(), this);

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
        PdeBase.showWarning("Compiler error",
                            "Could not find the compiler.\n" +
                            "jikes is missing from your PATH,\n" +
                            "see readme.txt for help.", null);
        return false;

      } else {
        e.printStackTrace();
        result = -1;
      }
    }

    // an error was queued up by message(), barf this back to build()
    // which will barf it back to PdeEditor. if you're having trouble
    // discerning the imagery, consider how cows regurgitate their food
    // to digest it, and the fact that they have five stomaches.
    //
    if (exception != null) throw exception; 

    // if the result isn't a known, expected value it means that something
    // is fairly wrong, one possibility is that jikes has crashed.
    //
    if (result != 0 && result != 1 ) {
      //exception = new PdeException(SUPER_BADNESS);
      //editor.error(exception);  // this will instead be thrown
      PdeBase.openURL(BUGS_URL);
      throw new PdeException(SUPER_BADNESS);
    }

    // success would mean that 'result' is set to zero
    return (result == 0); // ? true : false;
  }


  boolean firstErrorFound;
  boolean secondErrorFound;

  /**
   * Part of the PdeMessageConsumer interface, this is called 
   * whenever a piece (usually a line) of error message is spewed 
   * out from the compiler. The errors are parsed for their contents
   * and line number, which is then reported back to PdeEditor.
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
    for (int i = 0; i < sketch.codeCount; i++) {
      partialTempPath = buildPathSubst + sketch.code[i].preprocName;
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
        //System.out.println("description = " + description);
        exception = new PdeException(description, fileIndex, lineNumber-1, -1);

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
      if (PdeBase.platform == PdeBase.MACOSX) {
        additional = 
          contentsToClassPath(new File("/System/Library/Java/Extensions/"));
      }
      bootClassPath =  System.getProperty("sun.boot.class.path") + additional;
    }
    return bootClassPath;
  }


  /// 


  /**
   * Return the path for a folder, with appended paths to 
   * any .jar or .zip files inside that folder.
   * This will prepend a colon so that it can be directly
   * appended to another path string.
   */
  //static public String includeFolder(File folder) {
  static public String contentsToClassPath(File folder) {
    if (folder == null) return "";

    StringBuffer abuffer = new StringBuffer();
    String sep = System.getProperty("path.separator");

    try {
      // add the folder itself in case any unzipped files
      String path = folder.getCanonicalPath();
      abuffer.append(sep);
      abuffer.append(path);

      if (!path.endsWith(File.separator)) {
        path += File.separator;
      }
      //System.out.println("path is " + path);

      String list[] = folder.list();
      for (int i = 0; i < list.length; i++) {
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
    makeImportsFromClassPath(abuffer.toString());
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
  static public String[] makeImportsFromClassPath(String path) {
    Hashtable table = new Hashtable();
    String pieces[] = 
      BApplet.split(path, File.pathSeparatorChar);

    for (int i = 0; i < pieces.length; i++) {
      //System.out.println("checking piece '" + pieces[i] + "'");
      if (pieces[i].length() == 0) continue;

      if (pieces[i].toLowerCase().endsWith(".jar") || 
          pieces[i].toLowerCase().endsWith(".zip")) {
        makeImportsFromZip(pieces[i], table);

      } else {  // it's another type of file or directory
        File dir = new File(pieces[i]);
        if (dir.exists() && dir.isDirectory()) {
          makeImportsFromFolder(dir, null, table);
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
    return output;
  }


  static public void makeImportsFromZip(String filename, Hashtable table) {
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
  static public void makeImportsFromFolder(File dir, String sofar, 
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
        makeImportsFromFolder(sub, nowfar, table);
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
