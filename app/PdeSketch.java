/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeSketch - stores information about files in the current sketch
  Part of the Processing project - http://processing.org

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

/*

Export to:  [ Applet (for the web)   + ]    [  OK  ]

[ ] OK to overwrite HTML file   <-- only visible if there is one there
                                    remembers previous setting as a pref


> Advanced

  Version: [ Java 1.1   + ]

  Using a version of Java other than 1.1 will require
  your Windows users to install the Java Plug-In, 
  and your Macintosh users to be running OS X.




Export to:    [ Application   + ]    [  OK  ]

> Advanced

  Platform: [ Mac OS X   + ]     <-- defaults to your current platform
              Windows
              jar file

  Exports the application as a double-clickable .app package, 
  compatible with Mac OS X. 

  Exports the application as a double-clickable .exe 
  and a handful of supporting files. 

  A jar file can be used on any platform that has Java
  installed. Simply doube-click the jar to run the application. 
  It is the simplest (but ugliest) method for exporting. 


  Version: [ Java 1.3   + ]

  Java 1.3 is the recommended setting for exporting
  applications. Applications will run on any Windows or
  Unix machine that has java installed, and on Mac OS X.



Export to:   [ Library       + ]    [  OK  ]

> Advanced   
  
  (no settings here?)

 */

public class PdeSketch {
  String path;  // path to 'main' file for this sketch

  String name;
  File directory;

  static final int PDE = 0;
  static final int JAVA = 1; 

  PdeCode current;
  int codeCount;
  PdeCode code[];

  int hiddenCount;
  PdeCode hidden[];


  /**
   * path is location of the main .pde file, because this is also
   * simplest to use when opening the file from the finder/explorer.
   */
  public PdeSketch(String path) throws IOException {
    File mainFile = new File(path);
    System.out.println("main file is " + mainFile);

    main = mainFile.getName();
    /*
    if (main.endsWith(".pde")) {
      main = main.substring(0, main.length() - 4);

    } else if (main.endsWith(".java")) {
      main = main.substring(0, main.length() - 5);
    }
    */

    directory = new File(path.getParent());
    System.out.println("sketch dir is " + directory);

    load();
  }


  /**
   * Build the list of files. 
   *
   * Generally his is only done once, rather than
   * each time a change is made, because otherwise it gets to be 
   * a nightmare to keep track of what files went where, because 
   * not all the data will be saved to disk.
   *
   * The exception is when an external editor is in use,
   * in which case the load happens each time "run" is hit.
   */
  public void load() {
    // get list of files in the sketch folder
    String list[] = directory.list();

    for (int i = 0; i < list.length; i++) {
      if (list[i].endsWith(".pde")) fileCount++;
      else if (list[i].endsWith(".java")) fileCount++;
      else if (list[i].endsWith(".pde.x")) hiddenCount++;
      else if (list[i].endsWith(".java.x")) hiddenCount++;
    }

    code = new PdeCode[codeCount];
    hidden = new PdeCode[hiddenCount];

    int fileCounter = 0;
    int hiddenCounter = 0;

    for (int i = 0; i < list.length; i++) {
      if (list[i].endsWith(".pde")) {
        code[fileCounter++] = 
          new PdeCode(list[i].substring(0, list[i].length() - 4), 
                      new File(directory, list[i]), 
                      PDE);

      } else if (list[i].endsWith(".java")) {
        code[fileCounter++] = 
          new PdeCode(list[i].substring(0, list[i].length() - 5),
                      new File(directory, list[i]),
                      JAVA);

      } else if (list[i].endsWith(".pde.x")) {
        hidden[hiddenCounter++] = 
          new PdeCode(list[i].substring(0, list[i].length() - 6),
                      new File(directory, list[i]),
                      PDE);

      } else if (list[i].endsWith(".java.x")) {
        hidden[hiddenCounter++] = 
          new PdeCode(list[i].substring(0, list[i].length() - 7),
                      new File(directory, list[i]),
                      JAVA);
      }      
    }

    // remove any entries that didn't load properly
    int index = 0;
    while (index < codeCount) {
      if (code[index].program == null) {
        //hide(index);  // although will this file be hidable?
        for (int i = index+1; i < codeCount; i++) {
          code[i-1] = code[i];
        }
        codeCount--;

      } else {
        index++;
      }
    }

    // move the main class to the first tab
    // start at 1, if it's at zero, don't bother
    for (int i = 1; i < codeCount; i++) {
      if (code[i].file.getName().equals("main")) {
        System.out.println("found main code at slot " + i);
        PdeCode temp = code[0];
        code[0] = code[i];
        code[i] = temp;
      }
    }

    // cheap-ass sort of the rest of the files
    // it's a dumb, slow sort, but there shouldn't be more than ~5 files
    for (int i = 1; i < codeCount; i++) {
      int who = i;
      //String who = code[i].name;
      //int whoIndex = i;
      for (int j = i + 1; j < codeCount; j++) {
        if (code[j].name.compare(code[who].name) < 0) {
          who = j;  // this guy is earlier in the alphabet
        }
      }
      if (who != i) {  // swap with someone
        PdeCode temp = code[who];
        code[who] = code[i];
        code[i] = temp;
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
    //program[current] = editor.getText();
    if (current != null) {
      current.program = editor.getText();
    }

    current = code[which];

    // set to the text for this file
    // 'true' means to wipe out the undo buffer
    // (so they don't undo back to the other file.. whups!)
    editor.changeText(current.program, true); 

    // and i'll personally make a note of the change
    //current = which;
  }


  /**
   * This is not Runnable.run(), but a handler for the run() command.
   *
   * run externally if a code folder exists, 
   * or if more than one file is in the project
   *
   */
  public void run() {
    try {
      current.program = textarea.getText();

      // TODO record history here
      //current.history.record(program, PdeHistory.RUN);

      // if an external editor is being used, need to grab the
      // latest version of the code from the file.
      if (PdePreferences.getBoolean("editor.external")) {
        // history gets screwed by the open..
        //String historySaved = history.lastRecorded;
        //handleOpen(sketch);
        //history.lastRecorded = historySaved;

        // nuke previous files and settings, just get things loaded
        load();
      }

      // temporary build folder is inside 'lib'
      // this is added to the classpath by default
      tempBuildPath = "lib" + File.separator + "build";
      File buildDir = new File(tempBuildPath);
      if (!buildDir.exists()) {
        buildDir.mkdirs();
      }

      // copy contents of data dir into lib/build
      // TODO write a file sync procedure here.. if the files 
      //      already exist in the target, or haven't been modified
      //      don't' bother. this can waste a lot of time when running.
      File dataDir = new File(directory, "data");
      if (dataDir.exists()) {
        // just drop the files in the build folder (pre-68)
        //PdeBase.copyDir(dataDir, buildDir);
        // drop the files into a 'data' subfolder of the build dir
        PdeBase.copyDir(dataDir, new File(buildDir, "data"));
      }

      // start with the main 

      // make up a temporary class name to suggest
      // only used if the code is not in ADVANCED mode
      String suggestedClassName = 
        ("Temporary_" + String.valueOf((int) (Math.random() * 10000)) +
         "_" + String.valueOf((int) (Math.random() * 10000)));

      // handle preprocessing the main file's code
      String mainClassName = build(tempBuildPath, suggestedClassName);
      // externalPaths is magically set by build()

      // if the compilation worked, run the applet
      if (mainClassName != null) {

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
  // @return null if compilation failed, className if not
  //
  protected String build(String buildPath, String suggestedClassName)
    throws PdeException, Exception {

    boolean externalRuntime = false;
    //externalPaths = null;
    String additionalImports[] = null;
    String additionalClassPath = null;

    // figure out the contents of the code folder to see if there
    // are files that need to be added to the imports
    File codeFolder = new File(sketchDir, "code");
    if (codeFolder.exists()) {
      externalRuntime = true;
      additionalClassPath = PdeCompiler.contentsToClassPath(codeFolder);
      additionalImports = PdeCompiler.magicImports(additionalClassPath);
    } else {
      codeFolder = null;
    }

    // first run preproc on the 'main' file, using the sugg class name
    // then for code 1..count
    //   if .java, write programs[i] to buildpath
    //   if .pde, run preproc to buildpath
    //     if no class def'd for the pde file, then complain

    PdePreprocessor preprocessor = new PdePreprocessor();
    try {
      mainClassName = 
        preprocessor.write(program, buildPath,
                           suggestedClassName, externalImports);

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

    if (PdePreprocessor.programType == PdePreprocessor.JAVA) {
      externalRuntime = true; // we in advanced mode now, boy
    }
    if (codeCount > 1) {
      externalRuntime = true;
    }

    // compile the program
    //
    PdeCompiler compiler = 
      new PdeCompiler(buildPath, mainClassName, externalCode, this);

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


  // move things around in the array (as opposed to full reload)

  // may need to call setCurrent() if the tab was the last one
  // or maybe just call setCurrent(0) for good measure

  // don't allow the user to hide the 0 tab (the main file)

  public void hide(int which) {
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


  /**
   * Returns path to the main .pde file for this sketch.
   */
  public String getMainFilePath() {
    return files[0].getAbsolutePath();
  }
}


class PdeCode {
  String name;  // pretty name (no extension), not the full file name
  File file;
  int flavor;

  String program;
  boolean modified;
  //History history;  // later


  public PdeCode(String name, File file, int flavor) {
    this.name = name;
    this.file = file;
    this.flavor = flavor;
  }


  public void load() {
    program = null;
    try {
      if (files[i].length() != 0) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(files[i])));
        StringBuffer buffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
          buffer.append(line);
          buffer.append('\n');
        }
        reader.close();
        program = buffer.toString();

      } else {
        // empty code file.. no worries, might be getting filled up
        program = "";
      }

    } catch (IOException e) {
      PdeBase.showWarning("Error loading file", 
                          "Error while opening the file\n" + 
                          files[i].getPath(), e);
      program = null;  // just in case
    }

    //if (program != null) {
    //history = new History(file);
    //}
  }
}
