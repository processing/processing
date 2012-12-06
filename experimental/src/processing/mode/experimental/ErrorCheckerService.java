package processing.mode.experimental;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.table.DefaultTableModel;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;

import processing.app.Base;
import processing.app.Library;
import processing.app.SketchCode;
import processing.core.PApplet;

public class ErrorCheckerService implements Runnable{
  
  private DebugEditor editor;
  /**
   * Error check happens every sleepTime milliseconds
   */
  public static final int sleepTime = 1000;

  /**
   * The amazing eclipse ast parser
   */
  private ASTParser parser;
 
  /**
   * Used to indirectly stop the Error Checker Thread
   */
  public boolean stopThread = false;

  /**
   * If true, Error Checking is paused. Calls to checkCode() become useless.
   */
  private boolean pauseThread = false;

  protected ErrorWindow errorWindow;
//  protected ErrorBar errorBar;
  /**
   * IProblem[] returned by parser stored in here
   */
  private IProblem[] problems;

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
   * P5 Preproc offset
   */
  private int scPreProcOffset = 0;

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
  private boolean loadCompClass = true;

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
  private long lastTimeStamp = System.currentTimeMillis();

  /**
   * Used for displaying the rotating slash on the Problem Window title bar
   */
  private String[] slashAnimation = { "|", "/", "--", "\\", "|", "/", "--",
      "\\" };
  private int slashAnimationIndex = 0;

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
  
  public ErrorCheckerService(DebugEditor debugEditor) {
    this.editor = debugEditor;
    initParser();
    initializeErrorWindow();
    xqpreproc = new XQPreprocessor();
  }
  
  /**
   * Initializes ASTParser
   */
  private void initParser() {
    try {
      parser = ASTParser.newParser(AST.JLS4);
    } catch (Exception e) {
      System.err.println("XQMode initialization failed. "
          + "Are you running the right version of Processing? ");
      pauseThread();
    } catch (Error e) {
      System.err.println("XQMode initialization failed. ");
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
    stopThread = false;

    while (!stopThread) {
      try {
        // Take a nap.
        Thread.sleep(sleepTime);
      } catch (Exception e) {
        System.out.println("Oops! [ErrorCheckerThreaded]: " + e);
        // e.printStackTrace();
      }

      if (pauseThread)
        continue;

      // Check every x seconds
      checkCode();
     
    }
  }
  
  private boolean checkCode() {
    
    lastTimeStamp = System.currentTimeMillis();
    try {
      sourceCode = preprocessCode(editor.getSketch().getMainProgram());
      
      syntaxCheck();
      
      // No syntax errors, proceed for compilation check, Stage 2.
      if (problems.length == 0 && editor.compilationCheckEnabled) { //TODO: && editor.compilationCheckEnabled condition
        sourceCode = xqpreproc.doYourThing(sourceCode, programImports);
        prepareCompilerClasspath();
        mainClassOffset = xqpreproc.mainClassOffset; // tiny, but
                                // significant
        if (staticMode)
          mainClassOffset++; // Extra line for setup() decl.
//         System.out.println(sourceCode);
//         System.out.println("--------------------------");
        compileCheck();
      }
      
      
      updateErrorTable();
      editor.updateErrorBar(problemsList);
      updateEditorStatus();
      updateTextAreaPainter();
      return true;

    } catch (Exception e) {
      System.out.println("Oops! [ErrorCheckerService.checkCode]: " + e);
      e.printStackTrace();
    }
    return false;
  }

  private void syntaxCheck() {
    parser.setSource(sourceCode.toCharArray());
    parser.setKind(ASTParser.K_COMPILATION_UNIT);

    @SuppressWarnings("unchecked")
    Map<String, String> options = JavaCore.getOptions();

    JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
    options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
    parser.setCompilerOptions(options);
    cu = (CompilationUnit) parser.createAST(null);

    // Store errors returned by the ast parser
    problems = cu.getProblems();
    // System.out.println("Problem Count: " + problems.length);
    // Populate the probList
    problemsList = new ArrayList<Problem>();
    for (int i = 0; i < problems.length; i++) {
      int a[] = calculateTabIndexAndLineNumber(problems[i]);
      Problem p = new Problem(problems[i], a[0], a[1]);
      problemsList.add(p);
//      System.out.println(p.toString());
    }
  }
  
  private void compileCheck() {

    // Currently (Sept, 2012) I'm using Java's reflection api to load the
    // CompilationChecker class(from CompilationChecker.jar) that houses the
    // Eclispe JDT compiler and call its getErrorsAsObj method to obtain
    // errors. This way, I'm able to add the paths of contributed libraries
    // to the classpath of CompilationChecker, dynamically.

    try {

      // NOTE TO SELF: If classpath contains null Strings
      // URLClassLoader gets angry. Drops NPE bombs.

      // If imports have changed, reload classes with new classpath.
      if (loadCompClass) {

        // if (classpathJars.size() > 0)
        // System.out
        // .println("XQMode: Loading contributed libraries referenced by import statements.");
        
        File f = new File("modes"
            + File.separator
            + "java2"
            + File.separator + "mode");
        
        FileFilter fileFilter = new FileFilter() {
          public boolean accept(File file) {
            return (file.getName().endsWith(".jar") && !file
                .getName().startsWith("XQMode"));
          }
        };

        File[] jarFiles = f.listFiles(fileFilter);
        for (File jarFile : jarFiles) {
          classpathJars.add(jarFile.toURI().toURL());
        }

        classpath = new URL[classpathJars.size()]; // + 1 for
                              // Compilation
                              // Checker class
        for (int i = 0; i < classpathJars.size(); i++) {
          classpath[i] = classpathJars.get(i);
        }

        // System.out.println("CP Len -- " + classpath.length);
        URLClassLoader classLoader = new URLClassLoader(classpath);
        // System.out.println("1.");
        checkerClass = Class.forName("CompilationChecker", true,
            classLoader);
        // System.out.println("2.");
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

        int a[] = calculateTabIndexAndLineNumber(problem);
        Problem p = new Problem(problem, a[0], a[1]);
        if ((Boolean) errorList[i][8]) {
          p.setType(Problem.ERROR);
        }
        
        if ((Boolean) errorList[i][9]) {
          p.setType(Problem.WARNING);
        }

        // If warnings are disabled, skip 'em
        if (p.isWarning() && !warningsEnabled) {
          continue;
        }
        problemsList.add(p);
      }

    } catch (ClassNotFoundException e) {
      System.err.println("Compiltation Checker files couldn't be found! "
          + e + " compileCheck() problem.");
      stopThread();
    } catch (MalformedURLException e) {
      System.err.println("Compiltation Checker files couldn't be found! "
          + e + " compileCheck() problem.");
      stopThread();
    } catch (Exception e) {
      System.err.println("compileCheck() problem." + e);
      e.printStackTrace();
      stopThread();
    } catch (NoClassDefFoundError e) {
      System.err
          .println(e
              + " compileCheck() problem. Somebody tried to mess with XQMode files.");
      stopThread();
    }
    // System.out.println("Compilecheck, Done.");
  }
  
  /**
   * Processes import statements to obtain classpaths of contributed
   * libraries. This would be needed for compilation check. Also, adds
   * stuff(jar files, class files, candy) from the code folder. And it looks
   * messed up.
   * 
   */
  private void prepareCompilerClasspath() {
    if (!loadCompClass)
      return;
    // System.out.println("1..");
    classpathJars = new ArrayList<URL>();
    String entry = "";
    boolean codeFolderChecked = false;
    for (ImportStatement impstat : programImports) {
      String item = impstat.importName;
      int dot = item.lastIndexOf('.');
      entry = (dot == -1) ? item : item.substring(0, dot);

      entry = entry.substring(6).trim();
      // System.out.println("Entry--" + entry);
      if (ignorableImport(entry)) {
        // System.out.println("Ignoring: " + entry);
        continue;
      }
      Library library = null;

      // Try to get the library classpath and add it to the list
      try {
        library = editor.getMode().getLibrary(entry);
        // System.out.println("lib->" + library.getClassPath() + "<-");
        String libraryPath[] = PApplet.split(library.getClassPath()
            .substring(1).trim(), File.pathSeparatorChar);
        for (int i = 0; i < libraryPath.length; i++) {
          // System.out.println(entry + " ::"
          // + new File(libraryPath[i]).toURI().toURL());
          classpathJars.add(new File(libraryPath[i]).toURI().toURL());
        }
        // System.out.println("-- ");
        // classpath[count] = (new File(library.getClassPath()
        // .substring(1))).toURI().toURL();
        // System.out.println("  found ");
        // System.out.println(library.getClassPath().substring(1));
      } catch (Exception e) {
        if (library == null && !codeFolderChecked) {
          // System.out.println(1);
          // Look around in the code folder for jar files
          if (editor.getSketch().hasCodeFolder()) {
            File codeFolder = editor.getSketch().getCodeFolder();

            // get a list of .jar files in the "code" folder
            // (class files in subfolders should also be picked up)
            String codeFolderClassPath = Base
                .contentsToClassPath(codeFolder);
            codeFolderChecked = true;
            if (codeFolderClassPath.equalsIgnoreCase("")) {
              System.err.println("XQMODE: Yikes! Can't find \""
                  + entry
                  + "\" library! Line: "
                  + impstat.lineNumber
                  + " in tab: "
                  + editor.getSketch().getCode(impstat.tab)
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
            System.err.println("XQMODE: Yikes! Can't find \""
                + entry
                + "\" library! Line: "
                + impstat.lineNumber
                + " in tab: "
                + editor.getSketch().getCode(impstat.tab)
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
   * Enable/Disable warnings from being shown
   */
  public boolean warningsEnabled = true;

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
  synchronized public void updateErrorTable() {

    try {
      String[][] errorData = new String[problemsList.size()][3];
      for (int i = 0; i < problemsList.size(); i++) {
        errorData[i][0] = problemsList.get(i).message;
        errorData[i][1] = editor.getSketch()
            .getCode(problemsList.get(i).tabIndex).getPrettyName();
        errorData[i][2] = problemsList.get(i).lineNumber + "";
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
      System.out.println("Exception at updateErrorTable() " + e);
      e.printStackTrace();
      stopThread();
    }

  }
  
  /**
   * Repaints the textarea if required
   */
  public void updateTextAreaPainter() {
    editor.getTextArea().repaint();
    currentTab = editor.getSketch().getCodeIndex(
        editor.getSketch().getCurrentCode());
    if (currentTab != lastTab) {
      lastTab = currentTab;
      // editor.getTextArea().repaint();
      // System.out.println("1 Repaint " + System.currentTimeMillis());
      return;
    }

//    TODO: if (errorBar.errorPointsChanged())
//      editor.getTextArea().repaint();

  }
  
  /**
   * Updates editor status bar, depending on whether the caret is on an error
   * line or not
   */
  public void updateEditorStatus() {
    // editor.statusNotice("Position: " +
    // editor.getTextArea().getCaretLine());
    boolean notFound = true;
    for (ErrorMarker emarker : editor.errorBar.errorPoints) {
      if (emarker.problem.lineNumber == editor.getTextArea()
          .getCaretLine() + 1) {
        if (emarker.type == ErrorMarker.Warning) {
          editor.statusNotice(emarker.problem.message);
        }
        else {
          editor.statusError(emarker.problem.message);
        }
        return;
      }
    }
    if (notFound) {
      editor.statusEmpty();
    }
  }
  
  /**
   * Calculates the tab number and line number of the error in that particular
   * tab. Provides mapping between pure java and pde code.
   * 
   * @param problem
   *            - IProblem
   * @return int[0] - tab number, int[1] - line number
   */
  public int[] calculateTabIndexAndLineNumber(IProblem problem) {
    // String[] lines = {};// = PApplet.split(sourceString, '\n');
    int codeIndex = 0;
    int bigCount = 0;

    int x = problem.getSourceLineNumber() - mainClassOffset;
    if (x < 0) {
      // System.out.println("Negative line number "
      // + problem.getSourceLineNumber() + " , offset "
      // + mainClassOffset);
      x = problem.getSourceLineNumber() - 2; // Another -1 for 0 index
      if (x < programImports.size() && x >= 0) {
        ImportStatement is = programImports.get(x);
        // System.out.println(is.importName + ", " + is.tab + ", "
        // + is.lineNumber);
        return new int[] { is.tab, is.lineNumber };
      } else {

        // Some seriously ugly stray error, just can't find the source
        // line! Simply return first line for first tab.
        return new int[] { 0, 1 };
      }

    }

    try {
      for (SketchCode sc : editor.getSketch().getCode()) {
        if (sc.isExtension("pde")) {
          sc.setPreprocOffset(bigCount);
          int len = 0;
          if (editor.getSketch().getCurrentCode().equals(sc)) {
            len = Base.countLines(sc.getDocument().getText(0,
                sc.getDocument().getLength())) + 1;
          } else {
            len = Base.countLines(sc.getProgram()) + 1;
          }

          // System.out.println("x,len, CI: " + x + "," + len + ","
          // + codeIndex);

          if (x >= len) {

            // We're in the last tab and the line count is greater
            // than the no.
            // of lines in the tab,
            if (codeIndex >= editor.getSketch().getCodeCount() - 1) {
              // System.out.println("Exceeds lc " + x + "," + len
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
        bigCount += sc.getLineCount();
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
   * char(), etc to (int)(), (char)(), etc. <li>Replacing '#' with 0xff for
   * color representation<li>Converts all 'color' datatypes to int
   * (experimental) <li>Appends class declaration statement after determining
   * the mode the sketch is in - ACTIVE or STATIC
   * 
   * @return String - Pure java representation of PDE code. Note that this
   *         code is not yet compile ready.
   */ 

  private String preprocessCode(String pdeCode) {
    
    programImports = new ArrayList<ImportStatement>();
    
    StringBuffer rawCode = new StringBuffer();
    
    try {

      for (SketchCode sc : editor.getSketch().getCode()) {
        if (sc.isExtension("pde")) {

          sc.setPreprocOffset(scPreProcOffset);

          try {

            if (editor.getSketch().getCurrentCode().equals(sc)) {

              // rawCode.append(sc.getDocument().getText(0,
              // sc.getDocument().getLength()));
              rawCode.append(scrapImportStatements(sc.getDocument()
                                                   .getText(0,
                                                            sc.getDocument()
                                                            .getLength()),
                                                            editor.getSketch()
                                                            .getCodeIndex(sc)));
            } else {

              // rawCode.append(sc.getProgram());
              rawCode.append(scrapImportStatements(sc.getProgram(), editor
                                                   .getSketch().getCodeIndex(sc)));

            }
            rawCode.append('\n');
          } catch (Exception e) {
            System.err.println("Exception in preprocessCode() - bigCode "
              + e.toString());
          }
          rawCode.append('\n');
          scPreProcOffset += sc.getLineCount();
        }
      }

    } catch (Exception e) {
      System.out.println("Exception in preprocessCode()");
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
      // System.out.println(" End index: " + matcher.end() + " ");
      // System.out.println("-->" + matcher.group() + "<--");
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
      // System.out.println("Found at: " + webMatcher.start());
      String found = sourceAlt.substring(webMatcher.start(),
                                         webMatcher.end());
      // System.out.println("-> " + found);
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
      sourceAlt = "public class " + className + " extends PApplet {\n"
        + "public void setup() {\n" + sourceAlt
        + "\nnoLoop();\n}\n" + "\n}\n";
      staticMode = true;
      mainClassOffset = 2;

    } else {
      sourceAlt = "public class " + className + " extends PApplet {\n"
        + sourceAlt + "\n}";
      staticMode = false;
      mainClassOffset = 1;
    }

    // Handle unicode characters
    sourceAlt = substituteUnicode(sourceAlt);

//     System.out.println("-->\n" + sourceAlt + "\n<--");
//     System.out.println("PDE code processed - "
//     + editor.getSketch().getName());
    sourceCode = sourceAlt;
    return sourceAlt;  

  }
  
  /**
   * Scrolls to the error source in code. And selects the line text. Used by
   * XQErrorTable and ErrorBar
   * 
   * @param errorIndex
   *            - index of error
   */
  public void scrollToErrorLine(int errorIndex) {
    if (editor == null)
      return;
    if (errorIndex < problemsList.size() && errorIndex >= 0) {
      Problem p = problemsList.get(errorIndex);
      try {
        editor.toFront();
        editor.getSketch().setCurrentCode(p.tabIndex);
        editor.getTextArea().scrollTo(p.lineNumber - 1, 0);
        editor.setSelection(editor.getTextArea()
            .getLineStartNonWhiteSpaceOffset(p.lineNumber - 1)
            + editor.getTextArea().getLineText(p.lineNumber - 1)
                .trim().length(), editor.getTextArea()
            .getLineStartNonWhiteSpaceOffset(p.lineNumber - 1));
        editor.repaint();
      } catch (Exception e) {
        System.err
            .println(e
                + " : Error while selecting text in scrollToErrorLine()");
        // e.printStackTrace();
      }
      // System.out.println("---");

    }
  }
  
  /**
   * Checks if import statements in the sketch have changed. If they have,
   * compiler classpath needs to be updated.
   */
  private void checkForChangedImports() {
    // System.out.println("Imports: " + programImports.size() +
    // " Prev Imp: "
    // + previousImports.size());
    if (programImports.size() != previousImports.size()) {
      // System.out.println(1);
      loadCompClass = true;
      previousImports = programImports;
    } else {
      for (int i = 0; i < programImports.size(); i++) {
        if (!programImports.get(i).importName.equals(previousImports
            .get(i).importName)) {
          // System.out.println(2);
          loadCompClass = true;
          previousImports = programImports;
          break;
        }
      }
    }
    // System.out.println("load..? " + loadCompClass);
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
  private String scrapImportStatements(String tabProgram, int tabNumber) {

    String tabSource = new String(tabProgram);
    do {
      // System.out.println("-->\n" + sourceAlt + "\n<--");
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
      // System.out.println(" - "
      // + Base.countLines(tabSource.substring(0, idx)) + " tab "
      // + tabNumber);
      programImports.add(new ImportStatement(piece, tabNumber, Base
        .countLines(tabSource.substring(0, idx))));
      // Remove the import from the main program
      // Substitue with white spaces
      String whiteSpace = "";
      for (int j = 0; j < piece.length(); j++) {
        whiteSpace += " ";
      }
      tabSource = tabSource.substring(0, idx) + whiteSpace
        + tabSource.substring(idx + len);

    } while (true);
    // System.out.println(tabSource);
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
  
  /**
   * Stops the Error Checker Service thread
   */
  public void stopThread() {
    stopThread = true;
//    System.out.println(editor.getSketch().getName()
//        + " - Error Checker stopped.");
  }

  /**
   * Pauses the Error Checker Service thread
   */
  public void pauseThread() {
    pauseThread = true;
  }

  /**
   * Resumes the Error Checker Service thread
   */
  public void resumeThread() {
    pauseThread = false;
  }

  public DebugEditor getEditor() {
    return editor;
  }
}
