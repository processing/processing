/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org

Copyright (c) 2004-10 Ben Fry and Casey Reas
Copyright (c) 2001-04 Massachusetts Institute of Technology

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

package processing.mode.java;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import processing.app.Base;
import processing.app.Preferences;
import processing.app.RunnerException;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.core.PApplet;
import processing.core.PConstants;


public class Build {
  Sketch sketch;
  

  public Build(Sketch sketch) {
    
  }
  
  
  /**
   * Cleanup temporary files used during a build/run.
   */
//  protected void cleanup() {
//    // if the java runtime is holding onto any files in the build dir, we
//    // won't be able to delete them, so we need to force a gc here
//    System.gc();
//
//    // note that we can't remove the builddir itself, otherwise
//    // the next time we start up, internal runs using Runner won't
//    // work because the build dir won't exist at startup, so the classloader
//    // will ignore the fact that that dir is in the CLASSPATH in run.sh
//    Base.removeDescendants(tempBuildFolder);
//  }


  /**
   * Preprocess, Compile, and Run the current code.
   * <P>
   * There are three main parts to this process:
   * <PRE>
   *   (0. if not java, then use another 'engine'.. i.e. python)
   *
   *    1. do the p5 language preprocessing
   *       this creates a working .java file in a specific location
   *       better yet, just takes a chunk of java code and returns a
   *       new/better string editor can take care of saving this to a
   *       file location
   *
   *    2. compile the code from that location
   *       catching errors along the way
   *       placing it in a ready classpath, or .. ?
   *
   *    3. run the code
   *       needs to communicate location for window
   *       and maybe setup presentation space as well
   *       run externally if a code folder exists,
   *       or if more than one file is in the project
   *
   *    X. afterwards, some of these steps need a cleanup function
   * </PRE>
   */
  //protected String compile() throws RunnerException {


  /**
   * When running from the editor, take care of preparations before running
   * the build, then start the build into a temporary folder.
   */
  public void prepareRun() throws RunnerException {
    // make sure the user didn't hide the sketch folder
    ensureExistence();

    // make sure any edits have been stored
    current.setProgram(editor.getText());

    // if an external editor is being used, need to grab the
    // latest version of the code from the file.
    if (Preferences.getBoolean("editor.external")) {
      // set current to null so that the tab gets updated
      // http://dev.processing.org/bugs/show_bug.cgi?id=515
      current = null;
      // nuke previous files and settings, just get things loaded
      load();
    }
  }
  
  /**
   * Grab any extra files, and get the temporary build folder ready.
   * The targetFolder will be deleted (and re-created), unless the user has 
   * altered that entry in the preferences.
   * @param targetFolder location to be cleaned so the build can commence.
   * @throws RunnerException
   */
  protected void prepareExport(File targetFolder) throws RunnerException {
    // make sure the user didn't hide the sketch folder
    ensureExistence();

    // fix for issue posted on the board. make sure that the code
    // is reloaded when exporting and an external editor is being used.
    if (Preferences.getBoolean("editor.external")) {
      // don't do from the command line
      if (editor != null) {
        // nuke previous files and settings
        load();
      }
    }

    // Nuke the old applet/application folder because it can cause trouble
    if (Preferences.getBoolean("export.delete_target_folder")) {
      Base.removeDir(targetFolder);
    }
    // Create a fresh output folder (needed before preproc is run next)
    targetFolder.mkdirs();

    // create a safe/random place to do the build
//    return new File(folder, "build" + ((int) (Math.random() * 1000)));
  }


  /**
   * Run the build inside the temporary build folder.
   * @return null if compilation failed, main class name if not
   * @throws RunnerException
   */
//  public String build() throws RunnerException {
//    try {
//      File folder = Base.createTempFolder(name, "classes"); 
//      return build(folder.getAbsolutePath());
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//  }


  /**
   * Preprocess and compile all the code for this sketch.
   *
   * In an advanced program, the returned class name could be different,
   * which is why the className is set based on the return value.
   * A compilation error will burp up a RunnerException.
   *
   * @return null if compilation failed, main class name if not
   */
  public String build(File srcFolder, File binFolder) throws RunnerException {
    // run the preprocessor
    String primaryClassName = preprocess(srcFolder);

    // compile the program. errors will happen as a RunnerException
    // that will bubble up to whomever called build().
    Compiler compiler = new Compiler();
    String bootClasses = System.getProperty("sun.boot.class.path");
    if (compiler.compile(this, srcFolder, binFolder, primaryClassName, getClassPath(), bootClasses)) {
      return primaryClassName;
    }
    return null;
  }


  /**
   * Build all the code for this sketch.
   *
   * In an advanced program, the returned class name could be different,
   * which is why the className is set based on the return value.
   * A compilation error will burp up a RunnerException.
   *
   * Setting purty to 'true' will cause exception line numbers to be incorrect.
   * Unless you know the code compiles, you should first run the preprocessor
   * with purty set to false to make sure there are no errors, then once
   * successful, re-export with purty set to true.
   *
   * @param buildPath Location to copy all the .java files
   * @return null if compilation failed, main class name if not
   */
  public String preprocess() throws RunnerException {
      return preprocess(makeTempBuildFolder(), null, new PdePreprocessor(name));
  }
  
  
  protected File makeTempBuildFolder() {
    try {
      File buildFolder = Base.createTempFolder(name, "src");
      if (buildFolder.mkdirs()) {
        return buildFolder;

      } else {
        Base.showWarning("Build folder bad", 
                         "Could not create a place to build the sketch.", null);
      }
    } catch (IOException e) {
      Base.showWarning("Build folder bad", 
                       "Could not find a place to build the sketch.", e);
    }
    return null;
  }


  public String preprocess(File srcFolder) throws RunnerException {
    return preprocess(srcFolder, null, new PdePreprocessor(name));
  }


  /**
   * @param srcFolder location where the .java source files will be placed
   * @param packageName null, or the package name that should be used as default
   * @param preprocessor the preprocessor object ready to do the work
   * @return main PApplet class name found during preprocess, or null if error 
   * @throws RunnerException
   */
  public String preprocess(File srcFolder, 
                           String packageName,
                           PdePreprocessor preprocessor) throws RunnerException {
    // make sure the user isn't playing "hide the sketch folder"
    ensureExistence();

    String[] codeFolderPackages = null;
    classPath = srcFolder.getAbsolutePath();

    // figure out the contents of the code folder to see if there
    // are files that need to be added to the imports
    if (codeFolder.exists()) {
      javaLibraryPath = codeFolder.getAbsolutePath();

      // get a list of .jar files in the "code" folder
      // (class files in subfolders should also be picked up)
      String codeFolderClassPath =
        Compiler.contentsToClassPath(codeFolder);
      // append the jar files in the code folder to the class path
      classPath += File.pathSeparator + codeFolderClassPath;
      // get list of packages found in those jars
      codeFolderPackages =
        Compiler.packageListFromClassPath(codeFolderClassPath);

    } else {
      javaLibraryPath = "";
    }

    // 1. concatenate all .pde files to the 'main' pde
    //    store line number for starting point of each code bit

    StringBuffer bigCode = new StringBuffer();
    int bigCount = 0;
    for (SketchCode sc : code) {
      if (sc.isExtension("pde")) {
        sc.setPreprocOffset(bigCount);
        bigCode.append(sc.getProgram());
        bigCode.append('\n');
        bigCount += sc.getLineCount();
      }
    }


    PreprocessResult result;
    try {
      File outputFolder = new File(srcFolder, packageName.replace('.', '/'));
      outputFolder.mkdirs();
      final File java = new File(outputFolder, name + ".java");
      final PrintWriter stream = new PrintWriter(new FileWriter(java));
      try {
        result = preprocessor.write(stream, bigCode.toString(), codeFolderPackages);
      } finally {
        stream.close();
      }
    } catch (FileNotFoundException fnfe) {
      fnfe.printStackTrace();
      String msg = "Build folder disappeared or could not be written";
      throw new RunnerException(msg);
    } catch (antlr.RecognitionException re) {
      // re also returns a column that we're not bothering with for now

      // first assume that it's the main file
      int errorFile = 0;
      int errorLine = re.getLine() - 1;

      // then search through for anyone else whose preprocName is null,
      // since they've also been combined into the main pde.
      for (int i = 1; i < codeCount; i++) {
        if (code[i].isExtension("pde") &&
            (code[i].getPreprocOffset() < errorLine)) {
          // keep looping until the errorLine is past the offset
          errorFile = i;
        }
      }
      errorLine -= code[errorFile].getPreprocOffset();

//      System.out.println("i found this guy snooping around..");
//      System.out.println("whatcha want me to do with 'im boss?");
//      System.out.println(errorLine + " " + errorFile + " " + code[errorFile].getPreprocOffset());

      String msg = re.getMessage();

      if (msg.equals("expecting RCURLY, found 'null'")) {
        // This can be a problem since the error is sometimes listed as a line
        // that's actually past the number of lines. For instance, it might
        // report "line 15" of a 14 line program. Added code to highlightLine()
        // inside Editor to deal with this situation (since that code is also
        // useful for other similar situations).
        throw new RunnerException("Found one too many { characters " +
                                  "without a } to match it.",
                                  errorFile, errorLine, re.getColumn());
      }

      if (msg.indexOf("expecting RBRACK") != -1) {
        System.err.println(msg);
        throw new RunnerException("Syntax error, " +
                                  "maybe a missing ] character?",
                                  errorFile, errorLine, re.getColumn());
      }

      if (msg.indexOf("expecting SEMI") != -1) {
        System.err.println(msg);
        throw new RunnerException("Syntax error, " +
                                  "maybe a missing semicolon?",
                                  errorFile, errorLine, re.getColumn());
      }

      if (msg.indexOf("expecting RPAREN") != -1) {
        System.err.println(msg);
        throw new RunnerException("Syntax error, " +
                                  "maybe a missing right parenthesis?",
                                  errorFile, errorLine, re.getColumn());
      }

      if (msg.indexOf("preproc.web_colors") != -1) {
        throw new RunnerException("A web color (such as #ffcc00) " +
                                  "must be six digits.",
                                  errorFile, errorLine, re.getColumn(), false);
      }

      //System.out.println("msg is " + msg);
      throw new RunnerException(msg, errorFile,
                                errorLine, re.getColumn());

    } catch (antlr.TokenStreamRecognitionException tsre) {
      // while this seems to store line and column internally,
      // there doesn't seem to be a method to grab it..
      // so instead it's done using a regexp

//      System.err.println("and then she tells me " + tsre.toString());
      // TODO not tested since removing ORO matcher.. ^ could be a problem
      String mess = "^line (\\d+):(\\d+):\\s";

      String[] matches = PApplet.match(tsre.toString(), mess);
      if (matches != null) {
        int errorLine = Integer.parseInt(matches[1]) - 1;
        int errorColumn = Integer.parseInt(matches[2]);

        int errorFile = 0;
        for (int i = 1; i < codeCount; i++) {
          if (code[i].isExtension("pde") &&
              (code[i].getPreprocOffset() < errorLine)) {
            errorFile = i;
          }
        }
        errorLine -= code[errorFile].getPreprocOffset();

        throw new RunnerException(tsre.getMessage(),
                                  errorFile, errorLine, errorColumn);

      } else {
        // this is bad, defaults to the main class.. hrm.
        String msg = tsre.toString();
        throw new RunnerException(msg, 0, -1, -1);
      }

    } catch (RunnerException pe) {
      // RunnerExceptions are caught here and re-thrown, so that they don't
      // get lost in the more general "Exception" handler below.
      throw pe;

    } catch (Exception ex) {
      // TODO better method for handling this?
      System.err.println("Uncaught exception type:" + ex.getClass());
      ex.printStackTrace();
      throw new RunnerException(ex.toString());
    }

    // grab the imports from the code just preproc'd

    importedLibraries = new ArrayList<LibraryFolder>();
    for (String item : result.extraImports) {
      // remove things up to the last dot
      int dot = item.lastIndexOf('.');
      // http://dev.processing.org/bugs/show_bug.cgi?id=1145
      String entry = (dot == -1) ? item : item.substring(0, dot);
      LibraryFolder library = Base.importToLibraryTable.get(entry);

      if (library != null) {
        if (!importedLibraries.contains(library)) {
          importedLibraries.add(library);
          classPath += library.getClassPath();
          javaLibraryPath += File.pathSeparator + library.getNativePath();
        }
      } else {
        // Don't bother complaining about java.* or javax.* because it's 
        // probably in boot.class.path. But we're not checking against that
        // path since it's enormous. Unfortunately we do still have to check
        // for libraries that begin with a prefix like javax, since that 
        // includes the OpenGL library. 
        if (!item.startsWith("java.") && !item.startsWith("javax.")) {
          System.err.println("No library found for " + entry);
        }
      }
    }
//    PApplet.println(PApplet.split(libraryPath, File.pathSeparatorChar));

    // Finally, add the regular Java CLASSPATH. This contains everything 
    // imported by the PDE itself (core.jar, pde.jar, quaqua.jar) which may
    // in fact be more of a problem.
    String javaClassPath = System.getProperty("java.class.path");
    // Remove quotes if any.. A messy (and frequent) Windows problem
    if (javaClassPath.startsWith("\"") && javaClassPath.endsWith("\"")) {
      javaClassPath = javaClassPath.substring(1, javaClassPath.length() - 1);
    }
    classPath += File.pathSeparator + javaClassPath;


    // 3. then loop over the code[] and save each .java file

    for (SketchCode sc : code) {
      if (sc.isExtension("java")) {
        // In most cases, no pre-processing services necessary for Java files.
        // Just write the the contents of 'program' to a .java file
        // into the build directory. However, if a default package is being 
        // used (as in Android), and no package is specified in the source, 
        // then we need to move this code to the same package as the sketch.
        // Otherwise, the class may not be found, or at a minimum, the default
        // access across the packages will mean that things behave incorrectly.
        // For instance, desktop code that uses a .java file with no packages,
        // will be fine with the default access, but since Android's PApplet
        // requires a package, code from that (default) package (such as the 
        // PApplet itself) won't have access to methods/variables from the 
        // package-less .java file (unless they're all marked public).
        String filename = sc.getFileName();
        try {
          String javaCode = sc.getProgram();
          String[] packageMatch = PApplet.match(javaCode, PACKAGE_REGEX);
          // if no package, and a default package is being used 
          // (i.e. on Android) we'll have to add one

          if (packageMatch == null && packageName == null) {
            sc.copyTo(new File(srcFolder, filename));

          } else {
            if (packageMatch == null) {
              // use the default package name, since mixing with package-less code will break
              packageMatch = new String[] { packageName };
              // add the package name to the source before writing it
              javaCode = "package " + packageName + ";" + javaCode;
            }
            File packageFolder = new File(srcFolder, packageMatch[0].replace('.', '/'));
            packageFolder.mkdirs();
            Base.saveFile(javaCode, new File(packageFolder, filename));
          }

        } catch (IOException e) {
          e.printStackTrace();
          String msg = "Problem moving " + filename + " to the build folder";
          throw new RunnerException(msg);
        }

      } else if (sc.isExtension("pde")) {
        // The compiler and runner will need this to have a proper offset
        sc.addPreprocOffset(result.headerOffset);
      }
    }
    foundMain = preprocessor.getFoundMain();
    return result.className;
  }


  public boolean getFoundMain() {
    return foundMain;
  }

  
  /**
   * Get the list of imported libraries. Used by external tools like Android mode.
   * @return list of library folders connected to this sketch.
   */
  public ArrayList<LibraryFolder> getImportedLibraries() {
    return importedLibraries;
  }


  /**
   * Map an error from a set of processed .java files back to its location
   * in the actual sketch.
   * @param message The error message.
   * @param filename The .java file where the exception was found.
   * @param line Line number of the .java file for the exception (1-indexed)
   * @return A RunnerException to be sent to the editor, or null if it wasn't
   *         possible to place the exception to the sketch code.
   */
//  public RunnerException placeExceptionAlt(String message,
//                                        String filename, int line) {
//    String appletJavaFile = appletClassName + ".java";
//    SketchCode errorCode = null;
//    if (filename.equals(appletJavaFile)) {
//      for (SketchCode code : getCode()) {
//        if (code.isExtension("pde")) {
//          if (line >= code.getPreprocOffset()) {
//            errorCode = code;
//          }
//        }
//      }
//    } else {
//      for (SketchCode code : getCode()) {
//        if (code.isExtension("java")) {
//          if (filename.equals(code.getFileName())) {
//            errorCode = code;
//          }
//        }
//      }
//    }
//    int codeIndex = getCodeIndex(errorCode);
//
//    if (codeIndex != -1) {
//      //System.out.println("got line num " + lineNumber);
//      // in case this was a tab that got embedded into the main .java
//      line -= getCode(codeIndex).getPreprocOffset();
//
//      // lineNumber is 1-indexed, but editor wants zero-indexed
//      line--;
//
//      // getMessage() will be what's shown in the editor
//      RunnerException exception =
//        new RunnerException(message, codeIndex, line, -1);
//      exception.hideStackTrace();
//      return exception;
//    }
//    return null;
//  }


  /**
   * Map an error from a set of processed .java files back to its location
   * in the actual sketch.
   * @param message The error message.
   * @param filename The .java file where the exception was found.
   * @param line Line number of the .java file for the exception (0-indexed!)
   * @return A RunnerException to be sent to the editor, or null if it wasn't
   *         possible to place the exception to the sketch code.
   */
  public RunnerException placeException(String message,
                                        String dotJavaFilename,
                                        int dotJavaLine) {
    int codeIndex = 0; //-1;
    int codeLine = -1;

//    System.out.println("placing " + dotJavaFilename + " " + dotJavaLine);
//    System.out.println("code count is " + getCodeCount());

    // first check to see if it's a .java file
    for (int i = 0; i < getCodeCount(); i++) {
      SketchCode code = getCode(i);
      if (code.isExtension("java")) {
        if (dotJavaFilename.equals(code.getFileName())) {
          codeIndex = i;
          codeLine = dotJavaLine;
          return new RunnerException(message, codeIndex, codeLine);
        }
      }
    }

    // If not the preprocessed file at this point, then need to get out
    if (!dotJavaFilename.equals(name + ".java")) {
      return null;
    }

    // if it's not a .java file, codeIndex will still be 0
    // this section searches through the list of .pde files
    codeIndex = 0;
    for (int i = 0; i < getCodeCount(); i++) {
      SketchCode code = getCode(i);

      if (code.isExtension("pde")) {
//        System.out.println("preproc offset is " + code.getPreprocOffset());
//        System.out.println("looking for line " + dotJavaLine);
        if (code.getPreprocOffset() <= dotJavaLine) {
          codeIndex = i;
//          System.out.println("i'm thinkin file " + i);
          codeLine = dotJavaLine - code.getPreprocOffset();
        }
      }
    }
    // could not find a proper line number, so deal with this differently.
    // but if it was in fact the .java file we're looking for, though,
    // send the error message through.
    // this is necessary because 'import' statements will be at a line
    // that has a lower number than the preproc offset, for instance.
//    if (codeLine == -1 && !dotJavaFilename.equals(name + ".java")) {
//      return null;
//    }
    return new RunnerException(message, codeIndex, codeLine);
  }


  protected boolean exportApplet() throws Exception {
    return exportApplet(new File(folder, "applet").getAbsolutePath());
  }


  /**
   * Handle export to applet.
   */
  public boolean exportApplet(String appletPath) throws RunnerException, IOException {
    File appletFolder = new File(appletPath);
    
    // build the sketch
//    String foundName = build(appletFolder.getPath());
    prepareExport(appletFolder);
    File srcFolder = makeTempBuildFolder();
    File binFolder = makeTempBuildFolder();
//    srcFolder.deleteOnExit();
//    binFolder.deleteOnExit();
    String foundName = build(srcFolder, binFolder);

    // (already reported) error during export, exit this function
    if (foundName == null) return false;
    
    // If name != exportSketchName, then that's weirdness
    // BUG unfortunately, that can also be a bug in the preproc :(
    if (!name.equals(foundName)) {
      Base.showWarning("Error during export",
                       "Sketch name is " + name + " but the sketch\n" +
                       "name in the code was " + foundName, null);
      return false;
    }

    int wide = PApplet.DEFAULT_WIDTH;
    int high = PApplet.DEFAULT_HEIGHT;
    String renderer = "";

    String scrubbed = scrubComments(code[0].getProgram());
    String[] matches = PApplet.match(scrubbed, SIZE_REGEX);

    if (matches != null) {
      try {
        wide = Integer.parseInt(matches[1]);
        high = Integer.parseInt(matches[2]);

        // Adding back the trim() for 0136 to handle Bug #769
        if (matches.length == 4) renderer = matches[3].trim();
        // Actually, matches.length should always be 4...

      } catch (NumberFormatException e) {
        // found a reference to size, but it didn't
        // seem to contain numbers
        final String message =
          "The size of this applet could not automatically be\n" +
          "determined from your code. You'll have to edit the\n" +
          "HTML file to set the size of the applet.\n" +
          "Use only numeric values (not variables) for the size()\n" +
          "command. See the size() reference for an explanation.";

        Base.showWarning("Could not find applet size", message, null);
      }
    }  // else no size() command found

    // Grab the Javadoc-style description from the main code.
    String description = "";
    String[] javadoc = PApplet.match(code[0].getProgram(), "/\\*{2,}(.*)\\*+/");
    if (javadoc != null) {
      StringBuffer dbuffer = new StringBuffer();
      String[] pieces = PApplet.split(javadoc[1], '\n');
      for (String line : pieces) {
        // if this line starts with * characters, remove 'em
        String[] m = PApplet.match(line, "^\\s*\\*+(.*)");
        dbuffer.append(m != null ? m[1] : line);
        // insert the new line into the html to help w/ line breaks
        dbuffer.append('\n');
      }
      description = dbuffer.toString();
//      PApplet.println(description);
    }

    // Add links to all the code
    StringBuffer sources = new StringBuffer();
    for (int i = 0; i < codeCount; i++) {
      sources.append("<a href=\"" + code[i].getFileName() + "\">" +
                     code[i].getPrettyName() + "</a> ");
    }

    // Copy the source files to the target, since we like
    // to encourage people to share their code
    for (int i = 0; i < codeCount; i++) {
      try {
        File exportedSource = new File(appletFolder, code[i].getFileName());
        //Base.copyFile(code[i].getFile(), exportedSource);
        code[i].copyTo(exportedSource);

      } catch (IOException e) {
        e.printStackTrace();  // ho hum, just move on...
      }
    }

    // Use separate jarfiles whenever a library or code folder is in use.
    boolean separateJar =
      Preferences.getBoolean("export.applet.separate_jar_files") ||
      codeFolder.exists() ||
      javaLibraryPath.length() != 0;

    // Copy the loading gif to the applet
    String LOADING_IMAGE = "loading.gif";
    // Check if the user already has their own loader image
    File loadingImage = new File(folder, LOADING_IMAGE);
    if (!loadingImage.exists()) {
      File skeletonFolder = new File(Base.getContentFile("lib"), "export");
      loadingImage = new File(skeletonFolder, LOADING_IMAGE);
    }
    Base.copyFile(loadingImage, new File(appletFolder, LOADING_IMAGE));

    // Create new .jar file
    FileOutputStream zipOutputFile =
      new FileOutputStream(new File(appletFolder, name + ".jar"));
    ZipOutputStream zos = new ZipOutputStream(zipOutputFile);
//    ZipEntry entry;

    StringBuffer archives = new StringBuffer();
    archives.append(name + ".jar");

    // Add the manifest file
    addManifest(zos);

    if (codeFolder.exists()) {
      File[] codeJarFiles = codeFolder.listFiles(new FilenameFilter() {        
        public boolean accept(File dir, String name) {
          if (name.charAt(0) == '.') return false;
          if (name.toLowerCase().endsWith(".jar")) return true;
          if (name.toLowerCase().endsWith(".zip")) return true;
          return false;
        }
      });
      for (File exportFile : codeJarFiles) {
        Base.copyFile(exportFile, new File(appletFolder, exportFile.getName()));
      }
    }

    File openglLibraryFolder = 
      new File(Base.getLibrariesPath(), "opengl/library");
    String openglLibraryPath = openglLibraryFolder.getAbsolutePath();
    boolean openglApplet = false;

    HashMap<String,Object> zipFileContents = new HashMap<String,Object>();

    // add contents of 'library' folders
    for (LibraryFolder library : importedLibraries) {
      if (library.getPath().equals(openglLibraryPath)) {
        openglApplet = true;
      }
      for (File exportFile : library.getAppletExports()) {
        String exportName = exportFile.getName();
        if (!exportFile.exists()) {
          System.err.println("File " + exportFile.getAbsolutePath() + " does not exist");

        } else if (exportFile.isDirectory()) {
          System.err.println("Ignoring sub-folder \"" + exportFile.getAbsolutePath() + "\"");

        } else if (exportName.toLowerCase().endsWith(".zip") ||
                   exportName.toLowerCase().endsWith(".jar")) {
          if (separateJar) {
            Base.copyFile(exportFile, new File(appletFolder, exportName));
            archives.append("," + exportName);
          } else {
            String path = exportFile.getAbsolutePath();
            packClassPathIntoZipFile(path, zos, zipFileContents);
          }

        } else {  // just copy the file over.. prolly a .dll or something
          Base.copyFile(exportFile, new File(appletFolder, exportName));
        }
      }
    }

    // Copy core.jar, or add its contents to the output .jar file
    File bagelJar = Base.isMacOS() ?
      Base.getContentFile("core.jar") :
      Base.getContentFile("lib/core.jar");
    if (separateJar) {
      Base.copyFile(bagelJar, new File(appletFolder, "core.jar"));
      archives.append(",core.jar");
    } else {
      String bagelJarPath = bagelJar.getAbsolutePath();
      packClassPathIntoZipFile(bagelJarPath, zos, zipFileContents);
    }

//    if (dataFolder.exists()) {
//      String dataFiles[] = Base.listFiles(dataFolder, false);
//      int offset = folder.getAbsolutePath().length() + 1;
//      for (int i = 0; i < dataFiles.length; i++) {
//        if (Base.isWindows()) {
//          dataFiles[i] = dataFiles[i].replace('\\', '/');
//        }
//        File dataFile = new File(dataFiles[i]);
//        if (dataFile.isDirectory()) continue;
//
//        // don't export hidden files
//        // skipping dot prefix removes all: . .. .DS_Store
//        if (dataFile.getName().charAt(0) == '.') continue;
//
//        entry = new ZipEntry(dataFiles[i].substring(offset));
//        zos.putNextEntry(entry);
//        zos.write(Base.loadBytesRaw(dataFile));
//        zos.closeEntry();
//      }
//    }
    // Add the data folder to the output .jar file
    addDataFolder(zos);

    // add the project's .class files to the jar
    // just grabs everything from the build directory
    // since there may be some inner classes
    // (add any .class files from the applet dir, then delete them)
    // TODO this needs to be recursive (for packages)
//    String classfiles[] = appletFolder.list();
//    for (int i = 0; i < classfiles.length; i++) {
//      if (classfiles[i].endsWith(".class")) {
//        entry = new ZipEntry(classfiles[i]);
//        zos.putNextEntry(entry);
//        zos.write(Base.loadBytesRaw(new File(appletFolder, classfiles[i])));
//        zos.closeEntry();
//      }
//    }
    addClasses(zos, binFolder);

    // remove the .class files from the applet folder. if they're not
    // removed, the msjvm will complain about an illegal access error,
    // since the classes are outside the jar file.
//    for (int i = 0; i < classfiles.length; i++) {
//      if (classfiles[i].endsWith(".class")) {
//        File deadguy = new File(appletFolder, classfiles[i]);
//        if (!deadguy.delete()) {
//          Base.showWarning("Could not delete",
//                           classfiles[i] + " could not \n" +
//                           "be deleted from the applet folder.  \n" +
//                           "You'll need to remove it by hand.", null);
//        }
//      }
//    }

//    if (false) {
//    if (!srcFolder.delete() || !binFolder.delete()) {
//      Base.showWarning("Could not delete",
//                       buildFolder.getName() + " could not \n" +
//                       "be deleted from the applet folder.  \n" +
//                       "You'll need to remove it by hand.", null);
//    }
//    }

    // close up the jar file
    zos.flush();
    zos.close();

    //

    // convert the applet template
    // @@sketch@@, @@width@@, @@height@@, @@archive@@, @@source@@
    // and now @@description@@

    File htmlOutputFile = new File(appletFolder, "index.html");
    // UTF-8 fixes http://dev.processing.org/bugs/show_bug.cgi?id=474
    PrintWriter htmlWriter = PApplet.createWriter(htmlOutputFile);

    InputStream is = null;
    // if there is an applet.html file in the sketch folder, use that
    File customHtml = new File(folder, "applet.html");
    if (customHtml.exists()) {
      is = new FileInputStream(customHtml);
    }
//    for (File libraryFolder : importedLibraries) {
//      System.out.println(libraryFolder + " " + libraryFolder.getAbsolutePath());
//    }
    // If the renderer is set to the built-in OpenGL library, 
    // then it's definitely an OpenGL applet.
    if (renderer.equals("OPENGL")) {
      openglApplet = true;
    }
    if (is == null) {
      if (openglApplet) {
        is = Base.getLibStream("export/applet-opengl.html");
      } else {
        is = Base.getLibStream("export/applet.html");
      }
    }
    BufferedReader reader = PApplet.createReader(is);

    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.indexOf("@@") != -1) {
        StringBuffer sb = new StringBuffer(line);
        int index = 0;
        while ((index = sb.indexOf("@@sketch@@")) != -1) {
          sb.replace(index, index + "@@sketch@@".length(),
                     name);
        }
        while ((index = sb.indexOf("@@source@@")) != -1) {
          sb.replace(index, index + "@@source@@".length(),
                     sources.toString());
        }
        while ((index = sb.indexOf("@@archive@@")) != -1) {
          sb.replace(index, index + "@@archive@@".length(),
                     archives.toString());
        }
        while ((index = sb.indexOf("@@width@@")) != -1) {
          sb.replace(index, index + "@@width@@".length(),
                     String.valueOf(wide));
        }
        while ((index = sb.indexOf("@@height@@")) != -1) {
          sb.replace(index, index + "@@height@@".length(),
                     String.valueOf(high));
        }
        while ((index = sb.indexOf("@@description@@")) != -1) {
          sb.replace(index, index + "@@description@@".length(),
                     description);
        }
        line = sb.toString();
      }
      htmlWriter.println(line);
    }

    reader.close();
    htmlWriter.flush();
    htmlWriter.close();

    return true;
  }


  /**
   * Replace all commented portions of a given String as spaces.
   * Utility function used here and in the preprocessor.
   */
  static public String scrubComments(String what) {
    char p[] = what.toCharArray();

    int index = 0;
    while (index < p.length) {
      // for any double slash comments, ignore until the end of the line
      if ((p[index] == '/') &&
          (index < p.length - 1) &&
          (p[index+1] == '/')) {
        p[index++] = ' ';
        p[index++] = ' ';
        while ((index < p.length) &&
               (p[index] != '\n')) {
          p[index++] = ' ';
        }

        // check to see if this is the start of a new multiline comment.
        // if it is, then make sure it's actually terminated somewhere.
      } else if ((p[index] == '/') &&
                 (index < p.length - 1) &&
                 (p[index+1] == '*')) {
        p[index++] = ' ';
        p[index++] = ' ';
        boolean endOfRainbow = false;
        while (index < p.length - 1) {
          if ((p[index] == '*') && (p[index+1] == '/')) {
            p[index++] = ' ';
            p[index++] = ' ';
            endOfRainbow = true;
            break;

          } else {
            // continue blanking this area
            p[index++] = ' ';
          }
        }
        if (!endOfRainbow) {
          throw new RuntimeException("Missing the */ from the end of a " +
                                     "/* comment */");
        }
      } else {  // any old character, move along
        index++;
      }
    }
    return new String(p);
  }


  public boolean exportApplicationPrompt() throws IOException, RunnerException {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(Box.createVerticalStrut(6));

    //Box panel = Box.createVerticalBox();

    //Box labelBox = Box.createHorizontalBox();
//    String msg = "<html>Click Export to Application to create a standalone, " +
//      "double-clickable application for the selected plaforms.";

//    String msg = "Export to Application creates a standalone, \n" +
//      "double-clickable application for the selected plaforms.";
    String line1 = "Export to Application creates double-clickable,";
    String line2 = "standalone applications for the selected plaforms.";
    JLabel label1 = new JLabel(line1, SwingConstants.CENTER);
    JLabel label2 = new JLabel(line2, SwingConstants.CENTER);
    label1.setAlignmentX(Component.LEFT_ALIGNMENT);
    label2.setAlignmentX(Component.LEFT_ALIGNMENT);
//    label1.setAlignmentX();
//    label2.setAlignmentX(0);
    panel.add(label1);
    panel.add(label2);
    int wide = label2.getPreferredSize().width;
    panel.add(Box.createVerticalStrut(12));

    final JCheckBox windowsButton = new JCheckBox("Windows");
    //windowsButton.setMnemonic(KeyEvent.VK_W);
    windowsButton.setSelected(Preferences.getBoolean("export.application.platform.windows"));
    windowsButton.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Preferences.setBoolean("export.application.platform.windows", windowsButton.isSelected());
      }
    });

    final JCheckBox macosxButton = new JCheckBox("Mac OS X");
    //macosxButton.setMnemonic(KeyEvent.VK_M);
    macosxButton.setSelected(Preferences.getBoolean("export.application.platform.macosx"));
    macosxButton.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Preferences.setBoolean("export.application.platform.macosx", macosxButton.isSelected());
      }
    });

    final JCheckBox linuxButton = new JCheckBox("Linux");
    //linuxButton.setMnemonic(KeyEvent.VK_L);
    linuxButton.setSelected(Preferences.getBoolean("export.application.platform.linux"));
    linuxButton.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Preferences.setBoolean("export.application.platform.linux", linuxButton.isSelected());
      }
    });

    JPanel platformPanel = new JPanel();
    //platformPanel.setLayout(new BoxLayout(platformPanel, BoxLayout.X_AXIS));
    platformPanel.add(windowsButton);
    platformPanel.add(Box.createHorizontalStrut(6));
    platformPanel.add(macosxButton);
    platformPanel.add(Box.createHorizontalStrut(6));
    platformPanel.add(linuxButton);
    platformPanel.setBorder(new TitledBorder("Platforms"));
    //Dimension goodIdea = new Dimension(wide, platformPanel.getPreferredSize().height);
    //platformPanel.setMaximumSize(goodIdea);
    wide = Math.max(wide, platformPanel.getPreferredSize().width);
    platformPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(platformPanel);

//  Box indentPanel = Box.createHorizontalBox();
//  indentPanel.add(Box.createHorizontalStrut(new JCheckBox().getPreferredSize().width));
    final JCheckBox showStopButton = new JCheckBox("Show a Stop button");
    //showStopButton.setMnemonic(KeyEvent.VK_S);
    showStopButton.setSelected(Preferences.getBoolean("export.application.stop"));
    showStopButton.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Preferences.setBoolean("export.application.stop", showStopButton.isSelected());
      }
    });
    showStopButton.setEnabled(Preferences.getBoolean("export.application.fullscreen"));
    showStopButton.setBorder(new EmptyBorder(3, 13, 6, 13));
//  indentPanel.add(showStopButton);
//  indentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    final JCheckBox fullScreenButton = new JCheckBox("Full Screen (Present mode)");
    //fullscreenButton.setMnemonic(KeyEvent.VK_F);
    fullScreenButton.setSelected(Preferences.getBoolean("export.application.fullscreen"));
    fullScreenButton.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        boolean sal = fullScreenButton.isSelected();
        Preferences.setBoolean("export.application.fullscreen", sal);
        showStopButton.setEnabled(sal);
      }
    });
    fullScreenButton.setBorder(new EmptyBorder(3, 13, 3, 13));

    JPanel optionPanel = new JPanel();
    optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.Y_AXIS));
    optionPanel.add(fullScreenButton);
    optionPanel.add(showStopButton);
//    optionPanel.add(indentPanel);
    optionPanel.setBorder(new TitledBorder("Options"));
    wide = Math.max(wide, platformPanel.getPreferredSize().width);
    //goodIdea = new Dimension(wide, optionPanel.getPreferredSize().height);
    optionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    //optionPanel.setMaximumSize(goodIdea);
    panel.add(optionPanel);

    Dimension good;
    //label1, label2, platformPanel, optionPanel
    good = new Dimension(wide, label1.getPreferredSize().height);
    label1.setMaximumSize(good);
    good = new Dimension(wide, label2.getPreferredSize().height);
    label2.setMaximumSize(good);
    good = new Dimension(wide, platformPanel.getPreferredSize().height);
    platformPanel.setMaximumSize(good);
    good = new Dimension(wide, optionPanel.getPreferredSize().height);
    optionPanel.setMaximumSize(good);

//    JPanel actionPanel = new JPanel();
//    optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.X_AXIS));
//    optionPanel.add(Box.createHorizontalGlue());

//    final JDialog frame = new JDialog(editor, "Export to Application");

//    JButton cancelButton = new JButton("Cancel");
//    cancelButton.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        frame.dispose();
//        return false;
//      }
//    });

    // Add the buttons in platform-specific order
//    if (PApplet.platform == PConstants.MACOSX) {
//      optionPanel.add(cancelButton);
//      optionPanel.add(exportButton);
//    } else {
//      optionPanel.add(exportButton);
//      optionPanel.add(cancelButton);
//    }
    String[] options = { "Export", "Cancel" };
    final JOptionPane optionPane = new JOptionPane(panel,
                                                   JOptionPane.PLAIN_MESSAGE,
                                                   //JOptionPane.QUESTION_MESSAGE,
                                                   JOptionPane.YES_NO_OPTION,
                                                   null,
                                                   options,
                                                   options[0]);

    final JDialog dialog = new JDialog(editor, "Export Options", true);
    dialog.setContentPane(optionPane);

    optionPane.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();

        if (dialog.isVisible() &&
            (e.getSource() == optionPane) &&
            (prop.equals(JOptionPane.VALUE_PROPERTY))) {
          //If you were going to check something
          //before closing the window, you'd do
          //it here.
          dialog.setVisible(false);
        }
      }
    });
    dialog.pack();
    dialog.setResizable(false);

    Rectangle bounds = editor.getBounds();
    dialog.setLocation(bounds.x + (bounds.width - dialog.getSize().width) / 2,
                       bounds.y + (bounds.height - dialog.getSize().height) / 2);
    dialog.setVisible(true);

    Object value = optionPane.getValue();
    if (value.equals(options[0])) {
      return exportApplication();
    } else if (value.equals(options[1]) || value.equals(new Integer(-1))) {
      // closed window by hitting Cancel or ESC
      editor.statusNotice("Export to Application canceled.");
    }
    return false;
  }


  /**
   * Export to application via GUI.
   */
  protected boolean exportApplication() throws IOException, RunnerException {
    String path = null;
    for (String platformName : PConstants.platformNames) {
      int platform = Base.getPlatformIndex(platformName);
      if (Preferences.getBoolean("export.application.platform." + platformName)) {
        if (LibraryFolder.hasMultipleArch(platform, importedLibraries)) {
          // export the 32-bit version
          path = new File(folder, "application." + platformName + "32").getAbsolutePath();
          if (!exportApplication(path, platform, 32)) {
            return false;
          }
          // export the 64-bit version
          path = new File(folder, "application." + platformName + "64").getAbsolutePath();
          if (!exportApplication(path, platform, 64)) {
            return false;
          }
        }
      }
    }
    return true;  // all good
  }


//  public boolean exportApplication(String destPath,
//                                   String platformName, 
//                                   int exportBits) throws IOException, RunnerException {
//    return exportApplication(destPath, Base.getPlatformIndex(platformName), exportBits);
//  }


  /**
   * Export to application without GUI.
   */
  public boolean exportApplication(String destPath,
                                   int exportPlatform, 
                                   int exportBits) throws IOException, RunnerException {
    File destFolder = new File(destPath);
    prepareExport(destFolder);

    // build the sketch
    File srcFolder = makeTempBuildFolder();
    File binFolder = makeTempBuildFolder();
    String foundName = build(srcFolder, binFolder);

    // (already reported) error during export, exit this function
    if (foundName == null) return false;

    // if name != exportSketchName, then that's weirdness
    // BUG unfortunately, that can also be a bug in the preproc :(
    if (!name.equals(foundName)) {
      Base.showWarning("Error during export",
                       "Sketch name is " + name + " but the sketch\n" +
                       "name in the code was " + foundName, null);
      return false;
    }


    /// figure out where the jar files will be placed

    File jarFolder = new File(destFolder, "lib");


    /// where all the skeleton info lives

    File skeletonFolder = new File(Base.getContentFile("lib"), "export");

    /// on macosx, need to copy .app skeleton since that's
    /// also where the jar files will be placed
    File dotAppFolder = null;
    if (exportPlatform == PConstants.MACOSX) {
      dotAppFolder = new File(destFolder, name + ".app");
      String APP_SKELETON = "skeleton.app";
      //File dotAppSkeleton = new File(folder, APP_SKELETON);
      File dotAppSkeleton = new File(skeletonFolder, APP_SKELETON);
      Base.copyDir(dotAppSkeleton, dotAppFolder);

      String stubName = "Contents/MacOS/JavaApplicationStub";
      // need to set the stub to executable
      // will work on osx or *nix, but just dies on windows, oh well..
      if (Base.isWindows()) {
        File warningFile = new File(destFolder, "readme.txt");
        PrintWriter pw = PApplet.createWriter(warningFile);
        pw.println("This application was created on Windows, which does not");
        pw.println("properly support setting files as \"executable\",");
        pw.println("a necessity for applications on Mac OS X.");
        pw.println();
        pw.println("To fix this, use the Terminal on Mac OS X, and from this");
        pw.println("directory, type the following:");
        pw.println();
        pw.println("chmod +x " + dotAppFolder.getName() + "/" + stubName);
        pw.flush();
        pw.close();

      } else {
        File stubFile = new File(dotAppFolder, stubName);
        String stubPath = stubFile.getAbsolutePath();
        Runtime.getRuntime().exec(new String[] { "chmod", "+x", stubPath });
      }

      // set the jar folder to a different location than windows/linux
      jarFolder = new File(dotAppFolder, "Contents/Resources/Java");
    }


    /// make the jar folder (windows and linux)

    if (!jarFolder.exists()) jarFolder.mkdirs();


    /// on windows, copy the exe file

    if (exportPlatform == PConstants.WINDOWS) {
      Base.copyFile(new File(skeletonFolder, "application.exe"),
                    new File(destFolder, this.name + ".exe"));
    }


    /// start copying all jar files

    Vector<String> jarListVector = new Vector<String>();


    /// create the main .jar file

    HashMap<String,Object> zipFileContents = new HashMap<String,Object>();

    FileOutputStream zipOutputFile =
      new FileOutputStream(new File(jarFolder, name + ".jar"));
    ZipOutputStream zos = new ZipOutputStream(zipOutputFile);
//    ZipEntry entry;

    // add the manifest file so that the .jar can be double clickable
    addManifest(zos);

    // add the project's .class files to the jar
    // (just grabs everything from the build directory,
    // since there may be some inner classes)
    // TODO this needs to be recursive (for packages)
//    File classFiles[] = tempClassesFolder.listFiles(new FilenameFilter() {
//      public boolean accept(File dir, String name) {
//        return name.endsWith(".class");
//      }
//    });
//    for (File file : classFiles) {
//      entry = new ZipEntry(file.getName());
//      zos.putNextEntry(entry);
//      zos.write(Base.loadBytesRaw(file));
//      zos.closeEntry();
//    }
    addClasses(zos, binFolder);

    // add the data folder to the main jar file
    addDataFolder(zos);

    // add the contents of the code folder to the jar
    if (codeFolder.exists()) {
      String includes = Compiler.contentsToClassPath(codeFolder);
      // Use tokens to get rid of extra blanks, which causes huge exports
      String[] codeList = PApplet.splitTokens(includes, File.separator);
      String cp = "";
      for (int i = 0; i < codeList.length; i++) {
        if (codeList[i].toLowerCase().endsWith(".jar") ||
            codeList[i].toLowerCase().endsWith(".zip")) {
          File exportFile = new File(codeFolder, codeList[i]);
          String exportFilename = exportFile.getName();
          Base.copyFile(exportFile, new File(jarFolder, exportFilename));
          jarListVector.add(exportFilename);
        } else {
          cp += codeList[i] + File.separatorChar;
        }
      }
      packClassPathIntoZipFile(cp, zos, zipFileContents);
    }

    zos.flush();
    zos.close();

    jarListVector.add(name + ".jar");


    /// add core.jar to the jar destination folder

    File bagelJar = Base.isMacOS() ?
      Base.getContentFile("core.jar") :
      Base.getContentFile("lib/core.jar");
    Base.copyFile(bagelJar, new File(jarFolder, "core.jar"));
    jarListVector.add("core.jar");


    /// add contents of 'library' folders to the export

    for (LibraryFolder library : importedLibraries) {
      // add each item from the library folder / export list to the output
      for (File exportFile : library.getApplicationExports(exportPlatform, exportBits)) { 
        String exportName = exportFile.getName();
//      String[] exportList = library.getExports(exportPlatform, exportBits);
//      for (String item : exportList) {
//      for (int i = 0; i < exportList.length; i++) {
//        if (exportList[i].equals(".") ||
//            exportList[i].equals("..")) continue;

//        exportList[i] = PApplet.trim(exportList[i]);
//        if (exportList[i].equals("")) continue;

//        File exportFile = new File(libraryFolder, exportList[i]);
//        File exportFile = new File(library.getLibraryPath(), )
        if (!exportFile.exists()) {
          System.err.println("File " + exportFile.getName() + " does not exist");

        } else if (exportFile.isDirectory()) {
          //System.err.println("Ignoring sub-folder \"" + exportList[i] + "\"");
          if (exportPlatform == PConstants.MACOSX) {
            // For OS X, copy subfolders to Contents/Resources/Java
            Base.copyDir(exportFile, new File(jarFolder, exportName));
          } else {
            // For other platforms, just copy the folder to the same directory
            // as the application.
            Base.copyDir(exportFile, new File(destFolder, exportName));
          }

        } else if (exportFile.getName().toLowerCase().endsWith(".zip") ||
                   exportFile.getName().toLowerCase().endsWith(".jar")) {
          //packClassPathIntoZipFile(exportFile.getAbsolutePath(), zos);
//          Base.copyFile(exportFile, new File(jarFolder, exportList[i]));
//          jarListVector.add(exportList[i]);
          Base.copyFile(exportFile, new File(jarFolder, exportName));
          jarListVector.add(exportName);

        } else if ((exportPlatform == PConstants.MACOSX) &&
                   (exportFile.getName().toLowerCase().endsWith(".jnilib"))) {
          // jnilib files can be placed in Contents/Resources/Java
          Base.copyFile(exportFile, new File(jarFolder, exportName));

        } else {
          // copy the file to the main directory.. prolly a .dll or something
          Base.copyFile(exportFile, new File(destFolder, exportName));
        }
      }
    }


    /// create platform-specific CLASSPATH based on included jars

    String jarList[] = new String[jarListVector.size()];
    jarListVector.copyInto(jarList);
    StringBuffer exportClassPath = new StringBuffer();

    if (exportPlatform == PConstants.MACOSX) {
      for (int i = 0; i < jarList.length; i++) {
        if (i != 0) exportClassPath.append(":");
        exportClassPath.append("$JAVAROOT/" + jarList[i]);
      }
    } else if (exportPlatform == PConstants.WINDOWS) {
      for (int i = 0; i < jarList.length; i++) {
        if (i != 0) exportClassPath.append(",");
        exportClassPath.append(jarList[i]);
      }
    } else {
      for (int i = 0; i < jarList.length; i++) {
        if (i != 0) exportClassPath.append(":");
        exportClassPath.append("$APPDIR/lib/" + jarList[i]);
      }
    }


    /// figure out run options for the VM

    String runOptions = Preferences.get("run.options");
    if (Preferences.getBoolean("run.options.memory")) {
      runOptions += " -Xms" +
        Preferences.get("run.options.memory.initial") + "m";
      runOptions += " -Xmx" +
        Preferences.get("run.options.memory.maximum") + "m";
    }

    /// macosx: write out Info.plist (template for classpath, etc)

    if (exportPlatform == PConstants.MACOSX) {
      String PLIST_TEMPLATE = "template.plist";
      File plistTemplate = new File(folder, PLIST_TEMPLATE);
      if (!plistTemplate.exists()) {
        plistTemplate = new File(skeletonFolder, PLIST_TEMPLATE);
      }
      File plistFile = new File(dotAppFolder, "Contents/Info.plist");
      PrintWriter pw = PApplet.createWriter(plistFile);

      String lines[] = PApplet.loadStrings(plistTemplate);
      for (int i = 0; i < lines.length; i++) {
        if (lines[i].indexOf("@@") != -1) {
          StringBuffer sb = new StringBuffer(lines[i]);
          int index = 0;
          while ((index = sb.indexOf("@@vmoptions@@")) != -1) {
            sb.replace(index, index + "@@vmoptions@@".length(),
                       runOptions);
          }
          while ((index = sb.indexOf("@@sketch@@")) != -1) {
            sb.replace(index, index + "@@sketch@@".length(),
                       name);
          }
          while ((index = sb.indexOf("@@classpath@@")) != -1) {
            sb.replace(index, index + "@@classpath@@".length(),
                       exportClassPath.toString());
          }
          while ((index = sb.indexOf("@@lsuipresentationmode@@")) != -1) {
            sb.replace(index, index + "@@lsuipresentationmode@@".length(),
                       Preferences.getBoolean("export.application.fullscreen") ? "4" : "0");
          }
          lines[i] = sb.toString();
        }
        // explicit newlines to avoid Windows CRLF
        pw.print(lines[i] + "\n");
      }
      pw.flush();
      pw.close();

    } else if (exportPlatform == PConstants.WINDOWS) {
      File argsFile = new File(destFolder + "/lib/args.txt");
      PrintWriter pw = PApplet.createWriter(argsFile);

      pw.println(runOptions);

      pw.println(this.name);
      pw.println(exportClassPath);

      pw.flush();
      pw.close();

    } else {
      File shellScript = new File(destFolder, this.name);
      PrintWriter pw = PApplet.createWriter(shellScript);

      // do the newlines explicitly so that windows CRLF
      // isn't used when exporting for unix
      pw.print("#!/bin/sh\n\n");
      //ps.print("APPDIR=`dirname $0`\n");
      pw.print("APPDIR=$(dirname \"$0\")\n");  // more posix compliant
      // another fix for bug #234, LD_LIBRARY_PATH ignored on some platforms
      //ps.print("LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$APPDIR\n");
      pw.print("java " + Preferences.get("run.options") +
               " -Djava.library.path=\"$APPDIR\"" +
               " -cp \"" + exportClassPath + "\"" +
               " " + this.name + "\n");

      pw.flush();
      pw.close();

      String shellPath = shellScript.getAbsolutePath();
      // will work on osx or *nix, but just dies on windows, oh well..
      if (!Base.isWindows()) {
        Runtime.getRuntime().exec(new String[] { "chmod", "+x", shellPath });
      }
    }


    /// copy the source files to the target
    /// (we like to encourage people to share their code)

    File sourceFolder = new File(destFolder, "source");
    sourceFolder.mkdirs();

    for (int i = 0; i < codeCount; i++) {
      try {
//        Base.copyFile(code[i].getFile(),
//                      new File(sourceFolder, code[i].file.getFileName()));
        code[i].copyTo(new File(sourceFolder, code[i].getFileName()));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    // move the .java file from the preproc there too
    String preprocFilename = this.name + ".java";
    File preprocFile = new File(destFolder, preprocFilename);
    if (preprocFile.exists()) {
      preprocFile.renameTo(new File(sourceFolder, preprocFilename));
    }


    /// remove the .class files from the export folder.
//    for (File file : classFiles) {
//      if (!file.delete()) {
//        Base.showWarning("Could not delete",
//                         file.getName() + " could not \n" +
//                         "be deleted from the applet folder.  \n" +
//                         "You'll need to remove it by hand.", null);
//      }
//    }
    // these will now be removed automatically via the temp folder deleteOnExit() 


    /// goodbye
    return true;
  }


  protected void addManifest(ZipOutputStream zos) throws IOException {
    ZipEntry entry = new ZipEntry("META-INF/MANIFEST.MF");
    zos.putNextEntry(entry);

    String contents =
      "Manifest-Version: 1.0\n" +
      "Created-By: Processing " + Base.VERSION_NAME + "\n" +
      "Main-Class: " + name + "\n";  // TODO not package friendly
    zos.write(contents.getBytes());
    zos.closeEntry();
  }
  
  
  protected void addClasses(ZipOutputStream zos, File dir) throws IOException {
    addClasses(zos, dir, dir.getAbsolutePath());
  }


  protected void addClasses(ZipOutputStream zos, File dir, String rootPath) throws IOException {
    File files[] = dir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return (name.charAt(0) != '.');
      }
    });
    for (File sub : files) {
      String relativePath = sub.getAbsolutePath().substring(rootPath.length());
      System.out.println("relative path is " + relativePath);

      if (sub.isDirectory()) {
        addClasses(zos, sub, rootPath);

      } else if (sub.getName().endsWith(".class")) {
        System.out.println("  adding item " + relativePath);
        ZipEntry entry = new ZipEntry(relativePath);
        zos.putNextEntry(entry);
        zos.write(Base.loadBytesRaw(sub));
        zos.closeEntry();
      }
    }
  }


  protected void addDataFolder(ZipOutputStream zos) throws IOException {
    if (dataFolder.exists()) {
      String[] dataFiles = Base.listFiles(dataFolder, false);
      int offset = folder.getAbsolutePath().length() + 1;
      //for (int i = 0; i < dataFiles.length; i++) {
      for (String path : dataFiles) {
        if (Base.isWindows()) {
          //dataFiles[i] = dataFiles[i].replace('\\', '/');
          path = path.replace('\\', '/');
        }
        //File dataFile = new File(dataFiles[i]);
        File dataFile = new File(path);
        if (!dataFile.isDirectory()) {
          // don't export hidden files
          // skipping dot prefix removes all: . .. .DS_Store
          if (dataFile.getName().charAt(0) != '.') {
            //ZipEntry entry = new ZipEntry(dataFiles[i].substring(offset));
            ZipEntry entry = new ZipEntry(dataFile.getAbsolutePath().substring(offset));
            zos.putNextEntry(entry);
            zos.write(Base.loadBytesRaw(dataFile));
            zos.closeEntry();
          }
        }
      }
    }
  }


  /**
   * Slurps up .class files from a colon (or semicolon on windows)
   * separated list of paths and adds them to a ZipOutputStream.
   */
  protected void packClassPathIntoZipFile(String path,
                                          ZipOutputStream zos,
                                          HashMap<String,Object> zipFileContents)
    throws IOException {
    String[] pieces = PApplet.split(path, File.pathSeparatorChar);

    for (int i = 0; i < pieces.length; i++) {
      if (pieces[i].length() == 0) continue;

      // is it a jar file or directory?
      if (pieces[i].toLowerCase().endsWith(".jar") ||
          pieces[i].toLowerCase().endsWith(".zip")) {
        try {
          ZipFile file = new ZipFile(pieces[i]);
          Enumeration<?> entries = file.entries();
          while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.isDirectory()) {
              // actually 'continue's for all dir entries

            } else {
              String entryName = entry.getName();
              // ignore contents of the META-INF folders
              if (entryName.indexOf("META-INF") == 0) continue;

              // don't allow duplicate entries
              if (zipFileContents.get(entryName) != null) continue;
              zipFileContents.put(entryName, new Object());

              ZipEntry entree = new ZipEntry(entryName);

              zos.putNextEntry(entree);
              byte buffer[] = new byte[(int) entry.getSize()];
              InputStream is = file.getInputStream(entry);

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
          e.printStackTrace();
        }
      } else {  // not a .jar or .zip, prolly a directory
        File dir = new File(pieces[i]);
        // but must be a dir, since it's one of several paths
        // just need to check if it exists
        if (dir.exists()) {
          packClassPathIntoZipFileRecursive(dir, null, zos);
        }
      }
    }
  }


  /**
   * Continue the process of magical exporting. This function
   * can be called recursively to walk through folders looking
   * for more goodies that will be added to the ZipOutputStream.
   */
  static protected void packClassPathIntoZipFileRecursive(File dir,
                                                          String sofar,
                                                          ZipOutputStream zos)
    throws IOException {
    String files[] = dir.list();
    for (int i = 0; i < files.length; i++) {
      // ignore . .. and .DS_Store
      if (files[i].charAt(0) == '.') continue;

      File sub = new File(dir, files[i]);
      String nowfar = (sofar == null) ?
        files[i] : (sofar + "/" + files[i]);

      if (sub.isDirectory()) {
        packClassPathIntoZipFileRecursive(sub, nowfar, zos);

      } else {
        // don't add .jar and .zip files, since they only work
        // inside the root, and they're unpacked
        if (!files[i].toLowerCase().endsWith(".jar") &&
            !files[i].toLowerCase().endsWith(".zip") &&
            files[i].charAt(0) != '.') {
          ZipEntry entry = new ZipEntry(nowfar);
          zos.putNextEntry(entry);
          zos.write(Base.loadBytesRaw(sub));
          zos.closeEntry();
        }
      }
    }
  }
}