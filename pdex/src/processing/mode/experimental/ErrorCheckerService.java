/*
 * Copyright (C) 2012-14 Manindra Moharana <me@mkmoharana.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package processing.mode.experimental;

import static processing.mode.experimental.ExperimentalMode.log;
import static processing.mode.experimental.ExperimentalMode.logE;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;

import processing.app.Base;
import processing.app.Editor;
import processing.app.EditorStatus;
import processing.app.Library;
import processing.app.SketchCode;
import processing.app.syntax.SyntaxDocument;
import processing.core.PApplet;
import processing.mode.java.preproc.PdePreprocessor;

/**
 * The main error checking service
 * 
 * @author Manindra Moharana &lt;me@mkmoharana.com&gt;
 *
 */
public class ErrorCheckerService implements Runnable{
  
  protected DebugEditor editor;
  /**
   * Error check happens every sleepTime milliseconds
   */
  public static final int sleepTime = 1000;

  /**
   * The amazing eclipse ast parser
   */
  protected ASTParser parser;
 
  /**
   * Used to indirectly stop the Error Checker Thread
   */
  protected AtomicBoolean stopThread;

  /**
   * If true, Error Checking is paused. Calls to checkCode() become useless.
   */
  protected AtomicBoolean pauseThread;

  protected ErrorWindow errorWindow;

  /**
   * IProblem[] returned by parser stored in here
   */
  protected IProblem[] problems;

  /**
   * Class name of current sketch
   */
  protected String className;

  /**
   * Source code of current sketch
   */
  protected String sourceCode;

  /**
   * URLs of extra imports jar files stored here.
   */
  protected URL[] classpath;

  /**
   * Stores all Problems in the sketch
   */
  public ArrayList<Problem> problemsList;

  /**
   * How many lines are present till the initial class declaration? In static
   * mode, this would include imports, class declaration and setup
   * declaration. In nomral mode, this would include imports, class
   * declaration only. It's fate is decided inside preprocessCode()
   */
  public int mainClassOffset;

  /**
   * Fixed p5 offsets for all sketches
   */
  public int defaultImportsOffset;
  
  /**
   * Is the sketch running in static mode or active mode?
   */
  public boolean staticMode = false;

  /**
   * Compilation Unit for current sketch
   */
  protected CompilationUnit cu;

  /**
   * If true, compilation checker will be reloaded with updated classpath
   * items.
   */
  protected boolean loadCompClass = true;

  /**
   * Compiler Checker class. Note that methods for compilation checking are
   * called from the compilationChecker object, not from this
   */
  protected Class<?> checkerClass;

  /**
   * Compilation Checker object.
   */
  protected Object compilationChecker;

  
  /**
   * List of jar files to be present in compilation checker's classpath
   */
  protected ArrayList<URL> classpathJars;

  /**
   * Timestamp - for measuring total overhead
   */
  protected long lastTimeStamp = System.currentTimeMillis();

  /**
   * Used for displaying the rotating slash on the Problem Window title bar
   */
  protected String[] slashAnimation = { "|", "/", "--", "\\", "|", "/", "--",
      "\\" };
  protected int slashAnimationIndex = 0;

  /**
   * Used to detect if the current tab index has changed and thus repaint the
   * textarea.
   */
  public int currentTab = 0, lastTab = 0;

  /**
   * Stores the current import statements in the program. Used to compare for
   * changed import statements and update classpath if needed.
   */
  protected ArrayList<ImportStatement> programImports;

  /**
   * List of imports when sketch was last checked. Used for checking for
   * changed imports
   */
  protected ArrayList<ImportStatement> previousImports = new ArrayList<ImportStatement>();

  /**
   * Teh Preprocessor
   */
  protected XQPreprocessor xqpreproc;

  /**
   * Regexp for import statements. (Used from Processing source)
   */
  final public String importRegexp = "(?:^|;)\\s*(import\\s+)((?:static\\s+)?\\S+)(\\s*;)";
  
  /**
   * Regexp for function declarations. (Used from Processing source)
   */
  final Pattern FUNCTION_DECL = Pattern
    .compile("(^|;)\\s*((public|private|protected|final|static)\\s+)*"
      + "(void|int|float|double|String|char|byte)"
      + "(\\s*\\[\\s*\\])?\\s+[a-zA-Z0-9]+\\s*\\(", Pattern.MULTILINE);
  
  protected ErrorMessageSimplifier errorMsgSimplifier;
  
  public ErrorCheckerService(DebugEditor debugEditor) {
    this.editor = debugEditor;
    stopThread = new AtomicBoolean(false);
    pauseThread = new AtomicBoolean(false);
    
    problemsList = new ArrayList<Problem>();
    classpathJars = new ArrayList<URL>();
    
    initParser();
    initializeErrorWindow();
    xqpreproc = new XQPreprocessor();
    PdePreprocessor pdePrepoc = new PdePreprocessor(null);
    defaultImportsOffset = pdePrepoc.getCoreImports().length + 
        pdePrepoc.getDefaultImports().length + 1;
    astGenerator = new ASTGenerator(this);
    syntaxErrors = new AtomicBoolean(true);
    containsErrors = new AtomicBoolean(true);
    errorMsgSimplifier = new ErrorMessageSimplifier();
    tempErrorLog = new TreeMap<String, IProblem>();
    sketchChangedListener = new SketchChangedListener();
//    for (final SketchCode sc : editor.getSketch().getCode()) {
//      sc.getDocument().addDocumentListener(sketchChangedListener);
//    }
  }
  
  /**
   * Initializes ASTParser
   */
  protected void initParser() {
    try {
      parser = ASTParser.newParser(AST.JLS4);
    } catch (Exception e) {
      System.err.println("Experimental Mode initialization failed. "
          + "Are you running the right version of Processing? ");
      pauseThread();
    } catch (Error e) {
      System.err.println("Experimental Mode initialization failed. ");
      e.printStackTrace();
      pauseThread();
    }
  }
  
  /**
   * Initialiazes the Error Window
   */
  public void initializeErrorWindow() {
    
    if (editor == null) {
      return;
    }
    
    if (errorWindow != null) {
      return;
    }

    final ErrorCheckerService thisService = this;
    final DebugEditor thisEditor = editor;
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          errorWindow = new ErrorWindow(thisEditor, thisService);
          // errorWindow.setVisible(true);
          editor.toFront();
          errorWindow.errorTable.setFocusable(false);
          editor.setSelection(0, 0);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  public void run() {
    stopThread.set(false);
    
    checkCode();
    if(!hasSyntaxErrors())
      editor.showProblemListView(XQConsoleToggle.CONSOLE);
    // Make sure astGen has at least one CU to start with
    // This is when the loaded sketch already has syntax errors.
    // Completion wouldn't be complete, but it'd be still something
    // better than nothing
    astGenerator.buildAST(cu); 
    handleErrorCheckingToggle();
    while (!stopThread.get()) {
      try {
        // Take a nap.
        Thread.sleep(sleepTime);
      } catch (Exception e) {
        log("Oops! [ErrorCheckerThreaded]: " + e);
        // e.printStackTrace();
      }
      
      updatePaintedThingys();
      updateEditorStatus();
      updateSketchCodeListeners();
      if (pauseThread.get())
        continue;
      if(textModified.get() == 0)
    	  continue;
      // Check every x seconds
      checkCode();
      checkForMissingImports();
    }
    
    astGenerator.disposeAllWindows();
    compilationChecker = null;
    checkerClass = null;
    classLoader = null;
    System.gc();
    logE("Thread stopped: " + editor.getSketch().getName());
    System.gc();
  }
  
  protected void updateSketchCodeListeners() {
    for (final SketchCode sc : editor.getSketch().getCode()) {
      boolean flag = false;
      for (DocumentListener dl : ((SyntaxDocument)sc.getDocument()).getDocumentListeners()) {
        if(dl.equals(sketchChangedListener)){
          flag = true;
          break;
        }
      }
      if(!flag){
        log("Adding doc listener to " + sc.getPrettyName());
        sc.getDocument().addDocumentListener(sketchChangedListener);
      }
    }
  }

  protected void checkForMissingImports() {
    for (Problem p : problemsList) {
      if(p.getMessage().endsWith(" cannot be resolved to a type"));{
        int idx = p.getMessage().indexOf(" cannot be resolved to a type");
        if(idx > 1){
          String missingClass = p.getMessage().substring(0, idx);
          //log("Will suggest for type:" + missingClass);
          astGenerator.suggestImports(missingClass);
        }
      }
    }
  }

  protected ASTGenerator astGenerator;
  
  public ASTGenerator getASTGenerator() {
    return astGenerator;
  }

  /**
   * This thing acts as an event queue counter of sort.
   * Since error checking happens on demand, anytime this counter
   * goes above 0, error check is triggered, and counter reset.
   * It's thread safe to avoid any mess.
   */
  protected AtomicInteger textModified = new AtomicInteger();
  
  /**
   * Triggers error check
   */
  public void runManualErrorCheck() {
    log("Error Check.");
    textModified.incrementAndGet();
  }
  
  protected SketchChangedListener sketchChangedListener;
  protected class SketchChangedListener implements DocumentListener{
    
    private SketchChangedListener(){
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
      if (ExperimentalMode.errorCheckEnabled){
        runManualErrorCheck();
        log("doc insert update, man error check..");
      }
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      if (ExperimentalMode.errorCheckEnabled){
        runManualErrorCheck();
        log("doc remove update, man error check..");
      }
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
      if (ExperimentalMode.errorCheckEnabled){
        runManualErrorCheck();
        log("doc changed update, man error check..");
      }
    }
    
  }

  protected boolean checkCode() {
    //log("checkCode() " + textModified.get() );
    log("checkCode() " + textModified.get());
    lastTimeStamp = System.currentTimeMillis();
    try {
      sourceCode = preprocessCode(editor.getSketch().getMainProgram());

      syntaxCheck();
      log(editor.getSketch().getName() + "1 MCO "
          + mainClassOffset);
      // No syntax errors, proceed for compilation check, Stage 2.
      
      //if(hasSyntaxErrors()) astGenerator.buildAST(null);
      if (problems.length == 0 && editor.compilationCheckEnabled) {
        //mainClassOffset++; // just a hack.
        
        sourceCode = xqpreproc.doYourThing(sourceCode, programImports);
        prepareCompilerClasspath();
//        mainClassOffset = xqpreproc.mainClassOffset; // tiny, but
//                                // significant
//        if (staticMode) {
//        	mainClassOffset++; // Extra line for setup() decl.
//        }
        //         log(sourceCode);
        //         log("--------------------------");
        compileCheck();        
        log(editor.getSketch().getName() + "2 MCO "
            + mainClassOffset);
      }
      astGenerator.buildAST(cu);
      if(ExperimentalMode.errorCheckEnabled){
        updateErrorTable();
        editor.updateErrorBar(problemsList);
        updateEditorStatus();
        editor.getTextArea().repaint();
        updatePaintedThingys();
        editor.updateErrorToggle();
      }
      else
      {
        log("Error Check disabled, so not updating UI.");
      }
      int x = textModified.get();
      //log("TM " + x);
      if (x >= 2) {
        textModified.set(2);
        x = 2;
      }

      if (x > 0)
        textModified.set(x - 1);
      else
        textModified.set(0);
      return true;

    } catch (Exception e) {
      log("Oops! [ErrorCheckerService.checkCode]: " + e);
      e.printStackTrace();
    }
    return false;
  }
  
  protected AtomicBoolean syntaxErrors, containsErrors;
  
  public boolean hasSyntaxErrors(){
    return syntaxErrors.get();
  }
  
  public boolean hasErrors(){
    return containsErrors.get();
  }
  
  protected TreeMap<String, IProblem> tempErrorLog;

  protected void syntaxCheck() {
    syntaxErrors.set(true);
    containsErrors.set(true);
    parser.setSource(sourceCode.toCharArray());
    parser.setKind(ASTParser.K_COMPILATION_UNIT);

    Map<String, String> options = JavaCore.getOptions();

    JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
    options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
    options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
    parser.setCompilerOptions(options);
    
    if (cu == null)
      cu = (CompilationUnit) parser.createAST(null);
    else {
      synchronized (cu) {
        cu = (CompilationUnit) parser.createAST(null);
      }
    }
    
    synchronized (problemsList) {
      
      // Store errors returned by the ast parser
      problems = cu.getProblems();
      // log("Problem Count: " + problems.length);
      // Populate the probList
      problemsList = new ArrayList<Problem>();
      for (int i = 0; i < problems.length; i++) {
        int a[] = calculateTabIndexAndLineNumber(problems[i].getSourceLineNumber());
        Problem p = new Problem(problems[i], a[0], a[1] + 1);
        //TODO: ^Why do cheeky stuff?
        problemsList.add(p);
//      log(problems[i].getMessage());
//      for (String j : problems[i].getArguments()) {
//        log("arg " + j);
//      }
        // log(p.toString());
      }

      if (problems.length == 0) {
        syntaxErrors.set(false);
        containsErrors.set(false);
      } else {
        syntaxErrors.set(true);
        containsErrors.set(true);
      }
    }
  }
  
  protected URLClassLoader classLoader;
  
  protected void compileCheck() {
    
    // CU needs to be updated coz before compileCheck xqpreprocessor is run on the source code which makes some further changes
    //TODO Check if this breaks things 
    
    parser.setSource(sourceCode.toCharArray());
    parser.setKind(ASTParser.K_COMPILATION_UNIT);

    Map<String, String> options = JavaCore.getOptions();

    JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
    options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
    options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
    parser.setCompilerOptions(options);
    
    if (cu == null)
      cu = (CompilationUnit) parser.createAST(null);
    else {
      synchronized (cu) {
        cu = (CompilationUnit) parser.createAST(null);
      }
    }

    // Currently (Sept, 2012) I'm using Java's reflection api to load the
    // CompilationChecker class(from CompilationChecker.jar) that houses the
    // Eclispe JDT compiler, and call its getErrorsAsObj method to obtain
    // errors. This way, I'm able to add the paths of contributed libraries
    // to the classpath of CompilationChecker, dynamically. The eclipse compiler
    // needs all referenced libraries in the classpath. Totally a hack. If you find
    // a better method, do let me know.

    try {

      // NOTE TO SELF: If classpath contains null Strings
      // URLClassLoader shoots NPE bullets.

      // If imports have changed, reload classes with new classpath.
      if (loadCompClass) {

        // if (classpathJars.size() > 0)
        // System.out
        // .println("Experimental Mode: Loading contributed libraries referenced by import statements.");
        
        // The folder SketchBook/modes/ExperimentalMode/mode
        File f = editor.getMode().getContentFile("mode");
        
        if(!f.exists()) {
        	System.err.println("Could not locate the files required for on-the-fly error checking. Bummer.");
        	return;
        }
        
        FileFilter fileFilter = new FileFilter() {
          public boolean accept(File file) {
            return (file.getName().endsWith(".jar") && !file
                .getName().startsWith(editor.getMode().getClass().getSimpleName()));
          }
        };

        File[] jarFiles = f.listFiles(fileFilter);
        // log( "Jar files found? " + (jarFiles != null));
        //for (File jarFile : jarFiles) {
          //classpathJars.add(jarFile.toURI().toURL());
        //}
        
        classpath = new URL[classpathJars.size() + jarFiles.length]; 
        int ii = 0;
        for (; ii < classpathJars.size(); ii++) {
          classpath[ii] = classpathJars.get(ii);
        }
        for (int i = 0; i < jarFiles.length; i++) {
          classpath[ii++] = jarFiles[i].toURI().toURL();
        }
        
        compilationChecker = null;
        checkerClass = null;
        classLoader = null;
        System.gc();
        // log("CP Len -- " + classpath.length);
        classLoader = new URLClassLoader(classpath);
        // log("1.");
        checkerClass = Class.forName("CompilationChecker", true,
            classLoader);
        // log("2.");
        compilationChecker = checkerClass.newInstance();
        
        loadCompClass = false;
      }

      if (compilerSettings == null) {
        prepareCompilerSetting();
      }
      Method getErrors = checkerClass.getMethod("getErrorsAsObjArr",
          new Class[] { String.class, String.class, Map.class });

      Object[][] errorList = (Object[][]) getErrors
          .invoke(compilationChecker, className, sourceCode,
              compilerSettings);

      if (errorList == null) {
        return;
      }
      
      synchronized (problemsList) {
        problems = new DefaultProblem[errorList.length];
  
        for (int i = 0; i < errorList.length; i++) {
  
          // for (int j = 0; j < errorList[i].length; j++)
          // System.out.print(errorList[i][j] + ", ");
  
          problems[i] = new DefaultProblem((char[]) errorList[i][0],
              (String) errorList[i][1],
              ((Integer) errorList[i][2]).intValue(),
              (String[]) errorList[i][3],
              ((Integer) errorList[i][4]).intValue(),
              ((Integer) errorList[i][5]).intValue(),
              ((Integer) errorList[i][6]).intValue(),
              ((Integer) errorList[i][7]).intValue(), 0);
  
          // System.out
          // .println("ECS: " + problems[i].getMessage() + ","
          // + problems[i].isError() + ","
          // + problems[i].isWarning());
  
          IProblem problem = problems[i];
  //        log(problem.getMessage());
  //        for (String j : problem.getArguments()) {
  //          log("arg " + j);
  //        }
          int a[] = calculateTabIndexAndLineNumber(problem.getSourceLineNumber());
          Problem p = new Problem(problem, a[0], a[1]);
          if ((Boolean) errorList[i][8]) {
            p.setType(Problem.ERROR);
            containsErrors.set(true); // set flag
          }
          
          if ((Boolean) errorList[i][9]) {
            p.setType(Problem.WARNING);
          }
  
          // If warnings are disabled, skip 'em
          if (p.isWarning() && !ExperimentalMode.warningsEnabled) {
            continue;
          }
          problemsList.add(p);
        }
      }
    } catch (ClassNotFoundException e) {
      System.err.println("Compiltation Checker files couldn't be found! "
          + e + " compileCheck() problem.");
      pauseThread();
    } catch (MalformedURLException e) {
      System.err.println("Compiltation Checker files couldn't be found! "
          + e + " compileCheck() problem.");
      pauseThread();
    } catch (Exception e) {
      System.err.println("compileCheck() problem." + e);
      e.printStackTrace();
      pauseThread();
    } catch (NoClassDefFoundError e) {
      System.err
          .println(e
              + " compileCheck() problem. Somebody tried to mess with Experimental Mode files.");
      pauseThread();
    } catch(OutOfMemoryError e) {
      System.err.println("Processing has used up its maximum alloted memory. Please close some Processing " +
    " windows and then reopen this sketch.");
      pauseThread();
    }
    
    // log("Compilecheck, Done.");
  }
  
  private int loadClassCounter = 0;
  public URLClassLoader getSketchClassLoader() {
    loadClassCounter++;
    if(loadClassCounter > 100){
      loadClassCounter = 0;
      classLoader = null;
      System.gc();
      classLoader = new URLClassLoader(classpath);
    }
    return classLoader;
  }

  /**
   * Processes import statements to obtain classpaths of contributed
   * libraries. This would be needed for compilation check. Also, adds
   * stuff(jar files, class files, candy) from the code folder. And it looks
   * messed up.
   * 
   */
  protected void prepareCompilerClasspath() {
    if (!loadCompClass) {
      return;
    }
    
    synchronized (classpathJars) {
      // log("1..");
      classpathJars = new ArrayList<URL>();
      String entry = "";
      boolean codeFolderChecked = false;
      for (ImportStatement impstat : programImports) {
        String item = impstat.getImportName();
        int dot = item.lastIndexOf('.');
        entry = (dot == -1) ? item : item.substring(0, dot);
  
        entry = entry.substring(6).trim();
        // log("Entry--" + entry);
        if (ignorableImport(entry)) {
          // log("Ignoring: " + entry);
          continue;
        }
        Library library = null;
  
        // Try to get the library classpath and add it to the list
        try {
          library = editor.getMode().getLibrary(entry);
          // log("lib->" + library.getClassPath() + "<-");
          String libraryPath[] = PApplet.split(library.getClassPath()
              .substring(1).trim(), File.pathSeparatorChar);
          for (int i = 0; i < libraryPath.length; i++) {
            // log(entry + " ::"
            // + new File(libraryPath[i]).toURI().toURL());
            classpathJars.add(new File(libraryPath[i]).toURI().toURL());
          }
          // log("-- ");
          // classpath[count] = (new File(library.getClassPath()
          // .substring(1))).toURI().toURL();
          // log("  found ");
          // log(library.getClassPath().substring(1));
        } catch (Exception e) {
          if (library == null && !codeFolderChecked) {
            // log(1);
            // Look around in the code folder for jar files
            if (editor.getSketch().hasCodeFolder()) {
              File codeFolder = editor.getSketch().getCodeFolder();
  
              // get a list of .jar files in the "code" folder
              // (class files in subfolders should also be picked up)
              String codeFolderClassPath = Base
                  .contentsToClassPath(codeFolder);
              codeFolderChecked = true;
              if (codeFolderClassPath.equalsIgnoreCase("")) {
                System.err.println("Experimental Mode: Yikes! Can't find \""
                    + entry
                    + "\" library! Line: "
                    + impstat.getLineNumber()
                    + " in tab: "
                    + editor.getSketch().getCode(impstat.getTab())
                        .getPrettyName());
                System.out
                    .println("Please make sure that the library is present in <sketchbook "
                        + "folder>/libraries folder or in the code folder of your sketch");
  
              }
              String codeFolderPath[] = PApplet.split(
                  codeFolderClassPath.substring(1).trim(),
                  File.pathSeparatorChar);
              try {
                for (int i = 0; i < codeFolderPath.length; i++) {
                  classpathJars.add(new File(codeFolderPath[i])
                      .toURI().toURL());
                }
  
              } catch (Exception e2) {
                System.out
                    .println("Yikes! codefolder, prepareImports(): "
                        + e2);
              }
            } else {
              System.err.println("Experimental Mode: Yikes! Can't find \""
                  + entry
                  + "\" library! Line: "
                  + impstat.getLineNumber()
                  + " in tab: "
                  + editor.getSketch().getCode(impstat.getTab())
                      .getPrettyName());
              System.out
                  .println("Please make sure that the library is present in <sketchbook "
                      + "folder>/libraries folder or in the code folder of your sketch");
            }
  
          } else {
            System.err
                .println("Yikes! There was some problem in prepareImports(): "
                    + e);
            System.err.println("I was processing: " + entry);
  
            // e.printStackTrace();
          }
        }
  
      }
    }
    astGenerator.loadJars(); // update jar file for completion lookup  
  }
  
  /**
   * Ignore processing packages, java.*.*. etc.
   * 
   * @param packageName
   * @return boolean
   */
  protected boolean ignorableImport(String packageName) {
    // packageName.startsWith("processing.")
    // ||
    if (packageName.startsWith("java.") || packageName.startsWith("javax.")) {
      return true;
    }
    return false;
  }

  /**
   * Various option for JDT Compiler
   */
  @SuppressWarnings("rawtypes")
  protected Map compilerSettings;

  /**
   * Sets compiler options for JDT Compiler
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected void prepareCompilerSetting() {
    compilerSettings = new HashMap();

    compilerSettings.put(CompilerOptions.OPTION_LineNumberAttribute,
        CompilerOptions.GENERATE);
    compilerSettings.put(CompilerOptions.OPTION_SourceFileAttribute,
        CompilerOptions.GENERATE);
    compilerSettings.put(CompilerOptions.OPTION_Source,
        CompilerOptions.VERSION_1_6);
    compilerSettings.put(CompilerOptions.OPTION_ReportUnusedImport,
        CompilerOptions.IGNORE);
    compilerSettings.put(CompilerOptions.OPTION_ReportMissingSerialVersion,
        CompilerOptions.IGNORE);
    compilerSettings.put(CompilerOptions.OPTION_ReportRawTypeReference,
        CompilerOptions.IGNORE);
    compilerSettings.put(
        CompilerOptions.OPTION_ReportUncheckedTypeOperation,
        CompilerOptions.IGNORE);
  }

  
  /**
   * Updates the error table in the Error Window.
   */
  public void updateErrorTable() {

    try {
      String[][] errorData = new String[problemsList.size()][3];
      for (int i = 0; i < problemsList.size(); i++) {
        errorData[i][0] = problemsList.get(i).getMessage(); ////TODO: this is temporary
            //+ " : " + errorMsgSimplifier.getIDName(problemsList.get(i).getIProblem().getID());
        errorData[i][1] = editor.getSketch()
            .getCode(problemsList.get(i).getTabIndex()).getPrettyName();
        errorData[i][2] = problemsList.get(i).getLineNumber() + "";
        
        //TODO: This is temporary
        if(tempErrorLog.size() < 200)
        tempErrorLog.put(problemsList.get(i).getMessage(),problemsList.get(i).getIProblem());
      }
      
      if (errorWindow != null) {
        DefaultTableModel tm = new DefaultTableModel(errorData,
            XQErrorTable.columnNames);
        if (errorWindow.isVisible()) {
          errorWindow.updateTable(tm);
        }
        
        // Update error table in the editor
        editor.updateTable(tm);

        // A rotating slash animation on the title bar to show
        // that error checker thread is running

        slashAnimationIndex++;
        if (slashAnimationIndex == slashAnimation.length) {
          slashAnimationIndex = 0;
        }
        if (editor != null) {
          String info = slashAnimation[slashAnimationIndex] + " T:"
              + (System.currentTimeMillis() - lastTimeStamp)
              + "ms";
          errorWindow.setTitle("Problems - "
              + editor.getSketch().getName() + " " + info);
        }
      }

    } catch (Exception e) {
      log("Exception at updateErrorTable() " + e);
      e.printStackTrace();
      pauseThread();
    }

  }
  
  /**
   * Repaints the textarea if required
   */
  public void updatePaintedThingys() {    
    currentTab = editor.getSketch().getCodeIndex(
        editor.getSketch().getCurrentCode());
    //log("Tab changed " + currentTab + " LT " + lastTab);
    if (currentTab != lastTab) {
      textModified.set(5);
      lastTab = currentTab;
      editor.getTextArea().repaint();
      editor.statusEmpty();
      return;
    }

  }
  
  protected int lastCaretLine = -1;
  
  /**
   * Updates editor status bar, depending on whether the caret is on an error
   * line or not
   */
  public void updateEditorStatus() {
    
    if(editor.getStatusMode() == EditorStatus.EDIT) return;
    // editor.statusNotice("Position: " +
    // editor.getTextArea().getCaretLine());
    if(ExperimentalMode.errorCheckEnabled)
    synchronized (editor.errorBar.errorPoints) {
      for (ErrorMarker emarker : editor.errorBar.errorPoints) {
        if (emarker.getProblem().getLineNumber() == editor.getTextArea()
            .getCaretLine() + 1) {
          if (emarker.getType() == ErrorMarker.Warning) {
            editor.statusNotice(emarker.getProblem().getMessage()); 
                                //+  " : " + errorMsgSimplifier.getIDName(emarker.problem.getIProblem().getID()));
          //TODO: this is temporary
          }
          else {
            editor.statusError(emarker.getProblem().getMessage());
                               //+  " : " + errorMsgSimplifier.getIDName(emarker.problem.getIProblem().getID()));
          }
          return;
        }
      }
    }
    
    // This line isn't an error line anymore, so probably just clear it
    if (editor.getStatusMode() == EditorStatus.ERR
        || editor.getStatusMode() == EditorStatus.NOTICE) {
      editor.statusEmpty();
      return;
    }
//    if (editor.ta.getCaretLine() != lastCaretLine) {
//      editor.statusEmpty();
//      lastCaretLine = editor.ta.getCaretLine();
//    }
  }
  
  /**
   * Maps offset from java code to pde code. Returns a bunch of offsets as array
   * 
   * @param line
   *          - line number in java code
   * @param offset
   *          - offset from the start of the 'line'
   * @return int[0] - tab number, int[1] - line number in the int[0] tab, int[2]
   *         - line start offset, int[3] - offset from line start. int[2] and
   *         int[3] are on TODO
   */
  public int[] JavaToPdeOffsets(int line, int offset){
    int codeIndex = 0;

    int x = line - mainClassOffset;
    if (x < 0) {
      // log("Negative line number "
      // + problem.getSourceLineNumber() + " , offset "
      // + mainClassOffset);
      x = line - 2; // Another -1 for 0 index
      if (x < programImports.size() && x >= 0) {
        ImportStatement is = programImports.get(x);
        // log(is.importName + ", " + is.tab + ", "
        // + is.lineNumber);
        return new int[] { is.getTab(), is.getLineNumber() };
      } else {

        // Some seriously ugly stray error, just can't find the source
        // line! Simply return first line for first tab.
        return  new int[] { 0, 1 };
      }

    }

    try {
      for (SketchCode sc : editor.getSketch().getCode()) {
        if (sc.isExtension("pde")) {
          int len = 0;
          if (editor.getSketch().getCurrentCode().equals(sc)) {
            len = Base.countLines(sc.getDocument().getText(0,
                sc.getDocument().getLength())) + 1;
          } else {
            len = Base.countLines(sc.getProgram()) + 1;
          }

          // log("x,len, CI: " + x + "," + len + ","
          // + codeIndex);

          if (x >= len) {

            // We're in the last tab and the line count is greater
            // than the no.
            // of lines in the tab,
            if (codeIndex >= editor.getSketch().getCodeCount() - 1) {
              // log("Exceeds lc " + x + "," + len
              // + problem.toString());
              // x = len
              x = editor.getSketch().getCode(codeIndex)
                  .getLineCount();
              // TODO: Obtain line having last non-white space
              // character in the code.
              break;
            } else {
              x -= len;
              codeIndex++;
            }
          } else {

            if (codeIndex >= editor.getSketch().getCodeCount()) {
              codeIndex = editor.getSketch().getCodeCount() - 1;
            }
            break;
          }

        }
      }
    } catch (Exception e) {
      System.err
          .println("Things got messed up in ErrorCheckerService.JavaToPdeOffset()");
    }
    return new int[] { codeIndex, x };
  }
  
  public String getPDECodeAtLine(int tab, int linenumber){
    if(linenumber < 0) return null;
    editor.getSketch().setCurrentCode(tab);
    return editor.ta.getLineText(linenumber);
  }
  
  /**
   * Calculates the tab number and line number of the error in that particular
   * tab. Provides mapping between pure java and pde code.
   * 
   * @param problem
   *            - IProblem
   * @return int[0] - tab number, int[1] - line number
   */
  public int[] calculateTabIndexAndLineNumber(int javalineNumber) {
    // String[] lines = {};// = PApplet.split(sourceString, '\n');
    int codeIndex = 0;

    int x = javalineNumber - mainClassOffset;
    if (x < 0) {
      // log("Negative line number "
      // + problem.getSourceLineNumber() + " , offset "
      // + mainClassOffset);
      x = javalineNumber - 2; // Another -1 for 0 index
      if (x < programImports.size() && x >= 0) {
        ImportStatement is = programImports.get(x);
        // log(is.importName + ", " + is.tab + ", "
        // + is.lineNumber);
        return new int[] { is.getTab(), is.getLineNumber() };
      } else {

        // Some seriously ugly stray error, just can't find the source
        // line! Simply return first line for first tab.
        return new int[] { 0, 1 };
      }

    }

    try {
      for (SketchCode sc : editor.getSketch().getCode()) {
        if (sc.isExtension("pde")) {
          int len = 0;
          if (editor.getSketch().getCurrentCode().equals(sc)) {
            len = Base.countLines(sc.getDocument().getText(0,
                sc.getDocument().getLength())) + 1;
          } else {
            len = Base.countLines(sc.getProgram()) + 1;
          }

          // log("x,len, CI: " + x + "," + len + ","
          // + codeIndex);

          if (x >= len) {

            // We're in the last tab and the line count is greater
            // than the no.
            // of lines in the tab,
            if (codeIndex >= editor.getSketch().getCodeCount() - 1) {
              // log("Exceeds lc " + x + "," + len
              // + problem.toString());
              // x = len
              x = editor.getSketch().getCode(codeIndex)
                  .getLineCount();
              // TODO: Obtain line having last non-white space
              // character in the code.
              break;
            } else {
              x -= len;
              codeIndex++;
            }
          } else {

            if (codeIndex >= editor.getSketch().getCodeCount()) {
              codeIndex = editor.getSketch().getCodeCount() - 1;
            }
            break;
          }

        }
      }
    } catch (Exception e) {
      System.err
          .println("Things got messed up in ErrorCheckerService.calculateTabIndexAndLineNumber()");
    }

    return new int[] { codeIndex, x };
  }

  /**
   * Fetches code from the editor tabs and pre-processes it into parsable pure
   * java source. And there's a difference between parsable and compilable.
   * XQPrerocessor.java makes this code compilable. <br>
   * Handles: <li>Removal of import statements <li>Conversion of int(),
   * char(), etc to PApplet.parseInt(), etc. <li>Replacing '#' with 0xff for
   * color representation<li>Converts all 'color' datatypes to int
   * (experimental) <li>Appends class declaration statement after determining
   * the mode the sketch is in - ACTIVE or STATIC
   * 
   * @return String - Pure java representation of PDE code. Note that this
   *         code is not yet compile ready.
   */ 

  protected String preprocessCode(String pdeCode) {
    
    programImports = new ArrayList<ImportStatement>();
    
    StringBuffer rawCode = new StringBuffer();
    
    try {

      for (SketchCode sc : editor.getSketch().getCode()) {
        if (sc.isExtension("pde")) {

          try {

            if (editor.getSketch().getCurrentCode().equals(sc)) {

              rawCode.append(scrapImportStatements(sc.getDocument()
                                                   .getText(0,
                                                            sc.getDocument()
                                                            .getLength()),
                                                            editor.getSketch()
                                                            .getCodeIndex(sc)));
            } else {

              rawCode.append(scrapImportStatements(sc.getProgram(), editor
                                                   .getSketch().getCodeIndex(sc)));

            }
            rawCode.append('\n');
          } catch (Exception e) {
            System.err.println("Exception in preprocessCode() - bigCode "
              + e.toString());
          }
          rawCode.append('\n');
        }
      }

    } catch (Exception e) {
      log("Exception in preprocessCode()");
    }
    String sourceAlt = rawCode.toString();
    // Replace comments with whitespaces
    // sourceAlt = scrubComments(sourceAlt);

    // Find all int(*), replace with PApplet.parseInt(*)

    // \bint\s*\(\s*\b , i.e all exclusive "int("

    String dataTypeFunc[] = { "int", "char", "float", "boolean", "byte" };

    for (String dataType : dataTypeFunc) {
      String dataTypeRegexp = "\\b" + dataType + "\\s*\\(";
      Pattern pattern = Pattern.compile(dataTypeRegexp);
      Matcher matcher = pattern.matcher(sourceAlt);

      // while (matcher.find()) {
      // System.out.print("Start index: " + matcher.start());
      // log(" End index: " + matcher.end() + " ");
      // log("-->" + matcher.group() + "<--");
      // }
      sourceAlt = matcher.replaceAll("PApplet.parse"
        + Character.toUpperCase(dataType.charAt(0))
        + dataType.substring(1) + "(");

    }

    // Find all #[web color] and replace with 0xff[webcolor]
    // Should be 6 digits only.
    final String webColorRegexp = "#{1}[A-F|a-f|0-9]{6}\\W";
    Pattern webPattern = Pattern.compile(webColorRegexp);
    Matcher webMatcher = webPattern.matcher(sourceAlt);
    while (webMatcher.find()) {
      // log("Found at: " + webMatcher.start());
      String found = sourceAlt.substring(webMatcher.start(),
                                         webMatcher.end());
      // log("-> " + found);
      sourceAlt = webMatcher.replaceFirst("0xff" + found.substring(1));
      webMatcher = webPattern.matcher(sourceAlt);
    }

    // Replace all color data types with int
    // Regex, Y U SO powerful?
    final String colorTypeRegex = "color(?![a-zA-Z0-9_])(?=\\[*)(?!(\\s*\\())";
    Pattern colorPattern = Pattern.compile(colorTypeRegex);
    Matcher colorMatcher = colorPattern.matcher(sourceAlt);
    sourceAlt = colorMatcher.replaceAll("int");

    checkForChangedImports();

    className = (editor == null) ? "DefaultClass" : editor.getSketch()
      .getName();

    
    // Check whether the code is being written in STATIC mode(no function
    // declarations) - append class declaration and void setup() declaration
    Matcher matcher = FUNCTION_DECL.matcher(sourceAlt);
    if (!matcher.find()) {
      sourceAlt = xqpreproc.prepareImports(programImports) + "public class " + className + " extends PApplet {\n"
        + "public void setup() {\n" + sourceAlt
        + "\nnoLoop();\n}\n" + "\n}\n";
      staticMode = true;           

    } else {
      sourceAlt = xqpreproc.prepareImports(programImports) + "public class " + className + " extends PApplet {\n"
        + sourceAlt + "\n}";
      staticMode = false;     
    }
    
    int position = sourceAlt.indexOf("{") + 1;
    mainClassOffset = 1;
    for (int i = 0; i <= position; i++) {
      if (sourceAlt.charAt(i) == '\n') {
        mainClassOffset++;
      }
    }
    if(staticMode) {
      mainClassOffset++;
    }
    //mainClassOffset += 2;
    // Handle unicode characters
    sourceAlt = substituteUnicode(sourceAlt);

//     log("-->\n" + sourceAlt + "\n<--");
//     log("PDE code processed - "
//     + editor.getSketch().getName());
    sourceCode = sourceAlt;
    return sourceAlt;  

  }
  
  /**
   * The super method that highlights any ASTNode in the pde editor =D
   * @param node
   * @return true - if highlighting happened correctly.
   */
  public boolean highlightNode(ASTNodeWrapper awrap){
    log("Highlighting: " + awrap);
    try {
      int pdeoffsets[] = awrap.getPDECodeOffsets(this);
      int javaoffsets[] = awrap.getJavaCodeOffsets(this);
      log("offsets: " +pdeoffsets[0] + "," +
          pdeoffsets[1]+ "," +javaoffsets[1]+ "," +
          javaoffsets[2]);
      scrollToErrorLine(editor, pdeoffsets[0],
                                            pdeoffsets[1],javaoffsets[1],
                                            javaoffsets[2]);
      return true;
    } catch (Exception e) {
      logE("Scrolling failed for " + awrap);
      // e.printStackTrace();
    }
    return false;
  }
  
  public boolean highlightNode(ASTNode node){
    ASTNodeWrapper awrap = new ASTNodeWrapper(node);
    return highlightNode(awrap);
  }
  
  /**
   * Scrolls to the error source in code. And selects the line text. Used by
   * XQErrorTable and ErrorBar
   * 
   * @param errorIndex
   *            - index of error
   */
  public void scrollToErrorLine(int errorIndex) {
    if (editor == null) {
      return;
    }
    
    if (errorIndex < problemsList.size() && errorIndex >= 0) {
      Problem p = problemsList.get(errorIndex);
      scrollToErrorLine(p);
    }
  }
  
  public void scrollToErrorLine(Problem p) {
    if (editor == null) {
      return;
    }
    if (p == null)
      return;
    try {
      editor.toFront();
      editor.getSketch().setCurrentCode(p.getTabIndex());

      editor
          .setSelection(editor.getTextArea()
                            .getLineStartNonWhiteSpaceOffset(p.getLineNumber() - 1)
                            + editor.getTextArea()
                                .getLineText(p.getLineNumber() - 1).trim().length(),
                        editor.getTextArea()
                            .getLineStartNonWhiteSpaceOffset(p.getLineNumber() - 1));
      editor.getTextArea().scrollTo(p.getLineNumber() - 1, 0);
      editor.repaint();
    } catch (Exception e) {
      System.err.println(e
          + " : Error while selecting text in scrollToErrorLine()");
      e.printStackTrace();
    }
    // log("---");
  }
  
  /**
   * Static method for scroll to a particular line in the PDE. Also highlights
   * the length of the text. Requires the editor instance as arguement.
   * 
   * @param edt
   * @param tabIndex
   * @param lineNoInTab
   *          - line number in the corresponding tab
   * @param lineStartOffset
   *          - selection start offset(from line start non-whitespace offset)
   * @param length
   *          - length of selection
   * @return - true, if scroll was successful
   */
  public static boolean scrollToErrorLine(Editor edt, int tabIndex, int lineNoInTab, int lineStartOffset, int length) {
    if (edt == null) {
      return false;
    }
    try {
      edt.toFront();
      edt.getSketch().setCurrentCode(tabIndex);
      int lsno = edt.getTextArea()
          .getLineStartNonWhiteSpaceOffset(lineNoInTab - 1) + lineStartOffset;
      edt.setSelection(lsno, lsno + length);
      edt.getTextArea().scrollTo(lineNoInTab - 1, 0);
      edt.repaint();
      log(lineStartOffset + " LSO,len " + length);
    } catch (Exception e) {
      System.err.println(e
          + " : Error while selecting text in static scrollToErrorLine()");
      e.printStackTrace();
      return false;
    }
    return true;
  }
  
  /**
   * Checks if import statements in the sketch have changed. If they have,
   * compiler classpath needs to be updated.
   */
  protected void checkForChangedImports() {
//     log("Imports: " + programImports.size() +
//     " Prev Imp: "
//     + previousImports.size());
    if (programImports.size() != previousImports.size()) {
      // log(1);
      loadCompClass = true;
      previousImports = programImports;
    } else {
      for (int i = 0; i < programImports.size(); i++) {
        if (!programImports.get(i).getImportName().equals(previousImports
            .get(i).getImportName())) {
          // log(2);
          loadCompClass = true;
          previousImports = programImports;
          break;
        }
      }
    }
    // log("load..? " + loadCompClass);
  }

  protected int pdeImportsCount;
  
  public int getPdeImportsCount() {
    return pdeImportsCount;
  }

  /**
   * Removes import statements from tabSource, replaces each with white spaces
   * and adds the import to the list of program imports
   * 
   * @param tabProgram
   *            - Code in a tab
   * @param tabNumber
   *            - index of the tab
   * @return String - Tab code with imports replaced with white spaces
   */
  protected String scrapImportStatements(String tabProgram, int tabNumber) {
    //TODO: Commented out imports are still detected as main imports.
    pdeImportsCount = 0;
    String tabSource = new String(tabProgram);
    do {
      // log("-->\n" + sourceAlt + "\n<--");
      String[] pieces = PApplet.match(tabSource, importRegexp);

      // Stop the loop if we've removed all the import lines
      if (pieces == null) {
        break;
      }

      String piece = pieces[1] + pieces[2] + pieces[3];
      int len = piece.length(); // how much to trim out

      // programImports.add(piece); // the package name

      // find index of this import in the program
      int idx = tabSource.indexOf(piece);
      // System.out.print("Import -> " + piece);
      // log(" - "
      // + Base.countLines(tabSource.substring(0, idx)) + " tab "
      // + tabNumber);
      programImports.add(new ImportStatement(piece, tabNumber, Base
        .countLines(tabSource.substring(0, idx))));
      // Remove the import from the main program
      // Substitute with white spaces
      String whiteSpace = "";
      for (int j = 0; j < piece.length(); j++) {
        whiteSpace += " ";
      }
      tabSource = tabSource.substring(0, idx) + whiteSpace
        + tabSource.substring(idx + len);
      pdeImportsCount++;
    } while (true);
    // log(tabSource);
    return tabSource;
  }

  /**
   * Replaces non-ascii characters with their unicode escape sequences and
   * stuff. Used as it is from
   * processing.src.processing.mode.java.preproc.PdePreprocessor
   * 
   * @param program
   *            - Input String containing non ascii characters
   * @return String - Converted String
   */
  public static String substituteUnicode(String program) {
    // check for non-ascii chars (these will be/must be in unicode format)
    char p[] = program.toCharArray();
    int unicodeCount = 0;
    for (int i = 0; i < p.length; i++) {
      if (p[i] > 127) {
        unicodeCount++;
      }
    }
    if (unicodeCount == 0) {
      return program;
    }
    // if non-ascii chars are in there, convert to unicode escapes
    // add unicodeCount * 5.. replacing each unicode char
    // with six digit uXXXX sequence (xxxx is in hex)
    // (except for nbsp chars which will be a replaced with a space)
    int index = 0;
    char p2[] = new char[p.length + unicodeCount * 5];
    for (int i = 0; i < p.length; i++) {
      if (p[i] < 128) {
        p2[index++] = p[i];
      } else if (p[i] == 160) { // unicode for non-breaking space
        p2[index++] = ' ';
      } else {
        int c = p[i];
        p2[index++] = '\\';
        p2[index++] = 'u';
        char str[] = Integer.toHexString(c).toCharArray();
        // add leading zeros, so that the length is 4
        // for (int i = 0; i < 4 - str.length; i++) p2[index++] = '0';
        for (int m = 0; m < 4 - str.length; m++)
          p2[index++] = '0';
        System.arraycopy(str, 0, p2, index, str.length);
        index += str.length;
      }
    }
    return new String(p2, 0, index);
  }
  
  public void handleErrorCheckingToggle(){
    if (!ExperimentalMode.errorCheckEnabled) {
      // unticked Menu Item
      // pauseThread();
      log(editor.getSketch().getName()
          + " - Error Checker paused.");
      editor.errorBar.errorPoints.clear();
      problemsList.clear();
      updateErrorTable();
      updateEditorStatus();
      editor.getTextArea().repaint();
      editor.errorBar.repaint();
    } else {
      //resumeThread();
      log(editor.getSketch().getName()
          + " - Error Checker resumed.");
      runManualErrorCheck();
    }
  }
  
  /**
   * Stops the Error Checker Service thread
   */
  public void stopThread() {
    logE("Stopping thread: " + editor.getSketch().getName());
    stopThread.set(true);
  }

  /**
   * Pauses the Error Checker Service thread
   */
  public void pauseThread() {
    pauseThread.set(true);
  }

  /**
   * Resumes the Error Checker Service thread
   */
  public void resumeThread() {
    pauseThread.set(false);
  }

  public DebugEditor getEditor() {
    return editor;
  }
  
//  public static void log(String message){
//    if(ExperimentalMode.DEBUG)
//      log(message);
//  }
//  
//  public static void log2(String message){
//    if(ExperimentalMode.DEBUG)
//      System.out.print(message);
//  }

  public ArrayList<ImportStatement> getProgramImports() {
    return programImports;
  }
}
