package processing.app.tools.android;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.tools.ant.*;

import processing.app.*;
import processing.app.exec.*;
import processing.app.debug.RunnerException;
import processing.app.preproc.PdePreprocessor;
import processing.core.PApplet;


class Build {
  static SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd.HHmm");

  static String basePackage = "processing.android.test";

  private final Editor editor;
  private final AndroidSDK sdk;

  String className;

  File tempBuildFolder;

  File buildFile;

  static final String sdkVersion = "7";

  public Build(final Editor editor, final AndroidSDK sdk) {
    this.editor = editor;
    this.sdk = sdk;
  }

  // TODO this needs to be a generic function inside Sketch or elsewhere
  /*
  protected int[] getSketchSize() {
    int wide = AVD.DEFAULT_WIDTH;
    int high = AVD.DEFAULT_HEIGHT;
    // String renderer = "";

    // This matches against any uses of the size() function, whether numbers
    // or variables or whatever. This way, no warning is shown if size() isn't
    // actually used in the applet, which is the case especially for anyone
    // who is cutting/pasting from the reference.
    final String sizeRegex = "(?:^|\\s|;)size\\s*\\(\\s*(\\S+)\\s*,\\s*(\\d+),?\\s*([^\\)]*)\\s*\\)";

    final Sketch sketch = editor.getSketch();
    final String scrubbed = Sketch
        .scrubComments(sketch.getCode(0).getProgram());
    final String[] matches = PApplet.match(scrubbed, sizeRegex);

    if (matches != null) {
      try {
        wide = Integer.parseInt(matches[1]);
        high = Integer.parseInt(matches[2]);

        // Adding back the trim() for 0136 to handle Bug #769
        // if (matches.length == 4) renderer = matches[3].trim();

      } catch (final NumberFormatException e) {
        // found a reference to size, but it didn't
        // seem to contain numbers
        final String message = "The size of this applet could not automatically be\n"
            + "determined from your code.\n"
            + "Use only numeric values (not variables) for the size()\n"
            + "command. See the size() reference for an explanation.";

        Base.showWarning("Could not find sketch size", message, null);
      }
    }
    return new int[] { wide, high };
  }
  */

  
  public File createProject() {
    final Sketch sketch = editor.getSketch();

    try {
      tempBuildFolder = createTempBuildFolder(sketch);
    } catch (final IOException e) {
      editor.statusError(e);
      return null;
    }

    // Create the 'src' folder with the preprocessed code.
    final File srcFolder = new File(tempBuildFolder, "src");
//    Base.openFolder(tempBuildFolder);

    try {
      final File javaFolder = 
        mkdirs(srcFolder, getPackageName().replace('.', '/'));
      // File srcFile = new File(actualSrc, className + ".java");
      final String buildPath = javaFolder.getAbsolutePath();

      // String prefsLine = Preferences.get("preproc.imports");
      // System.out.println("imports are " + prefsLine);
      // Preferences.set("preproc.imports", "");

      // need to change to a better set of imports here

      // grab code from current editing window
      sketch.prepare();
      className = sketch.preprocess(buildPath, new Preproc(sketch.getName()));
      if (className != null) {
        final File androidXML = new File(tempBuildFolder, "AndroidManifest.xml");
        writeAndroidManifest(androidXML, sketch.getName(), className);
        writeBuildProps(new File(tempBuildFolder, "build.properties"));
        buildFile = new File(tempBuildFolder, "build.xml");
        writeBuildXML(buildFile, sketch.getName());
        writeDefaultProps(new File(tempBuildFolder, "default.properties"));
        writeLocalProps(new File(tempBuildFolder, "local.properties"));
        writeRes(new File(tempBuildFolder, "res"), className);

        final File libsFolder = mkdirs(tempBuildFolder, "libs");
        final File assetsFolder = mkdirs(tempBuildFolder, "assets");

        final InputStream input = PApplet.createInput(AndroidTool
            .getCoreZipLocation());
        PApplet.saveStream(new File(libsFolder, "processing-core.jar"), input);

        try {
          // Copy any imported libraries or code folder contents to the project
          writeLibraries(libsFolder, assetsFolder);

          // Copy the data folder, if one exists, to the 'assets' folder of the
          // project
          final File sketchDataFolder = sketch.getDataFolder();
          if (sketchDataFolder.exists()) {
            Base.copyDir(sketchDataFolder, assetsFolder);
          }
        } catch (final IOException e) {
          e.printStackTrace();
          throw new RunnerException(e.getMessage());
        }
      }
    } catch (final RunnerException e) {
      editor.statusError(e);
      return null;
    } catch (final IOException e) {
      editor.statusError(e);
      return null;
    }
    return tempBuildFolder;
  }

  /**
   * The Android dex util pukes on paths containing spaces, which will happen
   * most of the time on Windows, since Processing sketches wind up in
   * "My Documents". Therefore, build android in a temp file.
   * http://code.google.com/p/android/issues/detail?id=4567
   *
   * TODO: better would be to retrieve the 8.3 name for the sketch folder!
   *
   * @param sketch
   * @return A folder in which to build the android sketch
   * @throws IOException
   */
  private File createTempBuildFolder(final Sketch sketch) throws IOException {
    final File tmp = File.createTempFile("android", ".pde");
    if (!(tmp.delete() && tmp.mkdir())) {
      throw new IOException("Cannot create temp dir " + tmp
          + " to build android sketch");
    }
    return tmp;
  }
  
  
  protected File createExportFolder() throws IOException {
    Sketch sketch = editor.getSketch();
    // Create the 'android' build folder, and move any existing version out. 
    File androidFolder = new File(sketch.getFolder(), "android");
    if (androidFolder.exists()) {
      Date mod = new Date(androidFolder.lastModified());
      File dest = new File(sketch.getFolder(), "android." + dateFormat.format(mod));
      boolean result = androidFolder.renameTo(dest);
      if (!result) {
        ProcessHelper mv;
        ProcessResult pr;
        try {
          System.err.println("createProject renameTo() failed, resorting to mv/move instead.");
          mv = new ProcessHelper("mv", androidFolder.getAbsolutePath(), dest.getAbsolutePath());
          pr = mv.execute();

        } catch (IOException e) {
          editor.statusError(e);
          return null;

        } catch (InterruptedException e) {
          e.printStackTrace();
          return null;
        }
        if (!pr.succeeded()) {
          System.err.println(pr.getStderr());
          Base.showWarning("Failed to rename", 
                           "Could not rename the old “android” build folder.\n" + 
                           "Please delete, close, or rename the folder\n" + 
                           androidFolder.getAbsolutePath() + "\n" +  
                           "and try again." , null);
          Base.openFolder(sketch.getFolder());
          return null;
        }
      }
    } else {
      boolean result = androidFolder.mkdirs();
      if (!result) {
        Base.showWarning("Folders, folders, folders", 
                         "Could not create the necessary folders to build.\n" +
                         "Perhaps you have some file permissions to sort out?", null);
        return null;
      }
    }
    return androidFolder;
  }

  

  /**
   * @param target "debug" or "release"
   */
  protected boolean antBuild(final String target) {
    final Project p = new Project();
    String path = buildFile.getAbsolutePath().replace('\\', '/');
    p.setUserProperty("ant.file", path);

    // deals with a problem where javac error messages weren't coming through
    p.setUserProperty("build.compiler", "extJavac");
    // p.setUserProperty("build.compiler.emacs", "true"); // does nothing

    final DefaultLogger consoleLogger = new DefaultLogger();
    consoleLogger.setErrorPrintStream(System.err);
    //    consoleLogger.setOutputPrintStream(System.out);
    consoleLogger.setMessageOutputLevel(Project.MSG_ERR);
    p.addBuildListener(consoleLogger);

    final DefaultLogger errorLogger = new DefaultLogger();
    final ByteArrayOutputStream errb = new ByteArrayOutputStream();
    final PrintStream errp = new PrintStream(errb);
    errorLogger.setErrorPrintStream(errp);
    final ByteArrayOutputStream outb = new ByteArrayOutputStream();
    final PrintStream outp = new PrintStream(outb);
    errorLogger.setOutputPrintStream(outp);
    errorLogger.setMessageOutputLevel(Project.MSG_INFO);
    p.addBuildListener(errorLogger);

    try {
      editor.statusNotice("Building sketch for Android...");
      p.fireBuildStarted();
      p.init();
      final ProjectHelper helper = ProjectHelper.getProjectHelper();
      p.addReference("ant.projectHelper", helper);
      helper.parse(p, buildFile);
      // p.executeTarget(p.getDefaultTarget());
      p.executeTarget(target);
      editor.statusNotice("Finished building sketch.");
      return true;

    } catch (final BuildException e) {
      // Send a "build finished" event to the build listeners for this project.
      p.fireBuildFinished(e);

      // PApplet.println(new String(errb.toByteArray()));
      // PApplet.println(new String(outb.toByteArray()));

      // String errorOutput = new String(errb.toByteArray());
      // String[] errorLines =
      // errorOutput.split(System.getProperty("line.separator"));
      // PApplet.println(errorLines);

      final String outPile = new String(outb.toByteArray());
      final String[] outLines = outPile.split(System.getProperty("line.separator"));
      // PApplet.println(outLines);

      for (final String line : outLines) {
        final String javacPrefix = "[javac]";
        final int javacIndex = line.indexOf(javacPrefix);
        if (javacIndex != -1) {
          // System.out.println("checking: " + line);
          final Sketch sketch = editor.getSketch();
          // String sketchPath = sketch.getFolder().getAbsolutePath();
          final int offset = javacIndex + javacPrefix.length() + 1;
          final String[] pieces = PApplet.match(line.substring(offset),
            "^(.+):([0-9]+):\\s+(.+)$");
          if (pieces != null) {
            // PApplet.println(pieces);
            String fileName = pieces[1];
            // remove the path from the front of the filename
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            final int lineNumber = PApplet.parseInt(pieces[2]) - 1;
            // PApplet.println("looking for " + fileName + " line " +
            // lineNumber);
            final RunnerException rex = sketch.placeException(pieces[3],
              fileName, lineNumber);
            if (rex != null) {
              rex.hideStackTrace();
              editor.statusError(rex);
              return false; // get outta here
            }
          }
        }
      }
      editor.statusError(e);
    }
    return false;
  }

  
  protected String getPackageName() {
    return basePackage + "." + editor.getSketch().getName().toLowerCase();
  }

  
  protected String getClassName() {
    return className;
  }

  
  String getPathForAPK(final String target) {
    final Sketch sketch = editor.getSketch();
    String suffix = target.equals("release") ? "unsigned" : "debug";
    String apkName = "bin/" + sketch.getName() + "-" + suffix + ".apk";
    final File apkFile = new File(tempBuildFolder, apkName);
    return apkFile.getAbsolutePath();
  }

  
  class Preproc extends PdePreprocessor {

    public Preproc(final String sketchName) throws IOException {
      super(sketchName);
    }

    @Override
    protected int writeImports(final PrintWriter out,
                               final List<String> programImports,
                               final List<String> codeFolderImports) {
      out.println("package " + getPackageName() + ";");
      out.println();
      // add two lines for the package above
      return 2 + super.writeImports(out, programImports, codeFolderImports);
    }

    @Override
    public String[] getCoreImports() {
      return new String[] { "processing.core.*", "processing.xml.*" };
    }

    @Override
    public String[] getDefaultImports() {
      final String prefsLine = Preferences.get("android.preproc.imports.list");
      if (prefsLine != null) {
        return PApplet.splitTokens(prefsLine, ", ");
      }

      // In the future, this may include standard classes for phone or
      // accelerometer access within the Android APIs. This is currently living
      // in code rather than preferences.txt because Android mode needs to
      // maintain its independence from the rest of processing.app.
      final String[] androidImports = new String[] {
        "android.view.MotionEvent", "android.view.KeyEvent",
        "android.graphics.Bitmap", //"java.awt.Image",
        "java.io.*", // for BufferedReader, InputStream, etc
        //"java.net.*", "java.text.*", // leaving otu for now
        "java.util.*" // for ArrayList and friends
      //"java.util.zip.*", "java.util.regex.*" // not necessary w/ newer i/o
      };

      Preferences.set("android.preproc.imports.list", 
                      PApplet.join(androidImports, ","));

      return androidImports;
    }
  }

  
  private void writeAndroidManifest(final File file, final String sketchName,
                                    final String className) {
    final PrintWriter writer = PApplet.createWriter(file);
    writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    writer.println("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" ");
    writer.println("          package=\"" + getPackageName() + "\" ");
    writer.println("          android:versionCode=\"1\" ");
    writer.println("          android:versionName=\"1.0\">");

    writer.println("  <uses-sdk android:minSdkVersion=" + q(sdkVersion) + " />");
    
    writer.println("  <uses-permission android:name=\"android.permission.INTERNET\" />");
    writer.println("  <uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\" />");

    writer.println("  <application android:label=" + q("@string/app_name"));
    writer.println("               android:debuggable=" + q("true") + ">");
    writer.println("    <activity android:name=" + q("." + className));
    writer.println("              android:label=\"@string/app_name\">");
    writer.println("      <intent-filter>");
    writer.println("        <action android:name=\"android.intent.action.MAIN\" />");
    writer.println("        <category android:name=\"android.intent.category.LAUNCHER\" />");
    writer.println("      </intent-filter>");
    writer.println("    </activity>");
    writer.println("  </application>");
    writer.println("</manifest>");
    writer.flush();
    writer.close();
  }

  
  private void writeBuildProps(final File file) {
    final PrintWriter writer = PApplet.createWriter(file);
    writer.println("application-package=" + getPackageName());
    writer.flush();
    writer.close();
  }

  
  private void writeBuildXML(final File file, final String projectName) {
    final PrintWriter writer = PApplet.createWriter(file);
    writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");

    writer.println("<project name=\"" + projectName + "\" default=\"help\">");
    writer.println("  <property file=\"local.properties\"/>");
    writer.println("  <property file=\"build.properties\"/>");
    writer.println("  <property file=\"default.properties\"/>");

    writer.println("  <path id=\"android.antlibs\">");
    // writer.println("    <pathelement path=\"${sdk-location}/tools/lib/anttasks.jar\" />");
    // writer.println("    <pathelement path=\"${sdk-location}/tools/lib/sdklib.jar\" />");
    // writer.println("    <pathelement path=\"${sdk-location}/tools/lib/androidprefs.jar\" />");
    // writer.println("    <pathelement path=\"${sdk-location}/tools/lib/apkbuilder.jar\" />");
    // writer.println("    <pathelement path=\"${sdk-location}/tools/lib/jarutils.jar\" />");
    writer.println("    <pathelement path=\"${sdk.dir}/tools/lib/anttasks.jar\" />");
    writer.println("    <pathelement path=\"${sdk.dir}/tools/lib/sdklib.jar\" />");
    writer.println("    <pathelement path=\"${sdk.dir}/tools/lib/androidprefs.jar\" />");
    writer.println("    <pathelement path=\"${sdk.dir}/tools/lib/apkbuilder.jar\" />");
    writer.println("    <pathelement path=\"${sdk.dir}/tools/lib/jarutils.jar\" />");
    writer.println("  </path>");

    writer.println("  <taskdef name=\"setup\"");
    writer.println("           classname=\"com.android.ant.SetupTask\"");
    writer.println("           classpathref=\"android.antlibs\" />");

    writer.println("  <setup />");

    // copy the 'compile' target to the main build file, since the error
    // stream from javac isn't being passed through properly
    // writer.println("<target name=\"compile\" depends=\"resource-src, aidl\">");
    // writer.println("  <javac encoding=\"ascii\" target=\"1.5\" debug=\"true\" extdirs=\"\"");
    // writer.println("         destdir=\"${out-classes}\"");
    // writer.println("         bootclasspathref=\"android.target.classpath\">");
    // writer.println("    <src path=\"${source-folder}\" />");
    // writer.println("    <src path=\"${gen-folder}\" />");
    // writer.println("    <classpath>");
    // writer.println("      <fileset dir=\"${external-libs-folder}\" includes=\"*.jar\"/>");
    // writer.println("      <pathelement path=\"${main-out-classes}\"/>");
    // writer.println("    </classpath>");
    // writer.println("  </javac>");
    // writer.println("</target>");

    writer.println("</project>");
    writer.flush();
    writer.close();
  }

  
  private void writeDefaultProps(final File file) {
    final PrintWriter writer = PApplet.createWriter(file);
    writer.println("target=Google Inc.:Google APIs:" + sdkVersion);
    writer.flush();
    writer.close();
  }

  
  private void writeLocalProps(final File file) {
    final PrintWriter writer = PApplet.createWriter(file);
    final String sdkPath = sdk.getSdkFolder().getAbsolutePath();
    if (Base.isWindows()) {
      // Windows needs backslashes escaped, or it will also accept forward
      // slashes in the build file. We're using the forward slashes since this
      // path gets concatenated with a lot of others that use forwards anyway.
      writer.println("sdk.dir=" + sdkPath.replace('\\', '/'));
    } else {
      writer.println("sdk.dir=" + sdkPath);
    }
    writer.flush();
    writer.close();
  }

  
  private void writeRes(final File resFolder, final String className)
      throws RunnerException {
    final File layoutFolder = mkdirs(resFolder, "layout");
    final File layoutFile = new File(layoutFolder, "main.xml");
    writeResLayoutMain(layoutFile);
    final File valuesFolder = mkdirs(resFolder, "values");
    final File stringsFile = new File(valuesFolder, "strings.xml");
    writeResValuesStrings(stringsFile, className);
  }

  
  private File mkdirs(final File parent, final String name)
      throws RunnerException {
    final File result = new File(parent, name);
    if (!(result.exists() || result.mkdirs())) {
      throw new RunnerException("Could not create " + result);
    }
    return result;
  }

  
  private void writeResLayoutMain(final File file) {
    final PrintWriter writer = PApplet.createWriter(file);
    writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    writer.println("<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"");
    writer.println("              android:orientation=\"vertical\"");
    writer.println("              android:layout_width=\"fill_parent\"");
    writer.println("              android:layout_height=\"fill_parent\">");
    writer.println("</LinearLayout>");
    writer.flush();
    writer.close();
  }

  
  /** This recommended to be a string resource so that it can be localized. */
  private static void writeResValuesStrings(final File file,
                                            final String className) {
    final PrintWriter writer = PApplet.createWriter(file);
    writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    writer.println("<resources>");
    writer.println("  <string name=\"app_name\">" + className + "</string>");
    writer.println("</resources>");
    writer.flush();
    writer.close();
  }

  
  private void writeLibraries(final File libsFolder, final File assetsFolder)
      throws IOException {
    // Copy any libraries to the 'libs' folder
    final Sketch sketch = editor.getSketch();
    for (final File libraryFolder : sketch.getImportedLibraries()) {
      // in the list is a File object that points the
      // library sketch's "library" folder
      final File exportSettings = new File(libraryFolder, "export.txt");
      final HashMap<String, String> exportTable = Base
          .readSettings(exportSettings);
      final String androidList = exportTable.get("android");
      String exportList[] = null;
      if (androidList != null) {
        exportList = PApplet.splitTokens(androidList, ", ");
      } else {
        exportList = libraryFolder.list();
      }
      for (int i = 0; i < exportList.length; i++) {
        exportList[i] = PApplet.trim(exportList[i]);
        if (exportList[i].equals("") || exportList[i].equals(".")
            || exportList[i].equals("..")) {
          continue;
        }

        final File exportFile = new File(libraryFolder, exportList[i]);
        if (!exportFile.exists()) {
          System.err.println("File " + exportList[i] + " does not exist");
        } else if (exportFile.isDirectory()) {
          System.err.println("Ignoring sub-folder \"" + exportList[i] + "\"");
        } else {
          final String name = exportFile.getName();
          final String lcname = name.toLowerCase();
          if (lcname.endsWith(".zip") || lcname.endsWith(".jar")) {
            // As of r4 of the Android SDK, it looks like .zip files
            // are ignored in the libs folder, so rename to .jar
            final String jarName = 
              name.substring(0, name.length() - 4) + ".jar";
            Base.copyFile(exportFile, new File(libsFolder, jarName));
          } else {
            Base.copyFile(exportFile, new File(assetsFolder, name));
          }
        }
      }
    }

    // Copy files from the 'code' directory into the 'libs' folder
    final File codeFolder = sketch.getCodeFolder();
    if (codeFolder != null && codeFolder.exists()) {
      for (final File item : codeFolder.listFiles()) {
        if (!item.isDirectory()) {
          final String name = item.getName();
          final String lcname = name.toLowerCase();
          if (lcname.endsWith(".jar") || lcname.endsWith(".zip")) {
            final String jarName = name.substring(0, name.length() - 4)
                + ".jar";
            Base.copyFile(item, new File(libsFolder, jarName));
          }
        }
      }
    }
  }

  
  /**
   * Place quotes around a string to avoid dreadful syntax mess of escaping
   * quotes near quoted strings. Mmmm!
   */
  private static final String q(final String what) {
    return "\"" + what + "\"";
  }

  
  public void cleanup() {
    // don't want to be responsible for this
    //rm(tempBuildFolder);
    tempBuildFolder.deleteOnExit();
  }

//  private void rm(final File f) {
//    if (f.isDirectory()) {
//      final File[] kids = f.listFiles(new FilenameFilter() {
//        public boolean accept(final File dir, final String name) {
//          return !(name.equals(".") || name.equals(".."));
//        }
//      });
//      for (final File k : kids) {
//        rm(k);
//      }
//    }
//    f.delete();
//  }
}