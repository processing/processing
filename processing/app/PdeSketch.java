/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeSketch - stores information about files in the current sketch
  Part of the Processing project - http://Proce55ing.net

  Except where noted, code is written by Ben Fry
  Copyright (c) 2001-03 Massachusetts Institute of Technology

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

public class PdeSketch {
  String name; 
  File directory; 

  static final int PDE = 0;
  static final int JAVA = 1;

  int current;

  int fileCount;
  String names[];
  File files[];
  int flavor[]; 
  String program[];
  boolean modified[];
  PdeHistory history[];

  int hiddenCount;
  String hiddenNames[];
  File hiddenFiles[];

  //String sketchName; // name of the file (w/o pde if a sketch)
  //File sketchFile;   // the .pde file itself
  //File sketchDir;    // if a sketchbook project, the parent dir
  //boolean sketchModified;


  /**
   * path is location of the main .pde file, because this is also
   * simplest to use when opening the file from the finder/explorer.
   */
  public PdeSketch(String path) {
    File mainFile = new File(path);
    System.out.println("main file is " + mainFile);
    directory = new File(path.getParent());
    System.out.println("sketch dir is " + directory);
  
    // Build the list of files. This is only done once, rather than
    // each time a change is made, because otherwise it gets to be 
    // a nightmare to keep track of what files went where, because 
    // not all the data will be saved to disk.

    // get list of files in the sketch folder
    String list[] = directory.list();

    for (int i = 0; i < list.length; i++) {
      if (list[i].endsWith(".pde")) fileCount++;
      else if (list[i].endsWith(".java")) fileCount++;
      else if (list[i].endsWith(".pde.x")) hiddenCount++;
      else if (list[i].endsWith(".java.x")) hiddenCount++;
    }

    names = new String[fileCount];
    files = new File[fileCount];
    modified = new boolean[fileCount];
    flavor = new int[fileCount];
    program = new String[fileCount];
    history = new PdeHistory[fileCount];

    hiddenNames = new String[hiddenCount];
    hiddenFiles = new File[hiddenCount];

    int fileCounter = 0;
    int hiddenCounter = 0;

    for (int i = 0; i < list.length; i++) {
      int sub = 0;

      if (list[i].endsWith(".pde")) {
	names[fileCounter] = list[i].substring(0, list[i].length() - 4);
	files[fileCounter] = new File(directory, list[i]);
        flavor[fileCounter] = PDE;
	fileCounter++;

      } else if (list[i].endsWith(".java")) {
	names[fileCounter] = list[i].substring(0, list[i].length() - 5);
	files[fileCounter] = new File(directory, list[i]);
        flavor[fileCounter] = JAVA;
	fileCounter++;

      } else if (list[i].endsWith(".pde.x")) {
	names[hiddenCounter] = list[i].substring(0, list[i].length() - 6);
	files[hiddenCounter] = new File(directory, list[i]);
	hiddenCounter++;

      } else if (list[i].endsWith(".java.x")) {
	names[hiddenCounter] = list[i].substring(0, list[i].length() - 7);
	files[hiddenCounter] = new File(directory, list[i]);
	hiddenCounter++;
      }      
    }    
  }


  /**
   * Change the file currently being edited. 
   * 1. store the String for the text of the current file.
   * 2. retrieve the String for the text of the new file.
   * 3. change the text that's visible in the text area
   */
  public void setCurrent(int which) {
    // get the text currently being edited
    program[current] = editor.getText();

    // set to the text for this file
    // 'true' means to wipe out the undo buffer
    // (so they don't undo back to the other file.. whups!)
    editor.changeText(program[which], true); 

    // i'll personally make a note of the change
    current = which;
  }


  public void run() {
    try {
      String program = textarea.getText();
      history.record(program, PdeHistory.RUN);

      // if an external editor is being used, need to grab the
      // latest version of the code from the file.
      if (PdePreferences.getBoolean("editor.external")) {
        // history gets screwed by the open..
        String historySaved = history.lastRecorded;
        //handleOpen(sketchName, sketchFile, sketchDir);
        //handleOpen(sketch.name, sketch.file, sketch.directory);
        handleOpen(sketch);
        history.lastRecorded = historySaved;
      }

      // temporary build folder is inside 'lib'
      // this is added to the classpath by default
      tempBuildPath = "lib" + File.separator + "build";
      File buildDir = new File(tempBuildPath);
      if (!buildDir.exists()) {
        buildDir.mkdirs();
      }
      // copy (changed) files from data directory into build folder
      sketch.updateDataDirectory(buildDir);

      // make up a temporary class name to suggest
      int numero1 = (int) (Math.random() * 10000);
      int numero2 = (int) (Math.random() * 10000);
      //String className = TEMP_CLASS + "_" + numero1 + "_" + numero2;
      String className = "Temporary_" + numero1 + "_" + numero2;

      // handle building the code
      className = build(program, className, tempBuildPath, false);

      // if the compilation worked, run the applet
      if (className != null) {

        if (externalPaths == null) {
          externalPaths = 
            PdeCompiler.calcClassPath(null) + File.pathSeparator + 
            tempBuildPath;
        } else {
          externalPaths = 
            tempBuildPath + File.pathSeparator +
            PdeCompiler.calcClassPath(null) + File.pathSeparator +
            externalPaths;
        }

        // get a useful folder name for the 'code' folder
        // so that it can be included in the java.library.path
        String codeFolderPath = "";
        if (externalCode != null) {
          codeFolderPath = externalCode.getCanonicalPath();
        }

        // create a runtime object
        runtime = new PdeRuntime(this, className,
                                 externalRuntime, 
                                 codeFolderPath, externalPaths);

        // if programType is ADVANCED
        //   or the code/ folder is not empty -> or just exists (simpler)
        // then set boolean for external to true
        // include path to build in front, then path for code folder
        //   when passing the classpath through
        //   actually, build will already be in there, just prepend code

        // use the runtime object to consume the errors now
        //messageStream.setMessageConsumer(runtime);
        // no need to bother recycling the old guy
        PdeMessageStream messageStream = new PdeMessageStream(runtime);

        // start the applet
        runtime.start(presenting ? presentLocation : appletLocation,
                         new PrintStream(messageStream));
                         //leechErr);

        // spawn a thread to update PDE GUI state
        watcher = new RunButtonWatcher();

      } else {
        // [dmose] throw an exception here?
        // [fry] iirc the exception will have already been thrown
        cleanTempFiles(); //tempBuildPath);
      }
    } catch (PdeException e) { 
      // if it made it as far as creating a Runtime object, 
      // call its stop method to unwind its thread
      if (runtime != null) runtime.stop();
      cleanTempFiles(); //tempBuildPath);

      // printing the stack trace may be overkill since it happens
      // even on a simple parse error
      //e.printStackTrace();

      error(e);

    } catch (Exception e) {  // something more general happened
      e.printStackTrace();

      // if it made it as far as creating a Runtime object, 
      // call its stop method to unwind its thread
      if (runtime != null) runtime.stop();

      cleanTempFiles(); //tempBuildPath);
    }        


  /**
   * Have the contents of the currently visible tab been modified.
   */
  /*
  public boolean isCurrentModified() {
    return modified[current];
  }
  */


  // in an advanced program, the returned classname could be different,
  // which is why the className is set based on the return value.
  // @param exporting if set, then code is cleaner, 
  //                  but line numbers won't line up properly.
  //                  also modifies which imports (1.1 only) are included.
  // @return null if compilation failed, className if not
  //
  protected String build(String program, String className,
                         String buildPath, boolean exporting) 
    throws PdeException, Exception {

    // true if this should extend BApplet instead of BAppletGL
    //boolean extendsNormal = base.normalItem.getState();

    externalRuntime = false;
    externalPaths = null;

    externalCode = new File(sketchDir, "code");
    if (externalCode.exists()) {
      externalRuntime = true;
      externalPaths = PdeCompiler.includeFolder(externalCode);

    } else {
      externalCode = null;
    }

    // add the includes from the external code dir
    //
    String imports[] = null;
    if (externalCode != null) {
      imports = PdeCompiler.magicImports(externalPaths);
    }

    PdePreprocessor preprocessor = null;
    preprocessor = new PdePreprocessor(program, buildPath);
    try {
      className = 
        preprocessor.writeJava(className, imports, false);

    } catch (antlr.RecognitionException re) {
      // this even returns a column
      throw new PdeException(re.getMessage(), 
                             re.getLine() - 1, re.getColumn());

    } catch (antlr.TokenStreamRecognitionException tsre) {
      // while this seems to store line and column internally,
      // there doesn't seem to be a method to grab it.. 
      // so instead it's done using a regexp

      PatternMatcher matcher = new Perl5Matcher();
      PatternCompiler compiler = new Perl5Compiler();
      // line 3:1: unexpected char: 0xA0
      String mess = "^line (\\d+):(\\d+):\\s";
      Pattern pattern = compiler.compile(mess);

      PatternMatcherInput input = 
        new PatternMatcherInput(tsre.toString());
      if (matcher.contains(input, pattern)) {
        MatchResult result = matcher.getMatch();

        int line = Integer.parseInt(result.group(1).toString());
        int column = Integer.parseInt(result.group(2).toString());
        throw new PdeException(tsre.getMessage(), line-1, column);

      } else {
        throw new PdeException(tsre.toString());
      }

    } catch (PdeException pe) {
      throw pe;

    } catch (Exception ex) {
      System.err.println("Uncaught exception type:" + ex.getClass());
      ex.printStackTrace();
      throw new PdeException(ex.toString());
    }

    if (PdePreprocessor.programType == PdePreprocessor.ADVANCED) {
      externalRuntime = true; // we in advanced mode now, boy
    }

    // compile the program
    //
    PdeCompiler compiler = 
      new PdeCompiler(buildPath, className, externalCode, this);

    // run the compiler, and funnel errors to the leechErr
    // which is a wrapped around 
    // (this will catch and parse errors during compilation
    // the messageStream will call message() for 'compiler')
    messageStream = new PdeMessageStream(compiler);
    boolean success = compiler.compile(new PrintStream(messageStream));

    return success ? className : null;
  }


  /**
   * Returns true if this is a read-only sketch. Used for the 
   * examples directory, or when sketches are loaded from read-only
   * volumes or folders without appropraite permissions.
   */
  public boolean isReadOnly() {
    return false;
  }


  /**
   * Path to the data folder of this sketch.
   */
  /*
  public File getDataDirectory() {
    File dataDir = new File(directory, "data");
    return dataDir.exists() ? dataDir : null;
  }
  */

  // copy contents of data dir
  // eventually, if the files already exist in the target, don't' bother.
  public void updateDataDirectory(File buildDir) {
    File dataDir = new File(directory, "data");
    if (dataDir.exists()) {
      PdeBase.copyDir(dataDir, buildDir);
    }
  }



  /**
   * Returns path to the main .pde file for this sketch.
   */
  public String getMainFilePath() {
    return files[0].getAbsolutePath();
  }
}
