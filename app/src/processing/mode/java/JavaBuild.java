/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org

Copyright (c) 2004-12 Ben Fry and Casey Reas
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

import java.io.*;
import java.util.*;
import java.util.zip.*;

import processing.app.*;
import processing.core.*;
import processing.mode.java.preproc.*;

// Would you believe there's a java.lang.Compiler class? I wouldn't.


public class JavaBuild {
  public static final String PACKAGE_REGEX =
    "(?:^|\\s|;)package\\s+(\\S+)\\;";

  protected Sketch sketch;
  protected Mode mode;

  // what happens in the build, stays in the build.
  // (which is to say that everything below this line, stays within this class)

  protected File srcFolder;
  protected File binFolder;
  private boolean foundMain = false;
  private String classPath;
  protected String sketchClassName;

  /**
   * This will include the code folder, any library folders, etc. that might
   * contain native libraries that need to be picked up with java.library.path.
   * This is *not* the "Processing" libraries path, this is the Java libraries
   * path, as in java.library.path=BlahBlah, which identifies search paths for
   * DLLs or JNILIBs. (It's Java's LD_LIBRARY_PATH, for you UNIX fans.)
   * This is set by the preprocessor as it figures out where everything is.
   */
  private String javaLibraryPath;

  /** List of library folders, as figured out during preprocessing. */
  private ArrayList<Library> importedLibraries;
  

  public JavaBuild(Sketch sketch) {
    this.sketch = sketch;
    this.mode = sketch.getMode();
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
   * Run the build inside a temporary build folder. Used for run/present.
   * @return null if compilation failed, main class name if not
   * @throws RunnerException
   */
  public String build(boolean sizeWarning) throws SketchException {
    return build(sketch.makeTempFolder(), sketch.makeTempFolder(), sizeWarning);
  }


  /**
   * Preprocess and compile all the code for this sketch.
   *
   * In an advanced program, the returned class name could be different,
   * which is why the className is set based on the return value.
   * A compilation error will burp up a RunnerException.
   *
   * @return null if compilation failed, main class name if not
   */
  public String build(File srcFolder, File binFolder, boolean sizeWarning) throws SketchException {
    this.srcFolder = srcFolder;
    this.binFolder = binFolder;

//    Base.openFolder(srcFolder);
//    Base.openFolder(binFolder);

    // run the preprocessor
    String classNameFound = preprocess(srcFolder, sizeWarning);

    // compile the program. errors will happen as a RunnerException
    // that will bubble up to whomever called build().
//    Compiler compiler = new Compiler(this);
//    String bootClasses = System.getProperty("sun.boot.class.path");
//    if (compiler.compile(this, srcFolder, binFolder, primaryClassName, getClassPath(), bootClasses)) {
    if (Compiler.compile(this)) {
      sketchClassName = classNameFound;
      return classNameFound;
    }
    return null;
  }


  public String getSketchClassName() {
    return sketchClassName;
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
//  public String preprocess() throws SketchException {
//    return preprocess(sketch.makeTempFolder());
//  }


  public String preprocess(File srcFolder, boolean sizeWarning) throws SketchException {
    return preprocess(srcFolder, null, new PdePreprocessor(sketch.getName()), sizeWarning);
  }


  /**
   * @param srcFolder location where the .java source files will be placed
   * @param packageName null, or the package name that should be used as default
   * @param preprocessor the preprocessor object ready to do the work
   * @return main PApplet class name found during preprocess, or null if error
   * @throws SketchException
   */
  public String preprocess(File srcFolder,
                           String packageName,
                           PdePreprocessor preprocessor, 
                           boolean sizeWarning) throws SketchException {
    // make sure the user isn't playing "hide the sketch folder"
    sketch.ensureExistence();

//    System.out.println("srcFolder is " + srcFolder);
    classPath = binFolder.getAbsolutePath();

    // figure out the contents of the code folder to see if there
    // are files that need to be added to the imports
    String[] codeFolderPackages = null;
    if (sketch.hasCodeFolder()) {
      File codeFolder = sketch.getCodeFolder();
      javaLibraryPath = codeFolder.getAbsolutePath();

      // get a list of .jar files in the "code" folder
      // (class files in subfolders should also be picked up)
      String codeFolderClassPath =
        Base.contentsToClassPath(codeFolder);
      // append the jar files in the code folder to the class path
      classPath += File.pathSeparator + codeFolderClassPath;
      // get list of packages found in those jars
      codeFolderPackages =
        Base.packageListFromClassPath(codeFolderClassPath);

    } else {
      javaLibraryPath = "";
    }

    // 1. concatenate all .pde files to the 'main' pde
    //    store line number for starting point of each code bit

    StringBuffer bigCode = new StringBuffer();
    int bigCount = 0;
    for (SketchCode sc : sketch.getCode()) {
      if (sc.isExtension("pde")) {
        sc.setPreprocOffset(bigCount);
        bigCode.append(sc.getProgram());
        bigCode.append('\n');
        bigCount += sc.getLineCount();
      }
    }

//    // initSketchSize() sets the internal sketchWidth/Height/Renderer vars
//    // in the preprocessor. Those are used in preproc.write() so that they
//    // can be turned into sketchXxxx() methods.
//    // This also returns the size info as an array so that we can figure out
//    // if this fella is OpenGL, and if so, to add the import. It's messy and
//    // gross and someday we'll just always include OpenGL.
//    String[] sizeInfo =
//      preprocessor.initSketchSize(sketch.getMainProgram(), sizeWarning);
//      //PdePreprocessor.parseSketchSize(sketch.getMainProgram(), false);
//    if (sizeInfo != null) {
//      String sketchRenderer = sizeInfo[3];
//      if (sketchRenderer != null) {
//        if (sketchRenderer.equals("P2D") ||
//            sketchRenderer.equals("P3D") ||
//            sketchRenderer.equals("OPENGL")) {
//          bigCode.insert(0, "import processing.opengl.*; ");
//        }
//      }
//    }

    PreprocessorResult result;
    try {
      File outputFolder = (packageName == null) ?
        srcFolder : new File(srcFolder, packageName.replace('.', '/'));
      outputFolder.mkdirs();
      final File java = new File(outputFolder, sketch.getName() + ".java");
      final PrintWriter stream = new PrintWriter(new FileWriter(java));
      try {
        result = preprocessor.write(stream, bigCode.toString(), codeFolderPackages);
      } finally {
        stream.close();
      }
    } catch (FileNotFoundException fnfe) {
      fnfe.printStackTrace();
      String msg = "Build folder disappeared or could not be written";
      throw new SketchException(msg);
    } catch (antlr.RecognitionException re) {
      // re also returns a column that we're not bothering with for now

      // first assume that it's the main file
//      int errorFile = 0;
      int errorLine = re.getLine() - 1;

      // then search through for anyone else whose preprocName is null,
      // since they've also been combined into the main pde.
      int errorFile = findErrorFile(errorLine);
      errorLine -= sketch.getCode(errorFile).getPreprocOffset();

//      System.out.println("i found this guy snooping around..");
//      System.out.println("whatcha want me to do with 'im boss?");
//      System.out.println(errorLine + " " + errorFile + " " + code[errorFile].getPreprocOffset());

      String msg = re.getMessage();

      //System.out.println(java.getAbsolutePath());
      System.out.println(bigCode);

      if (msg.contains("expecting RCURLY")) {
      //if (msg.equals("expecting RCURLY, found 'null'")) {
        // This can be a problem since the error is sometimes listed as a line
        // that's actually past the number of lines. For instance, it might
        // report "line 15" of a 14 line program. Added code to highlightLine()
        // inside Editor to deal with this situation (since that code is also
        // useful for other similar situations).
        throw new SketchException("Found one too many { characters " +
                                  "without a } to match it.",
                                  errorFile, errorLine, re.getColumn(), false);
      }

      if (msg.contains("expecting LCURLY")) {
        System.err.println(msg);
        String suffix = ".";
        String[] m = PApplet.match(msg, "found ('.*')");
        if (m != null) {
          suffix = ", not " + m[1] + ".";
        }
        throw new SketchException("Was expecting a { character" + suffix,
                                   errorFile, errorLine, re.getColumn(), false);
      }

      if (msg.indexOf("expecting RBRACK") != -1) {
        System.err.println(msg);
        throw new SketchException("Syntax error, " +
                                  "maybe a missing ] character?",
                                  errorFile, errorLine, re.getColumn(), false);
      }

      if (msg.indexOf("expecting SEMI") != -1) {
        System.err.println(msg);
        throw new SketchException("Syntax error, " +
                                  "maybe a missing semicolon?",
                                  errorFile, errorLine, re.getColumn(), false);
      }

      if (msg.indexOf("expecting RPAREN") != -1) {
        System.err.println(msg);
        throw new SketchException("Syntax error, " +
                                  "maybe a missing right parenthesis?",
                                  errorFile, errorLine, re.getColumn(), false);
      }

      if (msg.indexOf("preproc.web_colors") != -1) {
        throw new SketchException("A web color (such as #ffcc00) " +
                                  "must be six digits.",
                                  errorFile, errorLine, re.getColumn(), false);
      }

      //System.out.println("msg is " + msg);
      throw new SketchException(msg, errorFile,
                                errorLine, re.getColumn(), false);

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
        for (int i = 1; i < sketch.getCodeCount(); i++) {
          SketchCode sc = sketch.getCode(i);
          if (sc.isExtension("pde") &&
              (sc.getPreprocOffset() < errorLine)) {
            errorFile = i;
          }
        }
        errorLine -= sketch.getCode(errorFile).getPreprocOffset();

        throw new SketchException(tsre.getMessage(),
                                  errorFile, errorLine, errorColumn);

      } else {
        // this is bad, defaults to the main class.. hrm.
        String msg = tsre.toString();
        throw new SketchException(msg, 0, -1, -1);
      }

    } catch (SketchException pe) {
      // RunnerExceptions are caught here and re-thrown, so that they don't
      // get lost in the more general "Exception" handler below.
      throw pe;

    } catch (Exception ex) {
      // TODO better method for handling this?
      System.err.println("Uncaught exception type:" + ex.getClass());
      ex.printStackTrace();
      throw new SketchException(ex.toString());
    }

    // grab the imports from the code just preproc'd

    importedLibraries = new ArrayList<Library>();
//    System.out.println("extra imports: " + result.extraImports);
    for (String item : result.extraImports) {
      // remove things up to the last dot
      int dot = item.lastIndexOf('.');
      // http://dev.processing.org/bugs/show_bug.cgi?id=1145
      String entry = (dot == -1) ? item : item.substring(0, dot);
//      System.out.println("library searching for " + entry);
      Library library = mode.getLibrary(entry);
//      System.out.println("  found " + library);

      if (library != null) {
        if (!importedLibraries.contains(library)) {
          importedLibraries.add(library);
          classPath += library.getClassPath();
          javaLibraryPath += File.pathSeparator + library.getNativePath();
        }
      } else {
        boolean found = false;
        // If someone insists on unnecessarily repeating the code folder
        // import, don't show an error for it.
        if (codeFolderPackages != null) {
          String itemPkg = item.substring(0, item.lastIndexOf('.'));
          for (String pkg : codeFolderPackages) {
            if (pkg.equals(itemPkg)) {
              found = true;
              break;
            }
          }
        }
        if (ignorableImport(item)) {
          found = true;
        }
        if (!found) {
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

    for (SketchCode sc : sketch.getCode()) {
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
          throw new SketchException(msg);
        }

      } else if (sc.isExtension("pde")) {
        // The compiler and runner will need this to have a proper offset
        sc.addPreprocOffset(result.headerOffset);
      }
    }
    foundMain = preprocessor.hasMethod("main");
    return result.className;
  }

  /**
   * Returns true if this package isn't part of a library (it's a system import
   * or something like that). Don't bother complaining about java.* or javax.*
   * because it's probably in boot.class.path. But we're not checking against
   * that path since it's enormous. Unfortunately we do still have to check
   * for libraries that begin with a prefix like javax, since that includes
   * the OpenGL library, even though we're just returning true here, hrm...
   */
  protected boolean ignorableImport(String pkg) {
    if (pkg.startsWith("java.")) return true;
    if (pkg.startsWith("javax.")) return true;
    return false;
  }


  protected int findErrorFile(int errorLine) {
    for (int i = sketch.getCodeCount() - 1; i > 0; i--) {
      SketchCode sc = sketch.getCode(i);
      if (sc.isExtension("pde") && (sc.getPreprocOffset() < errorLine)) {
        // keep looping until the errorLine is past the offset
        return i;
      }
    }
    return 0;  // i give up
  }


  /**
   * Path to the folder that will contain processed .java source files. Not
   * the location for .pde files, since that can be obtained from the sketch.
   */
  public File getSrcFolder() {
    return srcFolder;
  }


  public File getBinFolder() {
    return binFolder;
  }


  /**
   * Absolute path to the sketch folder. Used to set the working directry of
   * the sketch when running, i.e. so that saveFrame() goes to the right
   * location when running from the PDE, instead of the same folder as the
   * Processing.exe or the root of the user's home dir.
   */
  public String getSketchPath() {
    return sketch.getFolder().getAbsolutePath();
  }


  /** Class path determined during build. */
  public String getClassPath() {
    return classPath;
  }


  /** Return the java.library.path for this sketch (for all the native DLLs etc). */
  public String getJavaLibraryPath() {
    return javaLibraryPath;
  }


  /**
   * Whether the preprocessor found a main() method. If main() is found, then
   * it will be used to launch the sketch instead of PApplet.main().
   */
  public boolean getFoundMain() {
    return foundMain;
  }


  /**
   * Get the list of imported libraries. Used by external tools like Android mode.
   * @return list of library folders connected to this sketch.
   */
  public ArrayList<Library> getImportedLibraries() {
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
  public SketchException placeException(String message,
                                        String dotJavaFilename,
                                        int dotJavaLine) {
    int codeIndex = 0; //-1;
    int codeLine = -1;

//    System.out.println("placing " + dotJavaFilename + " " + dotJavaLine);
//    System.out.println("code count is " + getCodeCount());

    // first check to see if it's a .java file
    for (int i = 0; i < sketch.getCodeCount(); i++) {
      SketchCode code = sketch.getCode(i);
      if (code.isExtension("java")) {
        if (dotJavaFilename.equals(code.getFileName())) {
          codeIndex = i;
          codeLine = dotJavaLine;
          return new SketchException(message, codeIndex, codeLine);
        }
      }
    }

    // If not the preprocessed file at this point, then need to get out
    if (!dotJavaFilename.equals(sketch.getName() + ".java")) {
      return null;
    }

    // if it's not a .java file, codeIndex will still be 0
    // this section searches through the list of .pde files
    codeIndex = 0;
    for (int i = 0; i < sketch.getCodeCount(); i++) {
      SketchCode code = sketch.getCode(i);

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
//    return new SketchException(message, codeIndex, codeLine);
    return new SketchException(message, codeIndex, codeLine, -1, false);  // changed for 0194 for compile errors, but...
  }


  protected boolean exportApplet() throws SketchException, IOException {
    return exportApplet(new File(sketch.getFolder(), "applet"));
  }


  /**
   * Handle export to applet.
   */
  public boolean exportApplet(File appletFolder) throws SketchException, IOException {
    mode.prepareExportFolder(appletFolder);

    srcFolder = sketch.makeTempFolder();
    binFolder = sketch.makeTempFolder();
    String foundName = build(srcFolder, binFolder, true);

    // (already reported) error during export, exit this function
    if (foundName == null) return false;

    // If name != exportSketchName, then that's weirdness
    // BUG unfortunately, that can also be a bug in the preproc :(
    if (!sketch.getName().equals(foundName)) {
      Base.showWarning("Error during export",
                       "Sketch name is " + sketch.getName() + " but the\n" +
                       "name found in the code was " + foundName + ".", null);
      return false;
    }

    String[] sizeInfo =
      PdePreprocessor.parseSketchSize(sketch.getMainProgram(), false);
    int sketchWidth = PApplet.DEFAULT_WIDTH;
    int sketchHeight = PApplet.DEFAULT_HEIGHT;
    boolean openglApplet = false;
    boolean foundSize = false;
    if (sizeInfo != null) {
      try {
        if (sizeInfo[1] != null && sizeInfo[2] != null) {
          sketchWidth = Integer.parseInt(sizeInfo[1]);
          sketchHeight = Integer.parseInt(sizeInfo[2]);
          foundSize = true;
        }
      } catch (Exception e) {
        e.printStackTrace();
        // parsing errors, whatever; ignored
      }

      String sketchRenderer = sizeInfo[3];
      if (sketchRenderer != null) {
        if (sketchRenderer.equals("P2D") ||
            sketchRenderer.equals("P3D") ||
            sketchRenderer.equals("OPENGL")) {
          openglApplet = true;
        }
      }
    }
    if (!foundSize) {
      final String message =
        "The size of this applet could not automatically be\n" +
        "determined from your code. You'll have to edit the\n" +
        "HTML file to set the size of the applet.\n" +
        "Use only numeric values (not variables) for the size()\n" +
        "command. See the size() reference for an explanation.";
      Base.showWarning("Could not find applet size", message, null);
    }

//      // If the renderer is set to the built-in OpenGL library,
//      // then it's definitely an OpenGL applet.
//      if (sketchRenderer.equals("P3D") || sketchRenderer.equals("OPENGL")) {
//        openglApplet = true;
//      }


    /*
    int wide = PApplet.DEFAULT_WIDTH;
    int high = PApplet.DEFAULT_HEIGHT;
    String renderer = "";

    String scrubbed = PdePreprocessor.scrubComments(sketch.getCode(0).getProgram());
    String[] matches = PApplet.match(scrubbed, PdePreprocessor.SIZE_REGEX);

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
    */

    // Grab the Javadoc-style description from the main code.
    String description = "";
    // If there are multiple closings, need to catch the first,
    // which is what the (.*?) will do for us.
    // http://code.google.com/p/processing/issues/detail?id=877
    String[] javadoc = PApplet.match(sketch.getCode(0).getProgram(), "/\\*{2,}(.*?)\\*+/");
    if (javadoc != null) {
      StringBuffer dbuffer = new StringBuffer();
      String found = javadoc[1];
      String[] pieces = PApplet.split(found, '\n');
      for (String line : pieces) {
        // if this line starts with * characters, remove 'em
        String[] m = PApplet.match(line, "^\\s*\\*+(.*)");
        dbuffer.append(m != null ? m[1] : line);
        // insert the new line into the html to help w/ line breaks
        dbuffer.append('\n');
      }
      description = dbuffer.toString();
    }

    // Add links to all the code
    StringBuffer sources = new StringBuffer();
    //for (int i = 0; i < codeCount; i++) {
    for (SketchCode code : sketch.getCode()) {
      sources.append("<a href=\"" + code.getFileName() + "\">" +
                     code.getPrettyName() + "</a> ");
    }

    // Copy the source files to the target, since we like
    // to encourage people to share their code
//    for (int i = 0; i < codeCount; i++) {
    for (SketchCode code : sketch.getCode()) {
      try {
        File exportedSource = new File(appletFolder, code.getFileName());
        //Base.copyFile(code[i].getFile(), exportedSource);
        code.copyTo(exportedSource);

      } catch (IOException e) {
        e.printStackTrace();  // ho hum, just move on...
      }
    }
    // move the .java file from the preproc there too
    String preprocFilename = sketch.getName() + ".java";
    File preprocFile = new File(srcFolder, preprocFilename);
    if (preprocFile.exists()) {
      preprocFile.renameTo(new File(appletFolder, preprocFilename));
    } else {
      System.err.println("Could not copy source file: " + preprocFile.getAbsolutePath());
    }

    // Use separate .jar files whenever a library or code folder is in use.
    boolean separateJar =
      Preferences.getBoolean("export.applet.separate_jar_files") ||
      sketch.hasCodeFolder() ||
      javaLibraryPath.length() != 0;

    File skeletonFolder = mode.getContentFile("applet");

    // Copy the loading gif to the applet
    String LOADING_IMAGE = "loading.gif";
    // Check if the user already has their own loader image
    File loadingImage = new File(sketch.getFolder(), LOADING_IMAGE);
    if (!loadingImage.exists()) {
//      File skeletonFolder = new File(Base.getContentFile("lib"), "export");
      loadingImage = new File(skeletonFolder, LOADING_IMAGE);
    }
    Base.copyFile(loadingImage, new File(appletFolder, LOADING_IMAGE));

    // not a good idea after all
//    File deployFile = new File(skeletonFolder, "deployJava.js");
//    Base.copyFile(deployFile, new File(appletFolder, "deployJava.js"));

    // Create new .jar file
    FileOutputStream zipOutputFile =
      new FileOutputStream(new File(appletFolder, sketch.getName() + ".jar"));
    ZipOutputStream zos = new ZipOutputStream(zipOutputFile);
//    ZipEntry entry;

    StringBuffer archives = new StringBuffer();
    archives.append(sketch.getName() + ".jar");

    // Add the manifest file
    addManifest(zos);

//    File openglLibraryFolder =
//      new File(editor.getMode().getLibrariesFolder(), "opengl/library");
//    String openglLibraryPath = openglLibraryFolder.getAbsolutePath();
//    boolean openglApplet = false;

    HashMap<String,Object> zipFileContents = new HashMap<String,Object>();

    // add contents of 'library' folders
    for (Library library : importedLibraries) {
//      if (library.getPath().equals(openglLibraryPath)) {
      if (library.getName().equals("OpenGL")) {
        openglApplet = true;
      }
      for (File exportFile : library.getAppletExports()) {
        String exportName = exportFile.getName();
        if (!exportFile.exists()) {
          System.err.println("File " + exportFile.getAbsolutePath() + " does not exist");

        } else if (exportFile.isDirectory()) {
          System.out.println("Ignoring sub-folder \"" + exportFile.getAbsolutePath() + "\"");

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

    if (sketch.hasCodeFolder()) {
      File[] codeJarFiles = sketch.getCodeFolder().listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          if (name.charAt(0) == '.') return false;
          if (name.toLowerCase().endsWith(".jar")) return true;
          if (name.toLowerCase().endsWith(".zip")) return true;
          return false;
        }
      });
      for (File exportFile : codeJarFiles) {
        String name = exportFile.getName();
        Base.copyFile(exportFile, new File(appletFolder, name));
        archives.append("," + name);
      }
    }

    // Add the data folder to the output .jar file
    addDataFolder(zos);

    // add the project's .class files to the jar
    // just grabs everything from the build directory
    // since there may be some inner classes
    // (add any .class files from the applet dir, then delete them)
    // TODO this needs to be recursive (for packages)
    addClasses(zos, binFolder);

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
    File customHtml = new File(sketch.getFolder(), "applet.html");
    if (customHtml.exists()) {
      is = new FileInputStream(customHtml);
    }
//    for (File libraryFolder : importedLibraries) {
//      System.out.println(libraryFolder + " " + libraryFolder.getAbsolutePath());
//    }
    if (is == null) {
      if (openglApplet) {
        is = mode.getContentStream("applet/template-opengl.html");
      } else {
        is = mode.getContentStream("applet/template.html");
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
                     sketch.getName());
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
                     String.valueOf(sketchWidth));
        }
        while ((index = sb.indexOf("@@height@@")) != -1) {
          sb.replace(index, index + "@@height@@".length(),
                     String.valueOf(sketchHeight));
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
   * Export to application via GUI.
   */
  protected boolean exportApplication() throws IOException, SketchException {
    // Do the build once, so that we know what libraries are in use (and what
    // the situation is with their native libs), and also for efficiency of
    // not redoing the compilation for each platform. In particular, though,
    // importedLibraries won't be set until the preprocessing has finished,
    // so we have to do that before the stuff below.
    String foundName = build(true);

    // (already reported) error during export, exit this function
    if (foundName == null) return false;

    // if name != exportSketchName, then that's weirdness
    // BUG unfortunately, that can also be a bug in the preproc :(
    if (!sketch.getName().equals(foundName)) {
      Base.showWarning("Error during export",
                       "Sketch name is " + sketch.getName() + " but the sketch\n" +
                       "name in the code was " + foundName, null);
      return false;
    }

    File folder = null;
    for (String platformName : PConstants.platformNames) {
      int platform = Base.getPlatformIndex(platformName);
      if (Preferences.getBoolean("export.application.platform." + platformName)) {
        if (Library.hasMultipleArch(platform, importedLibraries)) {
          // export the 32-bit version
          folder = new File(sketch.getFolder(), "application." + platformName + "32");
          if (!exportApplication(folder, platform, 32)) {
            return false;
          }
          // export the 64-bit version
          folder = new File(sketch.getFolder(), "application." + platformName + "64");
          if (!exportApplication(folder, platform, 64)) {
            return false;
          }
        } else { // just make a single one for this platform
          folder = new File(sketch.getFolder(), "application." + platformName);
          if (!exportApplication(folder, platform, 0)) {
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
  private boolean exportApplication(File destFolder,
                                   int exportPlatform,
                                   int exportBits) throws IOException, SketchException {
    // TODO this should probably be a dialog box instead of a warning
    // on the terminal. And the message should be written better than this.
    // http://code.google.com/p/processing/issues/detail?id=884
    for (Library library : importedLibraries) {
      if (!library.supportsArch(exportPlatform, exportBits)) {
        String pn = PConstants.platformNames[exportPlatform];
        System.err.println("The application." + pn + exportBits +
                           " folder will not be created because no " +
                           exportBits + "-bit version of " +
                           library.getName() +
                           " is available for " + pn);
        return true;  // don't cancel export for this, just move along
      }
    }

    /// prep the output directory

    mode.prepareExportFolder(destFolder);


    /// figure out where the jar files will be placed

    File jarFolder = new File(destFolder, "lib");


    /// where all the skeleton info lives

    /// on macosx, need to copy .app skeleton since that's
    /// also where the jar files will be placed
    File dotAppFolder = null;
    if (exportPlatform == PConstants.MACOSX) {
      dotAppFolder = new File(destFolder, sketch.getName() + ".app");
//      String APP_SKELETON = "skeleton.app";
      //File dotAppSkeleton = new File(folder, APP_SKELETON);
      File dotAppSkeleton = mode.getContentFile("application/template.app");
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
      if (exportBits == 64) {
        // We don't yet have a 64-bit launcher, so this is a workaround for now.
        File batFile = new File(destFolder, sketch.getName() + ".bat");
        PrintWriter writer = PApplet.createWriter(batFile);
        writer.println("@echo off");
        writer.println("java -Djava.ext.dirs=lib -Djava.library.path=lib " + sketch.getName());
        writer.flush();
        writer.close();
      } else {
        Base.copyFile(mode.getContentFile("application/template.exe"),
                      new File(destFolder, sketch.getName() + ".exe"));
      }
    }


    /// start copying all jar files

    Vector<String> jarListVector = new Vector<String>();


    /// create the main .jar file

//    HashMap<String,Object> zipFileContents = new HashMap<String,Object>();

    FileOutputStream zipOutputFile =
      new FileOutputStream(new File(jarFolder, sketch.getName() + ".jar"));
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
//    addDataFolder(zos);
    // For 2.0a2, make the data folder a separate directory, rather than
    // packaging potentially large files into the JAR. On OS X, we have to hide
    // the folder inside the .app package, while Linux and Windows will have a
    // 'data' folder next to 'lib'.
    if (sketch.hasDataFolder()) {
      if (exportPlatform == PConstants.MACOSX) {
        Base.copyDir(sketch.getDataFolder(),  new File(jarFolder, "data"));
      } else {
        Base.copyDir(sketch.getDataFolder(),  new File(destFolder, "data"));
      }
    }

    // add the contents of the code folder to the jar
    if (sketch.hasCodeFolder()) {
      String includes = Base.contentsToClassPath(sketch.getCodeFolder());
      // Use tokens to get rid of extra blanks, which causes huge exports
      String[] codeList = PApplet.splitTokens(includes, File.pathSeparator);
//      String cp = "";
      for (int i = 0; i < codeList.length; i++) {
        if (codeList[i].toLowerCase().endsWith(".jar") ||
            codeList[i].toLowerCase().endsWith(".zip")) {
          File exportFile = new File(codeList[i]);
          String exportFilename = exportFile.getName();
          Base.copyFile(exportFile, new File(jarFolder, exportFilename));
          jarListVector.add(exportFilename);
        } else {
//          cp += codeList[i] + File.pathSeparator;
        }
      }
//      packClassPathIntoZipFile(cp, zos, zipFileContents);  // this was double adding the code folder prior to 2.0a2
    }

    zos.flush();
    zos.close();

    jarListVector.add(sketch.getName() + ".jar");


    /// add core.jar to the jar destination folder

    File bagelJar = Base.isMacOS() ?
      Base.getContentFile("core.jar") :
      Base.getContentFile("lib/core.jar");
    Base.copyFile(bagelJar, new File(jarFolder, "core.jar"));
    jarListVector.add("core.jar");


    /// add contents of 'library' folders to the export
    for (Library library : importedLibraries) {
      // add each item from the library folder / export list to the output
      for (File exportFile : library.getApplicationExports(exportPlatform, exportBits)) {
//        System.out.println("export: " + exportFile);
        String exportName = exportFile.getName();
        if (!exportFile.exists()) {
          System.err.println(exportFile.getName() +
                             " is mentioned in export.txt, but it's " +
                             "a big fat lie and does not exist.");

        } else if (exportFile.isDirectory()) {
          //System.err.println("Ignoring sub-folder \"" + exportList[i] + "\"");
//          if (exportPlatform == PConstants.MACOSX) {
//            // For OS X, copy subfolders to Contents/Resources/Java
          Base.copyDir(exportFile, new File(jarFolder, exportName));
//          } else {
//            // For other platforms, just copy the folder to the same directory
//            // as the application.
//            Base.copyDir(exportFile, new File(destFolder, exportName));
//          }

        } else if (exportName.toLowerCase().endsWith(".zip") ||
                   exportName.toLowerCase().endsWith(".jar")) {
          Base.copyFile(exportFile, new File(jarFolder, exportName));
          jarListVector.add(exportName);

          // old style, prior to 2.0a2
//        } else if ((exportPlatform == PConstants.MACOSX) &&
//                   (exportFile.getName().toLowerCase().endsWith(".jnilib"))) {
//          // jnilib files can be placed in Contents/Resources/Java
//          Base.copyFile(exportFile, new File(jarFolder, exportName));
//
//        } else {
//          // copy the file to the main directory.. prolly a .dll or something
//          Base.copyFile(exportFile, new File(destFolder, exportName));
//        }

          // first 2.0a2 attempt, until below...
//        } else if (exportPlatform == PConstants.MACOSX) {
//          Base.copyFile(exportFile, new File(jarFolder, exportName));
//
//        } else {
//          Base.copyFile(exportFile, new File(destFolder, exportName));
        } else {
          // Starting with 2.0a2 put extra export files (DLLs, plugins folder,
          // anything else for libraries) inside lib or Contents/Resources/Java
          Base.copyFile(exportFile, new File(jarFolder, exportName));
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
//    if (exportPlatform == PConstants.MACOSX) {
//      // If no bits specified (libs are all universal, or no native libs)
//      // then exportBits will be 0, and can be controlled via "Get Info".
//      // Otherwise, need to specify the bits as a VM option.
//      if (exportBits == 32) {
//        runOptions += " -d32";
//      } else if (exportBits == 64) {
//        runOptions += " -d64";
//      }
//    }

    /// macosx: write out Info.plist (template for classpath, etc)

    if (exportPlatform == PConstants.MACOSX) {
      String PLIST_TEMPLATE = "template.plist";
      File plistTemplate = new File(sketch.getFolder(), PLIST_TEMPLATE);
      if (!plistTemplate.exists()) {
        plistTemplate = mode.getContentFile("application/template.plist");
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
                       sketch.getName());
          }
          while ((index = sb.indexOf("@@classpath@@")) != -1) {
            sb.replace(index, index + "@@classpath@@".length(),
                       exportClassPath.toString());
          }
          while ((index = sb.indexOf("@@lsuipresentationmode@@")) != -1) {
            sb.replace(index, index + "@@lsuipresentationmode@@".length(),
                       Preferences.getBoolean("export.application.fullscreen") ? "4" : "0");
          }
          while ((index = sb.indexOf("@@lsarchitecturepriority@@")) != -1) {
            // More about this mess: http://support.apple.com/kb/TS2827
            // First default to exportBits == 0 case
            String arch = "<string>x86_64</string>\n      <string>i386</string>";
            if (exportBits == 32) {
              arch = "<string>i386</string>";
            } else if (exportBits == 64) {
              arch = "<string>x86_64</string>";
            }
            sb.replace(index, index + "@@lsarchitecturepriority@@".length(), arch);
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

      pw.println(sketch.getName());
      pw.println(exportClassPath);

      pw.flush();
      pw.close();

    } else {
      File shellScript = new File(destFolder, sketch.getName());
      PrintWriter pw = PApplet.createWriter(shellScript);

      // do the newlines explicitly so that windows CRLF
      // isn't used when exporting for unix
      pw.print("#!/bin/sh\n\n");
      //ps.print("APPDIR=`dirname $0`\n");
      pw.print("APPDIR=$(dirname \"$0\")\n");  // more posix compliant
      // another fix for bug #234, LD_LIBRARY_PATH ignored on some platforms
      //ps.print("LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$APPDIR\n");
      pw.print("java " + Preferences.get("run.options") +
               " -Djava.library.path=\"$APPDIR:$APPDIR/lib\"" +
               " -cp \"" + exportClassPath + "\"" +
               " " + sketch.getName() + "\n");

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

    for (SketchCode code : sketch.getCode()) {
      try {
        code.copyTo(new File(sourceFolder, code.getFileName()));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    // move the .java file from the preproc there too
    String preprocFilename = sketch.getName() + ".java";
    File preprocFile = new File(srcFolder, preprocFilename);
    if (preprocFile.exists()) {
      Base.copyFile(preprocFile, new File(sourceFolder, preprocFilename));
    } else {
      System.err.println("Could not copy source file: " + preprocFile.getAbsolutePath());
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
      "Main-Class: " + sketch.getName() + "\n";  // TODO not package friendly
    zos.write(contents.getBytes());
    zos.closeEntry();
  }


  protected void addClasses(ZipOutputStream zos, File dir) throws IOException {
    String path = dir.getAbsolutePath();
    if (!path.endsWith("/") && !path.endsWith("\\")) {
      path += '/';
    }
//    System.out.println("path is " + path);
    addClasses(zos, dir, path);
  }


  protected void addClasses(ZipOutputStream zos, File dir, String rootPath) throws IOException {
    File files[] = dir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return (name.charAt(0) != '.');
      }
    });
    for (File sub : files) {
      String relativePath = sub.getAbsolutePath().substring(rootPath.length());
//      System.out.println("relative path is " + relativePath);

      if (sub.isDirectory()) {
        addClasses(zos, sub, rootPath);

      } else if (sub.getName().endsWith(".class")) {
//        System.out.println("  adding item " + relativePath);
        ZipEntry entry = new ZipEntry(relativePath);
        zos.putNextEntry(entry);
        //zos.write(Base.loadBytesRaw(sub));
        PApplet.saveStream(zos, new FileInputStream(sub));
        zos.closeEntry();
      }
    }
  }


  protected void addDataFolder(ZipOutputStream zos) throws IOException {
    if (sketch.hasDataFolder()) {
      String[] dataFiles = Base.listFiles(sketch.getDataFolder(), false);
      int offset = sketch.getFolder().getAbsolutePath().length() + 1;
      for (String path : dataFiles) {
        if (Base.isWindows()) {
          path = path.replace('\\', '/');
        }
        //File dataFile = new File(dataFiles[i]);
        File dataFile = new File(path);
        if (!dataFile.isDirectory()) {
          // don't export hidden files
          // skipping dot prefix removes all: . .. .DS_Store
          if (dataFile.getName().charAt(0) != '.') {
            ZipEntry entry = new ZipEntry(path.substring(offset));
            zos.putNextEntry(entry);
            //zos.write(Base.loadBytesRaw(dataFile));
            PApplet.saveStream(zos, new FileInputStream(dataFile));
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
          //zos.write(Base.loadBytesRaw(sub));
          PApplet.saveStream(zos, new FileInputStream(sub));
          zos.closeEntry();
        }
      }
    }
  }
}

