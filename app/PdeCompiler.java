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
import java.util.*;
import java.util.zip.*;
import javax.swing.*;

public class PdeCompiler implements PdeMessageConsumer {
  static final String BUGS_URL = 
    "http://Proce55ing.net/bugs/";
  static final String SUPER_BADNESS = 
    "Compiler error, please submit this code to " + BUGS_URL;

  String buildPath;
  String className;
  File includeFolder;
  PdeException exception;
  PdeEditor editor;


  public PdeCompiler(String buildPath, String className, 
                     File includeFolder, PdeEditor editor) {
    this.buildPath = buildPath;
    this.includeFolder = includeFolder;
    this.className = className;
    this.editor = editor;
  }


  public boolean compileJava(PrintStream leechErr) {
    String userdir = System.getProperty("user.dir") + File.separator;

    //System.out.println(userdir + "jikes");
    //System.out.println(System.getProperty("sun.boot.class.path"));

    String command[] = new String[] { 
#ifdef MACOS
      // linux doesn't seem to like this
      // though windows probably doesn't care
      userdir + "jikes",
#else 
      "jikes",
#endif

      // necessary to make output classes compatible with 1.1
      // i.e. so that exported applets can work with ms jvm on the web
      "-target",
      "1.1",

      // used when run without a vm ("expert" mode)
      "-bootclasspath",
      calcBootClassPath(),
      //System.getProperty("sun.boot.class.path") + additional,

      // needed for macosx so that the classpath is set properly
      // also for windows because qtjava will most likely be here
      // and for linux, it just doesn't hurt
      "-classpath",
      //System.getProperty("java.class.path"),
      calcClassPath(includeFolder),

      "-nowarn", // we're not currently interested in warnings
      "+E", // output errors in machine-parsable format
      "-d", buildPath, // output the classes in the buildPath
      buildPath + File.separator + className + ".java" // file to compile
    };

    //for (int i = 0; i < command.length; i++) {
    //System.out.println("C" + i + ": " + command[i]);
    //System.out.println();
    //}

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

      // wait for the process to finish.  if interrupted 
      // before waitFor returns, continue waiting
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
      String msg = e.getMessage();
      if ((msg != null) && (msg.indexOf("jikes: not found") != -1)) {
        //System.err.println("jikes is missing");
        JOptionPane.showMessageDialog(editor.base, 
                                      "Could not find the compiler.\n" +
                                      "jikes is missing from your PATH,\n" +
                                      "see readme.txt for help.",
                                      "Compiler error",
                                      JOptionPane.ERROR_MESSAGE);
        return false;
      }
      e.printStackTrace();
      result = -1;
    }

    // if the result isn't a known, expected value it means that something
    // is fairly wrong, one possibility is that jikes has crashed.
    //
    if (result != 0 && result != 1 ) {
      exception = new PdeException(SUPER_BADNESS);
      editor.error(exception);
      PdeBase.openURL(BUGS_URL); 
    }

    return (result == 0) ? true : false;
  }


  boolean firstErrorFound;
  boolean secondErrorFound;

  // part of the PdeMessageConsumer interface
  //
  public void message(String s) {
    //System.err.println("MSG: " + s);
    System.err.print(s);

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


  static String additional;

  static public String calcBootClassPath() {
    if (additional == null) {
#ifdef MACOS
      additional = 
        includeFolder(new File("/System/Library/Java/Extensions/"));
      /*
      // for macosx only, doesn't work on macos9
      StringBuffer abuffer = new StringBuffer();

      // add the build folder.. why isn't it already there?
      //abuffer.append(":" + userdir + "lib/build");

      String list[] = new File("/System/Library/Java/Extensions").list();
      for (int i = 0; i < list.length; i++) {
        if (list[i].endsWith(".class") || list[i].endsWith(".jar") ||
            list[i].endsWith(".zip")) {
          //abuffer.append(System.getProperty("path.separator"));
          abuffer.append(":/System/Library/Java/Extensions/" + list[i]);
        }
      }
      additional = abuffer.toString();
      */
#else
      additional = "";
#endif
    }
    return System.getProperty("sun.boot.class.path") + additional;
  }


  static public String calcClassPath(File include) {
    return System.getProperty("java.class.path") + includeFolder(include);
  }


  /**
   * Return the path for a folder, with appended paths to 
   * any .jar or .zip files inside that folder.
   */
  static public String includeFolder(File folder) {
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
    magicImports(abuffer.toString());
    return abuffer.toString();
  }


  static public String[] magicImports(String path) {
    String imports[] = new String[100];
    int importCount = 0;

    String pieces[] = 
      BApplet.splitStrings(path, File.pathSeparatorChar);

    for (int i = 0; i < pieces.length; i++) {
      //System.out.println("checking piece " + pieces[i]);
      if (pieces[i].length() == 0) continue;

      if (pieces[i].toLowerCase().endsWith(".jar") || 
          pieces[i].toLowerCase().endsWith(".zip")) {
        try {
          ZipFile file = new ZipFile(pieces[i]);
          Enumeration entries = file.entries();
          while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.isDirectory()) {
              String name = entry.getName();
              if (name.equals("META-INF/")) continue;
              name = name.substring(0, name.length() - 1);
              name = name.replace('/', '.');

              if (importCount == imports.length) {
                String temp[] = new String[importCount << 1];
                System.arraycopy(imports, 0, temp, 0, importCount);
                imports = temp;
              }
              imports[importCount++] = name;
              //System.out.println("import " + name + ".*;");
            }
            //System.out.print(entry.isDirectory() ? "D " : "c ");
            //System.out.println(entry.getName());
          }
        } catch (IOException e) {
          System.err.println("Error in file " + pieces[i]);
        }
      } else {
        File dir = new File(pieces[i]);
        if (dir.exists()) {
          importCount = magicImportsRecursive(dir, null,
                                              imports, importCount);
        }
      }
    }
    //return null;
    String output[] = new String[importCount];
    System.arraycopy(imports, 0, output, 0, importCount);
    return output;
  }


  static public int magicImportsRecursive(File dir, String sofar, 
                                          String imports[], 
                                          int importCount) {
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


  /// 


  // goes through a colon (or semicolon on windows) separated list
  // of all the paths inside the 'code' folder

  static public void magicExports(String path, ZipOutputStream zos) 
  throws IOException {
    String pieces[] = 
      BApplet.splitStrings(path, File.pathSeparatorChar);

    for (int i = 0; i < pieces.length; i++) {
      if (pieces[i].length() == 0) continue;
      //System.out.println("checking piece " + pieces[i]);

      // is it a jar file or directory?
      if (pieces[i].toLowerCase().endsWith(".jar") || 
          pieces[i].toLowerCase().endsWith(".zip")) {
        try {
          ZipFile file = new ZipFile(pieces[i]);
          Enumeration entries = file.entries();
          while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.isDirectory()) {
              String name = entry.getName();
              if (name.equals("META-INF/")) continue;
              // actually 'continue's for all dir entries

            } else {
              //zos.putNextEntry(entry);
              ZipEntry entree = new ZipEntry(entry.getName());
              zos.putNextEntry(entree);
              byte buffer[] = new byte[(int) entry.getSize()];
              InputStream is = file.getInputStream(entry);
              //DataInputStream is = 
              //new DataInputStream(file.getInputStream(entry));
              //is.readFully(buffer);
              //System.out.println(buffer.length);
              //System.out.println(count + " " + buffer.length);

              int offset = 0;
              int remaining = buffer.length; 
              while (remaining > 0) {
                int count = is.read(buffer, offset, remaining);
                offset += count;
                remaining -= count;
              }

              zos.write(buffer);
              zos.flush();
              zos.closeEntry();
            }
          }
        } catch (IOException e) {
          System.err.println("Error in file " + pieces[i]);
        }
      } else {  // not a .jar or .zip, prolly a directory
        File dir = new File(pieces[i]);
        // but must be a dir, since it's one of several paths
        // just need to check if it exists
        if (dir.exists()) {
          magicExportsRecursive(dir, null, zos);
        }
      }
    }
  }


  static public void magicExportsRecursive(File dir, String sofar, 
                                           ZipOutputStream zos) 
  throws IOException {

    String files[] = dir.list();
    for (int i = 0; i < files.length; i++) {
      //if (files[i].equals(".") || files[i].equals("..")) continue;
      // ignore . .. and .DS_Store
      if (files[i].charAt(0) == '.') continue;

      File sub = new File(dir, files[i]);
      String nowfar = (sofar == null) ? 
        files[i] : (sofar + "/" + files[i]);

      if (sub.isDirectory()) {
        magicExportsRecursive(sub, nowfar, zos);

      } else {
        // don't add .jar and .zip files, since they only work
        // inside the root, and they're unpacked
        if (!files[i].toLowerCase().endsWith(".jar") &&
            !files[i].toLowerCase().endsWith(".zip") &&
            files[i].charAt(0) != '.') {
          ZipEntry entry = new ZipEntry(nowfar);
          zos.putNextEntry(entry);
          zos.write(PdeEditor.grabFile(sub));
          zos.closeEntry();
        }
      }
    }
  }
}
