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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import processing.app.*;
import processing.app.exec.ProcessHelper;
import processing.core.*;
import processing.data.StringList;
import processing.data.XML;
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
    StringList codeFolderPackages = null;
    if (sketch.hasCodeFolder()) {
      File codeFolder = sketch.getCodeFolder();
      javaLibraryPath = codeFolder.getAbsolutePath();

      // get a list of .jar files in the "code" folder
      // (class files in subfolders should also be picked up)
      String codeFolderClassPath =
        Util.contentsToClassPath(codeFolder);
      // append the jar files in the code folder to the class path
      classPath += File.pathSeparator + codeFolderClassPath;
      // get list of packages found in those jars
      codeFolderPackages =
        Util.packageListFromClassPath(codeFolderClassPath);

    } else {
      javaLibraryPath = "";
    }

    // 1. concatenate all .pde files to the 'main' pde
    //    store line number for starting point of each code bit

    StringBuilder bigCode = new StringBuilder();
    int bigCount = 0;
    for (SketchCode sc : sketch.getCode()) {
      if (sc.isExtension("pde")) {
        sc.setPreprocOffset(bigCount);
        bigCode.append(sc.getProgram());
        bigCode.append('\n');
        bigCount += sc.getLineCount();
      }
    }

    // initSketchSize() sets the internal sketchWidth/Height/Renderer vars
    // in the preprocessor. Those are used in preproc.write() so that they
    // can be used to add methods (settings() or sketchXxxx())
    //String[] sizeParts =
    SurfaceInfo sizeInfo =
      preprocessor.initSketchSize(sketch.getMainProgram(), sizeWarning);
    if (sizeInfo == null) {
      // An error occurred while trying to pull out the size, so exit here
      return null;
    }
    //System.out.format("size() is '%s'%n", info[0]);

    // Remove the entries being moved to settings(). They will be re-inserted
    // by writeFooter() when it emits the settings() method.
    if (sizeInfo != null && sizeInfo.hasSettings()) {
//      String sizeStatement = sizeInfo.getStatement();
      for (String stmt : sizeInfo.getStatements()) {
//      if (sizeStatement != null) {
        //System.out.format("size stmt is '%s'%n", sizeStatement);
        int index = bigCode.indexOf(stmt);
        if (index != -1) {
          bigCode.delete(index, index + stmt.length());
        } else {
          // TODO remove once we hit final; but prevent an exception like in
          // https://github.com/processing/processing/issues/3531
          System.err.format("Error removing '%s' from the code.", stmt);
        }
      }
    }

    PreprocessorResult result;
    try {
      File outputFolder = (packageName == null) ?
        srcFolder : new File(srcFolder, packageName.replace('.', '/'));
      outputFolder.mkdirs();
//      Base.openFolder(outputFolder);
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
//      System.out.println("error line is " + errorLine + ", file is " + errorFile);
      errorLine -= sketch.getCode(errorFile).getPreprocOffset();
//      System.out.println("  preproc offset for that file: " + sketch.getCode(errorFile).getPreprocOffset());

//      System.out.println("i found this guy snooping around..");
//      System.out.println("whatcha want me to do with 'im boss?");
//      System.out.println(errorLine + " " + errorFile + " " + code[errorFile].getPreprocOffset());

      String msg = re.getMessage();

      //System.out.println(java.getAbsolutePath());
//      System.out.println(bigCode);

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

    // grab the imports from the code just preprocessed

    importedLibraries = new ArrayList<Library>();
    Library core = mode.getCoreLibrary();
    if (core != null) {
      importedLibraries.add(core);
      classPath += core.getClassPath();
      javaLibraryPath += File.pathSeparator + core.getNativePath();
    }

//    System.out.println("extra imports: " + result.extraImports);
    for (String item : result.extraImports) {
//      System.out.println("item = '" + item + "'");
      // remove things up to the last dot
      int dot = item.lastIndexOf('.');
      // http://dev.processing.org/bugs/show_bug.cgi?id=1145
      String entry = (dot == -1) ? item : item.substring(0, dot);
//      System.out.print(entry + " => ");

      if (item.startsWith("static ")) {
        // import static - https://github.com/processing/processing/issues/8
        // Remove more stuff.
        int dot2 = item.lastIndexOf('.');
        entry = entry.substring(7, (dot2 == -1) ? entry.length() : dot2);
//        System.out.println(entry);
      }

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
          String itemPkg = entry;
          for (String pkg : codeFolderPackages) {
            if (pkg.equals(itemPkg)) {
              found = true;
              break;
            }
          }
        }
        if (ignorableImport(entry + '.')) {
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

    // But make sure that there isn't anything in there that's missing,
    // otherwise ECJ will complain and die. For instance, Java 1.7 (or maybe
    // it's appbundler?) adds Java/Classes to the path, which kills us.
    //String[] classPieces = PApplet.split(classPath, File.pathSeparator);
    // Nah, nevermind... we'll just create the @!#$! folder until they fix it.


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
            Util.saveFile(javaCode, new File(packageFolder, filename));
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

    if (pkg.startsWith("processing.core.")) return true;
    if (pkg.startsWith("processing.data.")) return true;
    if (pkg.startsWith("processing.event.")) return true;
    if (pkg.startsWith("processing.opengl.")) return true;

//    if (pkg.startsWith("com.jogamp.")) return true;

//    // ignore core, data, and opengl packages
//    String[] coreImports = preprocessor.getCoreImports();
//    for (int i = 0; i < coreImports.length; i++) {
//      String imp = coreImports[i];
//      if (imp.endsWith(".*")) {
//        imp = imp.substring(0, imp.length() - 2);
//      }
//      if (pkg.startsWith(imp)) {
//        return true;
//      }
//    }

    return false;
  }


  protected int findErrorFile(int errorLine) {
    for (int i = sketch.getCodeCount() - 1; i > 0; i--) {
      SketchCode sc = sketch.getCode(i);
      if (sc.isExtension("pde") && (sc.getPreprocOffset() <= errorLine)) {
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

      // Can only embed Java on the native platform
      boolean embedJava = (platform == PApplet.platform) &&
        Preferences.getBoolean("export.application.embed_java");

      if (Preferences.getBoolean(JavaEditor.EXPORT_PREFIX + platformName)) {
        if (Library.hasMultipleArch(platform, importedLibraries)) {
          // export the 32-bit version
          folder = new File(sketch.getFolder(), "application." + platformName + "32");
          if (!exportApplication(folder, platform, "32", embedJava && Base.getNativeBits() == 32 && "x86".equals(Base.getNativeArch()))) {
            return false;
          }
          // export the 64-bit version
          folder = new File(sketch.getFolder(), "application." + platformName + "64");
          if (!exportApplication(folder, platform, "64", embedJava && Base.getNativeBits() == 64 && "x86".equals(Base.getNativeArch()))) {
            return false;
          }
          if (platform == PConstants.LINUX) {
            // export the armv6hf version as well
            folder = new File(sketch.getFolder(), "application.linux-armv6hf");
            if (!exportApplication(folder, platform, "armv6hf", embedJava && Base.getNativeBits() == 32 && "arm".equals(Base.getNativeArch()))) {
              return false;
            }
          }
        } else { // just make a single one for this platform
          folder = new File(sketch.getFolder(), "application." + platformName);
          if (!exportApplication(folder, platform, "", embedJava)) {
            return false;
          }
        }
      }
    }

    /*
    File folder = null;
    String platformName = Base.getPlatformName();
    boolean embedJava = Preferences.getBoolean("export.application.embed_java");
    if (Library.hasMultipleArch(PApplet.platform, importedLibraries)) {
      if (Base.getNativeBits() == 32) {
        // export the 32-bit version
        folder = new File(sketch.getFolder(), "application." + platformName + "32");
        if (!exportApplication(folder, PApplet.platform, 32, embedJava)) {
          return false;
        }
      } else if (Base.getNativeBits() == 64) {
        // export the 64-bit version
        folder = new File(sketch.getFolder(), "application." + platformName + "64");
        if (!exportApplication(folder, PApplet.platform, 64, embedJava)) {
          return false;
        }
      }
    } else { // just make a single one for this platform
      folder = new File(sketch.getFolder(), "application." + platformName);
      if (!exportApplication(folder, PApplet.platform, 0, embedJava)) {
        return false;
      }
    }
    */
    return true;  // all good
  }


  /**
   * Export to application without GUI. Also called by the Commander.
   */
  protected boolean exportApplication(File destFolder,
                                      int exportPlatform,
                                      String exportVariant,
                                      boolean embedJava) throws IOException, SketchException {
    // TODO this should probably be a dialog box instead of a warning
    // on the terminal. And the message should be written better than this.
    // http://code.google.com/p/processing/issues/detail?id=884
    for (Library library : importedLibraries) {
      if (!library.supportsArch(exportPlatform, exportVariant)) {
        String pn = PConstants.platformNames[exportPlatform];
        Base.showWarning("Quibbles 'n Bits",
                         "The application." + pn + exportVariant +
                         " folder will not be created\n" +
                         "because no " + exportVariant + " version of " +
                         library.getName() + " is available for " + pn, null);
        return true;  // don't cancel all exports for this, just move along
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
    String jvmRuntime = "";
    String jdkPath = null;
    if (exportPlatform == PConstants.MACOSX) {
      dotAppFolder = new File(destFolder, sketch.getName() + ".app");

      File contentsOrig = new File(Base.getJavaHome(), "../../../../..");

      if (embedJava) {
        File jdkFolder = new File(Base.getJavaHome(), "../../..");
        String jdkFolderName = jdkFolder.getCanonicalFile().getName();
        jvmRuntime = "<key>JVMRuntime</key>\n    <string>" + jdkFolderName + "</string>";
        jdkPath = new File(dotAppFolder, "Contents/PlugIns/" + jdkFolderName).getAbsolutePath();
      }

      File contentsFolder = new File(dotAppFolder, "Contents");
      contentsFolder.mkdirs();

      // Info.plist will be written later

      // set the jar folder to a different location than windows/linux
      //jarFolder = new File(dotAppFolder, "Contents/Resources/Java");
      jarFolder = new File(contentsFolder, "Java");

      File macosFolder = new File(contentsFolder, "MacOS");
      macosFolder.mkdirs();
      Util.copyFile(new File(contentsOrig, "MacOS/Processing"),
                    new File(contentsFolder, "MacOS/" + sketch.getName()));

      File pkgInfo = new File(contentsFolder, "PkgInfo");
      PrintWriter writer = PApplet.createWriter(pkgInfo);
      writer.println("APPL????");
      writer.flush();
      writer.close();

      // Use faster(?) native copy here (also to do sym links)
      if (embedJava) {
        Util.copyDirNative(new File(contentsOrig, "PlugIns"),
                           new File(contentsFolder, "PlugIns"));
      }

      File resourcesFolder = new File(contentsFolder, "Resources");
      Util.copyDir(new File(contentsOrig, "Resources/en.lproj"),
                   new File(resourcesFolder, "en.lproj"));
      Util.copyFile(mode.getContentFile("application/sketch.icns"),
                    new File(resourcesFolder, "sketch.icns"));

      /*
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
      */
    } else if (exportPlatform == PConstants.LINUX) {
      if (embedJava) {
        Util.copyDirNative(Base.getJavaHome(), new File(destFolder, "java"));
      }

    } else if (exportPlatform == PConstants.WINDOWS) {
      if (embedJava) {
        Util.copyDir(Base.getJavaHome(), new File(destFolder, "java"));
      }
    }


    /// make the jar folder (all platforms)

    if (!jarFolder.exists()) jarFolder.mkdirs();


    /*
    /// on windows, copy the exe file

    if (exportPlatform == PConstants.WINDOWS) {
      if (exportBits == 64) {
        // We don't yet have a 64-bit launcher, so this is a workaround for now.
        File batFile = new File(destFolder, sketch.getName() + ".bat");
        PrintWriter writer = PApplet.createWriter(batFile);
        writer.println("@echo off");
        String javaPath = embedJava ? ".\\java\\bin\\java.exe" : "java";
        writer.println(javaPath + " -Djna.nosys=true -Djava.ext.dirs=lib -Djava.library.path=lib " + sketch.getName());
        writer.flush();
        writer.close();
      } else {
        Base.copyFile(mode.getContentFile("application/template.exe"),
                      new File(destFolder, sketch.getName() + ".exe"));
      }
    }
    */


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
        Util.copyDir(sketch.getDataFolder(),  new File(jarFolder, "data"));
      } else {
        Util.copyDir(sketch.getDataFolder(),  new File(destFolder, "data"));
      }
    }

    // add the contents of the code folder to the jar
    if (sketch.hasCodeFolder()) {
      String includes = Util.contentsToClassPath(sketch.getCodeFolder());
      // Use tokens to get rid of extra blanks, which causes huge exports
      String[] codeList = PApplet.splitTokens(includes, File.pathSeparator);
      for (int i = 0; i < codeList.length; i++) {
        if (codeList[i].toLowerCase().endsWith(".jar") ||
            codeList[i].toLowerCase().endsWith(".zip")) {
          File exportFile = new File(codeList[i]);
          String exportFilename = exportFile.getName();
          Util.copyFile(exportFile, new File(jarFolder, exportFilename));
          jarListVector.add(exportFilename);
        } else {
//          cp += codeList[i] + File.pathSeparator;
        }
      }
    }

    zos.flush();
    zos.close();

    jarListVector.add(sketch.getName() + ".jar");


    /// add contents of 'library' folders to the export
    for (Library library : importedLibraries) {
      // add each item from the library folder / export list to the output
      for (File exportFile : library.getApplicationExports(exportPlatform, exportVariant)) {
//        System.out.println("export: " + exportFile);
        String exportName = exportFile.getName();
        if (!exportFile.exists()) {
          System.err.println(exportFile.getName() +
                             " is mentioned in export.txt, but it's " +
                             "a big fat lie and does not exist.");

        } else if (exportFile.isDirectory()) {
          Util.copyDir(exportFile, new File(jarFolder, exportName));

        } else if (exportName.toLowerCase().endsWith(".zip") ||
                   exportName.toLowerCase().endsWith(".jar")) {
          Util.copyFile(exportFile, new File(jarFolder, exportName));
          jarListVector.add(exportName);

        } else {
          // Starting with 2.0a2 put extra export files (DLLs, plugins folder,
          // anything else for libraries) inside lib or Contents/Resources/Java
          Util.copyFile(exportFile, new File(jarFolder, exportName));
        }
      }
    }


    /// create platform-specific CLASSPATH based on included jars

    String jarList[] = new String[jarListVector.size()];
    jarListVector.copyInto(jarList);
    StringBuilder exportClassPath = new StringBuilder();

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
      exportClassPath.append("$APPDIR");
      for (int i = 0; i < jarList.length; i++) {
        exportClassPath.append(":$APPDIR/lib/" + jarList[i]);
      }
    }


    /// figure out run options for the VM

    List<String> runOptions = new ArrayList<String>();
    if (Preferences.getBoolean("run.options.memory")) {
      runOptions.add("-Xms" + Preferences.get("run.options.memory.initial") + "m");
      runOptions.add("-Xmx" + Preferences.get("run.options.memory.maximum") + "m");
    }
    // https://github.com/processing/processing/issues/2239
    runOptions.add("-Djna.nosys=true");
    // https://github.com/processing/processing/issues/2559
    if (exportPlatform == PConstants.WINDOWS) {
      runOptions.add("-Djava.library.path=\"%EXEDIR%\\lib\"");
    }


    /// macosx: write out Info.plist (template for classpath, etc)

    if (exportPlatform == PConstants.MACOSX) {
      StringBuilder runOptionsXML = new StringBuilder();
      for (String opt : runOptions) {
        runOptionsXML.append("      <string>");
        runOptionsXML.append(opt);
        runOptionsXML.append("</string>");
        runOptionsXML.append('\n');
      }

      String PLIST_TEMPLATE = "Info.plist.tmpl";
      File plistTemplate = new File(sketch.getFolder(), PLIST_TEMPLATE);
      if (!plistTemplate.exists()) {
        plistTemplate = mode.getContentFile("application/" + PLIST_TEMPLATE);
      }
      File plistFile = new File(dotAppFolder, "Contents/Info.plist");
      PrintWriter pw = PApplet.createWriter(plistFile);

      String lines[] = PApplet.loadStrings(plistTemplate);
      for (int i = 0; i < lines.length; i++) {
        if (lines[i].indexOf("@@") != -1) {
          StringBuilder sb = new StringBuilder(lines[i]);
          int index = 0;
          while ((index = sb.indexOf("@@jvm_runtime@@")) != -1) {
            sb.replace(index, index + "@@jvm_runtime@@".length(),
                       jvmRuntime);
          }
          while ((index = sb.indexOf("@@jvm_options_list@@")) != -1) {
            sb.replace(index, index + "@@jvm_options_list@@".length(),
                       runOptionsXML.toString());
          }
          while ((index = sb.indexOf("@@sketch@@")) != -1) {
            sb.replace(index, index + "@@sketch@@".length(),
                       sketch.getName());
          }
          while ((index = sb.indexOf("@@lsuipresentationmode@@")) != -1) {
            sb.replace(index, index + "@@lsuipresentationmode@@".length(),
                       Preferences.getBoolean("export.application.present") ? "4" : "0");
          }

          lines[i] = sb.toString();
        }
        // explicit newlines to avoid Windows CRLF
        pw.print(lines[i] + "\n");
      }
      pw.flush();
      pw.close();

      // attempt to code sign if the Xcode tools appear to be installed
      if (Base.isMacOS() && new File("/usr/bin/codesign_allocate").exists()) {
        if (embedJava) {
          ProcessHelper.ffs("codesign", "--force", "--sign", "-", jdkPath);
        }
        String appPath = dotAppFolder.getAbsolutePath();
        ProcessHelper.ffs("codesign", "--force", "--sign", "-", appPath);
      }

    } else if (exportPlatform == PConstants.WINDOWS) {
      File buildFile = new File(destFolder, "launch4j-build.xml");
      File configFile = new File(destFolder, "launch4j-config.xml");

      XML project = new XML("project");
      XML target = project.addChild("target");
      target.setString("name", "windows");

      XML taskdef = target.addChild("taskdef");
      taskdef.setString("name", "launch4j");
      taskdef.setString("classname", "net.sf.launch4j.ant.Launch4jTask");
      String launchPath = mode.getContentFile("application/launch4j").getAbsolutePath();
      taskdef.setString("classpath", launchPath + "/launch4j.jar:" + launchPath + "/lib/xstream.jar");

      XML launch4j = target.addChild("launch4j");
      // not all launch4j options are available when embedded inside the ant
      // build file (i.e. the icon param doesn't work), so use a config file
      //<launch4j configFile="windows/work/config.xml" />
      launch4j.setString("configFile", configFile.getAbsolutePath());

      XML config = new XML("launch4jConfig");
      config.addChild("headerType").setContent("gui");
      config.addChild("dontWrapJar").setContent("true");
      config.addChild("downloadUrl").setContent("http://java.com/download");

      File exeFile = new File(destFolder, sketch.getName() + ".exe");
      config.addChild("outfile").setContent(exeFile.getAbsolutePath());

      File iconFile = mode.getContentFile("application/sketch.ico");
      config.addChild("icon").setContent(iconFile.getAbsolutePath());

      XML clazzPath = config.addChild("classPath");
      clazzPath.addChild("mainClass").setContent(sketch.getName());
      for (String jarName : jarList) {
        clazzPath.addChild("cp").setContent("lib/" + jarName);
      }
      XML jre = config.addChild("jre");
      if (embedJava) {
        jre.addChild("path").setContent("java");
      }
      jre.addChild("minVersion").setContent("1.7.0_40");
      for (String opt : runOptions) {
        jre.addChild("opt").setContent(opt);
      }

      /*
      XML config = launch4j.addChild("config");
      config.setString("headerType", "gui");
      File exeFile = new File(destFolder, sketch.getName() + ".exe");
      config.setString("outfile", exeFile.getAbsolutePath());
      config.setString("dontWrapJar", "true");
      config.setString("jarPath", "lib\\" + jarList[0]);

      File iconFile = mode.getContentFile("application/sketch.ico");
      config.addChild("icon").setContent(iconFile.getAbsolutePath());

      XML clazzPath = config.addChild("classPath");
      clazzPath.setString("mainClass", sketch.getName());
      for (int i = 1; i < jarList.length; i++) {
        String jarName = jarList[i];
        clazzPath.addChild("cp").setContent("lib\\" + jarName);
      }
      XML jre = config.addChild("jre");
      jre.setString("minVersion", "1.7.0_40");
      //PApplet.join(runOptions.toArray(new String[0]), " ")
      for (String opt : runOptions) {
        jre.addChild("opt").setContent(opt);
      }
      */

      config.save(configFile);
      project.save(buildFile);
      if (!buildWindowsLauncher(buildFile, "windows")) {
        // don't delete the build file, might be useful for debugging
        return false;
      }
      configFile.delete();
      buildFile.delete();

    } else {
      File shellScript = new File(destFolder, sketch.getName());
      PrintWriter pw = PApplet.createWriter(shellScript);

      // Do the newlines explicitly so that Windows CRLF
      // isn't used when exporting for Unix.
      pw.print("#!/bin/sh\n\n");
      //ps.print("APPDIR=`dirname $0`\n");
      pw.print("APPDIR=$(dirname \"$0\")\n");  // more posix compliant
      // another fix for bug #234, LD_LIBRARY_PATH ignored on some platforms
      //ps.print("LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$APPDIR\n");
      if (embedJava) {
        // https://github.com/processing/processing/issues/2349
        pw.print("$APPDIR/java/bin/");
      }
      String runOptionsStr =
        PApplet.join(runOptions.toArray(new String[0]), " ");
      pw.print("java " + runOptionsStr +
               " -Djava.library.path=\"$APPDIR:$APPDIR/lib\"" +
               " -cp \"" + exportClassPath + "\"" +
               " " + sketch.getName() + " \"$@\"\n");

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
      Util.copyFile(preprocFile, new File(sourceFolder, preprocFilename));
    } else {
      System.err.println("Could not copy source file: " + preprocFile.getAbsolutePath());
    }


    /// goodbye
    return true;
  }


  /**
   * Run the launch4j build.xml file through ant to create the exe.
   * Most of this code was lifted from Android mode.
   */
  protected boolean buildWindowsLauncher(File buildFile, String target) {
    Project p = new Project();
    String path = buildFile.getAbsolutePath().replace('\\', '/');
    p.setUserProperty("ant.file", path);

    // deals with a problem where javac error messages weren't coming through
    p.setUserProperty("build.compiler", "extJavac");

    // too chatty
    /*
    // try to spew something useful to the console
    final DefaultLogger consoleLogger = new DefaultLogger();
    consoleLogger.setErrorPrintStream(System.err);
    consoleLogger.setOutputPrintStream(System.out);
    // WARN, INFO, VERBOSE, DEBUG
    consoleLogger.setMessageOutputLevel(Project.MSG_ERR);
    p.addBuildListener(consoleLogger);
    */

    DefaultLogger errorLogger = new DefaultLogger();
    ByteArrayOutputStream errb = new ByteArrayOutputStream();
    PrintStream errp = new PrintStream(errb);
    errorLogger.setErrorPrintStream(errp);
    ByteArrayOutputStream outb = new ByteArrayOutputStream();
    PrintStream outp = new PrintStream(outb);
    errorLogger.setOutputPrintStream(outp);
    errorLogger.setMessageOutputLevel(Project.MSG_INFO);
    p.addBuildListener(errorLogger);

    try {
      p.fireBuildStarted();
      p.init();
      final ProjectHelper helper = ProjectHelper.getProjectHelper();
      p.addReference("ant.projectHelper", helper);
      helper.parse(p, buildFile);
      p.executeTarget(target);
      return true;

    } catch (final BuildException e) {
      // Send a "build finished" event to the build listeners for this project.
      p.fireBuildFinished(e);

      String out = new String(outb.toByteArray());
      String err = new String(errb.toByteArray());
      System.out.println(out);
      System.err.println(err);
    }
    return false;
  }


  protected void addManifest(ZipOutputStream zos) throws IOException {
    ZipEntry entry = new ZipEntry("META-INF/MANIFEST.MF");
    zos.putNextEntry(entry);

    String contents =
      "Manifest-Version: 1.0\n" +
      "Created-By: Processing " + Base.getVersionName() + "\n" +
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
      String[] dataFiles = Util.listFiles(sketch.getDataFolder(), false);
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
          file.close();

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
