/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-15 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java.pdex;

import com.google.classpath.ClassPath;
import com.google.classpath.ClassPathFactory;

import java.awt.EventQueue;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

import processing.app.Library;
import processing.app.Messages;
import processing.app.Preferences;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.SketchException;
import processing.app.Util;
import processing.app.ui.Editor;
import processing.app.ui.EditorStatus;
import processing.app.ui.ErrorTable;
import processing.core.PApplet;
import processing.data.IntList;
import processing.data.StringList;
import processing.mode.java.JavaMode;
import processing.mode.java.JavaEditor;
import processing.mode.java.preproc.PdePreprocessor;
import processing.mode.java.preproc.PdePreprocessor.Mode;

import static processing.app.Util.countLines;


/**
 * The main error checking service
 */
@SuppressWarnings("unchecked")
public class ErrorCheckerService {

  protected final JavaEditor editor;

  /** The amazing eclipse ast parser */
  protected final ASTParser parser = ASTParser.newParser(AST.JLS8);

  /** Class path factory for ASTGenerator */
  protected final ClassPathFactory classPathFactory = new ClassPathFactory();

  /**
   * Used to indirectly stop the Error Checker Thread
   */
  private volatile boolean running;

  /**
   * How many lines are present till the initial class declaration? In static
   * mode, this would include imports, class declaration and setup
   * declaration. In nomral mode, this would include imports, class
   * declaration only. It's fate is decided inside preprocessCode()
   */
  public int mainClassOffset;

  /**
   * ASTGenerator for operations on AST
   */
  protected final ASTGenerator astGenerator;

  /**
   * Regexp for import statements. (Used from Processing source)
   */
  // TODO: merge this with SourceUtils one
  public static final String IMPORT_REGEX =
    "(?:^|;)\\s*(import\\s+)((?:static\\s+)?\\S+)(\\s*;)";


  public ErrorCheckerService(JavaEditor editor) {
    this.editor = editor;
    astGenerator = new ASTGenerator(editor, this);
  }


  /**
   * Error checking doesn't happen before this interval has ellapsed since the
   * last request() call.
   */
  private final static long errorCheckInterval = 650;

  protected volatile PreprocessedSketch latestResult = new PreprocessedSketch();

  private Thread errorCheckerThread;
  private final BlockingQueue<Boolean> requestQueue = new ArrayBlockingQueue<>(1);
  private ScheduledExecutorService scheduler;
  private volatile ScheduledFuture<?> scheduledUiUpdate = null;
  private volatile long nextUiUpdate = 0;

  private final Runnable mainLoop = new Runnable() {
    @Override
    public void run() {
      running = true;

      latestResult = checkCode();

      if (!latestResult.hasSyntaxErrors && !latestResult.hasCompilationErrors) {
//      editor.showProblemListView(Language.text("editor.footer.console"));
        editor.showConsole();
      }
      // Make sure astGen has at least one CU to start with
      // This is when the loaded sketch already has syntax errors.
      // Completion wouldn't be complete, but it'd be still something
      // better than nothing
      {
        final DefaultMutableTreeNode tree =
            ASTGenerator.buildTree(latestResult.compilationUnit);
        EventQueue.invokeLater(new Runnable() {
          @Override
          public void run() {
            astGenerator.updateAST(latestResult.compilationUnit, tree);
          }
        });
      }

      while (running) {
        try {
          requestQueue.take(); // blocking until there is more work
        } catch (InterruptedException e) {
          break;
        }
        requestQueue.clear();

        try {

          Messages.log("Starting error check");
          PreprocessedSketch result = checkCode();

          if (!JavaMode.errorCheckEnabled) {
            latestResult.problems.clear();
            Messages.log("Error Check disabled, so not updating UI.");
          }

          latestResult = result;

          final DefaultMutableTreeNode tree =
              ASTGenerator.buildTree(latestResult.compilationUnit);

          checkForMissingImports(latestResult);

          if (JavaMode.errorCheckEnabled) {
            if (scheduledUiUpdate != null) {
              scheduledUiUpdate.cancel(true);
            }
            // Update UI after a delay. See #2677
            long delay = nextUiUpdate - System.currentTimeMillis();
            Runnable uiUpdater = new Runnable() {
              final PreprocessedSketch result = latestResult;

              @Override
              public void run() {
                if (nextUiUpdate > 0 &&
                    System.currentTimeMillis() >= nextUiUpdate) {
                  EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                      synchronized (astGenerator) {
                        astGenerator.updateAST(latestResult.compilationUnit, tree);
                        astGenerator.classPath = result.classPath;
                      }
                      updateErrorTable(result.problems);
                      editor.updateErrorBar(result.problems);
                      editor.getTextArea().repaint();
                      editor.updateErrorToggle(result.hasSyntaxErrors || result.hasCompilationErrors);
                    }
                  });
                }
              }
            };
            scheduledUiUpdate = scheduler.schedule(uiUpdater, delay,
                                                   TimeUnit.MILLISECONDS);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      synchronized (astGenerator) {
        astGenerator.getGui().disposeAllWindows();
      }
      Messages.loge("Thread stopped: " + editor.getSketch().getName());

      // TODO: clear last result

      running = false;
    }
  };


  public void start() {
    scheduler = Executors.newSingleThreadScheduledExecutor();
    errorCheckerThread = new Thread(mainLoop);
    errorCheckerThread.start();
  }


  public void stop() {
    cancel();
    running = false;
    errorCheckerThread.interrupt();
    if (scheduler != null) {
      scheduler.shutdownNow();
    }
  }


  public void cancel() {
    requestQueue.clear();
    nextUiUpdate = 0;
    if (scheduledUiUpdate != null) {
      scheduledUiUpdate.cancel(true);
    }
  }


  public void request() {
    nextUiUpdate = System.currentTimeMillis() + errorCheckInterval;
    requestQueue.offer(Boolean.TRUE);
  }


  public void addListener(Document doc) {
    if (doc != null) doc.addDocumentListener(sketchChangedListener);
  }


  protected void checkForMissingImports(PreprocessedSketch result) {
    if (Preferences.getBoolean(JavaMode.SUGGEST_IMPORTS_PREF)) {
      for (Problem p : result.problems) {
        if (p.getIProblem().getID() == IProblem.UndefinedType) {
          String args[] = p.getIProblem().getArguments();
          if (args.length > 0) {
            String missingClass = args[0];
            Messages.log("Will suggest for type:" + missingClass);
            //astGenerator.suggestImports(missingClass);
          }
        }
      }
    }
  }


  public ASTGenerator getASTGenerator() {
    return astGenerator;
  }


  protected final DocumentListener sketchChangedListener = new DocumentListener() {
    @Override
    public void insertUpdate(DocumentEvent e) {
      if (JavaMode.errorCheckEnabled) request();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      if (JavaMode.errorCheckEnabled) request();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
      if (JavaMode.errorCheckEnabled) request();
    }
  };


  public static class PreprocessedSketch {

    Sketch sketch;

    Mode mode;

    String className;

    CompilationUnit compilationUnit;

    ClassPath classPath;
    URLClassLoader classLoader;

    int[] tabStarts;

    String pdeCode;
    String preprocessedCode;

    SourceMapping syntaxMapping;
    SourceMapping compilationMapping;

    boolean hasSyntaxErrors;
    boolean hasCompilationErrors;

    final List<Problem> problems = new ArrayList<>();

    final List<ImportStatement> programImports = new ArrayList<>();
    final List<ImportStatement> coreAndDefaultImports = new ArrayList<>();
    final List<ImportStatement> codeFolderImports = new ArrayList<>();
  }

  protected PreprocessedSketch checkCode() {

    PreprocessedSketch result = new PreprocessedSketch();
    PreprocessedSketch prevResult = null; // TODO: get previous result

    List<ImportStatement> coreAndDefaultImports = result.coreAndDefaultImports;
    List<ImportStatement> codeFolderImports = result.codeFolderImports;
    List<ImportStatement> programImports = result.programImports;

    Sketch sketch = result.sketch = editor.getSketch();
    String className = result.className = sketch.getName();

    StringBuilder workBuffer = new StringBuilder();

    // Combine code into one buffer
    IntList tabStartsList = new IntList();
    for (SketchCode sc : sketch.getCode()) {
      if (sc.isExtension("pde")) {
        tabStartsList.append(workBuffer.length());
        if (sketch.getCurrentCode().equals(sc)) {
          try {
            workBuffer.append(sc.getDocumentText());
          } catch (BadLocationException e) {
            e.printStackTrace();
          }
        } else {
          workBuffer.append(sc.getProgram());
        }
        workBuffer.append('\n');
      }
    }
    result.tabStarts = tabStartsList.array();

    String pdeStage = result.pdeCode = workBuffer.toString();
    String syntaxStage;
    String javaStage;

    { // Prepare core and default imports
      // TODO: do this only once
      PdePreprocessor p = editor.createPreprocessor(null);
      String[] defaultImports = p.getDefaultImports();
      String[] coreImports = p.getCoreImports();

      for (String imp : coreImports) {
        coreAndDefaultImports.add(ImportStatement.parse(imp));
      }
      for (String imp : defaultImports) {
        coreAndDefaultImports.add(ImportStatement.parse(imp));
      }
    }

    // Prepare code folder imports
    if (sketch.hasCodeFolder()) {
      File codeFolder = sketch.getCodeFolder();
      String codeFolderClassPath = Util.contentsToClassPath(codeFolder);
      StringList codeFolderPackages = Util.packageListFromClassPath(codeFolderClassPath);
      for (String item : codeFolderPackages) {
        codeFolderImports.add(ImportStatement.wholePackage(item));
      }
    }

    // TODO: do we need to do this? why?
    //SourceUtils.substituteUnicode(rawCode);

    List<IProblem> syntaxProblems = Collections.emptyList();
    List<IProblem> compilationProblems = Collections.emptyList();

    CompilationUnit syntaxCU;

    {{ // SYNTAX CHECK

      try {
        SourceUtils.scrubCommentsAndStrings(workBuffer);
      } catch (RuntimeException e) {
        // Continue normally, comments were scrubbed
        // Unterminated comment will get caught during syntax check
      }

      Mode mode = PdePreprocessor.parseMode(workBuffer);

      // Prepare transforms
      SourceMapping syntaxMapping = new SourceMapping();
      syntaxMapping.addAll(SourceUtils.insertImports(coreAndDefaultImports));
      syntaxMapping.addAll(SourceUtils.insertImports(codeFolderImports));
      syntaxMapping.addAll(SourceUtils.parseProgramImports(workBuffer, programImports));
      syntaxMapping.addAll(SourceUtils.replaceTypeConstructors(workBuffer));
      syntaxMapping.addAll(SourceUtils.replaceHexLiterals(workBuffer));
      syntaxMapping.addAll(SourceUtils.wrapSketch(mode, className, workBuffer.length()));

      // TODO: all imports are parsed now, check if they need to be filtered somehow

      // Transform code
      syntaxStage = syntaxMapping.apply(pdeStage);

      // Create AST
      syntaxCU = makeAST(parser, syntaxStage.toCharArray(), COMPILER_OPTIONS);

      // Get syntax problems
      syntaxProblems = Arrays.asList(syntaxCU.getProblems());

      // Update result
      result.mode = mode;
      result.syntaxMapping = syntaxMapping;
      result.compilationUnit = syntaxCU;
      result.preprocessedCode = syntaxStage;
      for (IProblem problem : syntaxProblems) {
        if (problem.isError()) {
          result.hasSyntaxErrors = true;
        }
      }

    }}

    if (!result.hasSyntaxErrors) {

      {{ // COMPILATION CHECK

        // Prepare transforms
        SourceMapping compilationMapping = new SourceMapping();
        compilationMapping.addAll(SourceUtils.addPublicToTopLevelMethods(syntaxCU));
        compilationMapping.addAll(SourceUtils.replaceColorAndFixFloats(syntaxCU));

        // Transform code
        javaStage = compilationMapping.apply(syntaxStage);

        char[] javaStageChars = javaStage.toCharArray();

        // Create AST
        CompilationUnit compilationCU = makeAST(parser, javaStageChars, COMPILER_OPTIONS);

        // Prepare ClassLoader and ClassPath
        final URLClassLoader classLoader;
        final ClassPath classPath;

        boolean importsChanged = prevResult == null ||
            prevResult.classPath == null || prevResult.classLoader == null ||
            checkIfImportsChanged(programImports, prevResult.programImports) ||
            checkIfImportsChanged(codeFolderImports, prevResult.codeFolderImports);

        if (!importsChanged) {
          classLoader = result.classLoader = prevResult.classLoader;
          classPath = prevResult.classPath;
        } else {
          List<String> paths = prepareCompilerClasspath(programImports, sketch);

          // ClassLoader
          List<URL> urls = new ArrayList<>();
          for (Iterator<String> it = paths.iterator(); it.hasNext(); ) {
            String path = it.next();
            try {
              urls.add(new File(path).toURI().toURL());
            } catch (MalformedURLException e) {
              it.remove(); // malformed, get rid of it
            }
          }
          URL[] classPathArray = urls.toArray(new URL[urls.size()]);
          classLoader = result.classLoader = new URLClassLoader(classPathArray, null);

          // ClassPath
          String[] pathArray = paths.toArray(new String[paths.size()]);
          classPath = classPathFactory.createFromPaths(pathArray);
        }

        // Compile it
        try {
          compilationProblems =
              compileAndReturnProblems(className, javaStageChars,
                                       COMPILER_OPTIONS, classLoader);
        } catch (NoClassDefFoundError e) {
          // TODO: can this happen?
          e.printStackTrace();
        }

        // Update result
        result.compilationMapping = compilationMapping;
        result.classPath = classPath;
        result.classLoader = classLoader;
        result.preprocessedCode = javaStage;
        result.compilationUnit = compilationCU;
        for (IProblem problem : compilationProblems) {
          if (problem.isError()) {
            result.hasCompilationErrors = true;
          }
        }
      }}
    }

    List<Problem> mappedSyntaxProblems =
        mapProblems(syntaxProblems, result.tabStarts, result.pdeCode,
                    result.syntaxMapping);

    List<Problem> mappedCompilationProblems =
        mapProblems(compilationProblems, result.tabStarts, result.pdeCode,
                    result.compilationMapping, result.syntaxMapping);

    result.problems.addAll(mappedSyntaxProblems);
    result.problems.addAll(mappedCompilationProblems);

    return result;
  }


  protected static List<Problem> mapProblems(List<IProblem> problems,
                                             int[] tabStarts, String pdeCode,
                                             SourceMapping... mappings) {

    List<Problem> result = new ArrayList<>();

    for (IProblem problem : problems) {
      if (problem.isWarning() && !JavaMode.warningsEnabled) continue;

      // Hide a useless error which is produced when a line ends with
      // an identifier without a semicolon. "Missing a semicolon" is
      // also produced and is preferred over this one.
      // (Syntax error, insert ":: IdentifierOrNew" to complete Expression)
      // See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=405780
      if (problem.getMessage().contains("Syntax error, insert \":: IdentifierOrNew\"")) {
        continue;
      }

      int start = problem.getSourceStart();
      int stop = problem.getSourceEnd(); // inclusive

      for (SourceMapping mapping : mappings) {
        start = mapping.getInputOffset(start);
        stop = mapping.getInputOffset(stop);
      }

      int pdeStart = PApplet.constrain(start, 0, pdeCode.length()-1);
      int pdeStop = PApplet.constrain(stop + 1, 1, pdeCode.length()); // +1 for exclusive end

      // TODO: maybe optimize this, some people use tons of tabs
      int tab = Arrays.binarySearch(tabStarts, pdeStart);
      if (tab < 0) {
        tab = -(tab + 1) - 1;
      }

      int tabStart = tabStarts[tab];

      // TODO: quick hack; make it smart, fast & beautiful later
      int line = Util.countLines(pdeCode.substring(tabStart, pdeStart)) - 1;

      Problem p = new Problem(problem, tab, line);
      p.setPDEOffsets(pdeStart - tabStart, pdeStop - tabStart);
      result.add(p);
    }

    return result;
  }


  protected static CompilationUnit makeAST(ASTParser parser,
                                           char[] source,
                                           Map<String, String> options) {
    parser.setSource(source);
    parser.setKind(ASTParser.K_COMPILATION_UNIT);
    parser.setCompilerOptions(options);
    parser.setStatementsRecovery(true);

    return (CompilationUnit) parser.createAST(null);
  }


  public boolean hasSyntaxErrors(){
    return latestResult.hasSyntaxErrors;
  }


  /**
   * Performs compiler error check.
   * @param sourceName - name of the class
   * @param source - source code
   * @param options - compiler options
   * @param classLoader - custom classloader which can load all dependencies
   * @return list of compiler errors and warnings
   */
  static public List<IProblem> compileAndReturnProblems(String sourceName,
                                                        char[] source,
                                                        Map<String, String> options,
                                                        URLClassLoader classLoader) {
    final List<IProblem> problems = new ArrayList<>();

    ICompilerRequestor requestor = new ICompilerRequestor() {
      @Override
      public void acceptResult(CompilationResult cr) {
        if (cr.hasProblems()) Collections.addAll(problems, cr.getProblems());
      }
    };

    final char[] contents = source;
    final char[][] packageName = new char[][]{};
    final char[] mainTypeName = sourceName.toCharArray();
    final char[] fileName = (sourceName + ".java").toCharArray();

    ICompilationUnit unit = new ICompilationUnit() {
      @Override public char[] getContents() { return contents; }
      @Override public char[][] getPackageName() { return packageName; }
      @Override public char[] getMainTypeName() { return mainTypeName; }
      @Override public char[] getFileName() { return fileName; }
      @Override public boolean ignoreOptionalProblems() { return false; }
    };

    org.eclipse.jdt.internal.compiler.Compiler compiler =
        new Compiler(new NameEnvironmentImpl(classLoader),
                     DefaultErrorHandlingPolicies.proceedWithAllProblems(),
                     new CompilerOptions(options),
                     requestor,
                     new DefaultProblemFactory(Locale.getDefault()));

    compiler.compile(new ICompilationUnit[]{unit});
    return problems;
  }


  public CompilationUnit getLatestCU() {
    return latestResult.compilationUnit;
  }


  /**
   * Processes import statements to obtain class paths of contributed
   * libraries. This would be needed for compilation check. Also, adds
   * stuff(jar files, class files, candy) from the code folder. And it looks
   * messed up.
   */
  protected List<String> prepareCompilerClasspath(List<ImportStatement> programImports, Sketch sketch) {
    JavaMode mode = (JavaMode) editor.getMode();

    StringBuilder classPath = new StringBuilder();

    for (ImportStatement impstat : programImports) {
      String entry = impstat.getPackageName();
      if (!ignorableImport(entry)) {

        // Try to get the library classpath and add it to the list
        try {
          Library library = mode.getLibrary(entry);
          if (library != null) {
            classPath.append(library.getClassPath());
          }
        } catch (SketchException e) {
          // More libraries competing, ignore
        }
      }
    }

    if (sketch.hasCodeFolder()) {
      File codeFolder = sketch.getCodeFolder();
      String codeFolderClassPath = Util.contentsToClassPath(codeFolder);
      classPath.append(codeFolderClassPath);
    }

    // Also add jars specified in mode's search path
    // TODO: maybe we need mode.getCoreLibrary().getClassPath() here
    String searchPath = mode.getSearchPath();
    if (searchPath != null) {
      if (!searchPath.startsWith(File.pathSeparator)) {
        classPath.append(File.pathSeparator);
      }
      classPath.append(searchPath);
    }

    // TODO: maybe we need lib.getClassPath() here
    for (Library lib : mode.coreLibraries) {
      classPath.append(File.pathSeparator).append(lib.getJarPath());
    }

    String javaClassPath = System.getProperty("java.class.path");
    if (!javaClassPath.startsWith(File.pathSeparator)) {
      classPath.append(File.pathSeparator);
    }
    classPath.append(javaClassPath);

    String rtPath = System.getProperty("java.home") +
        File.separator + "lib" + File.separator + "rt.jar";
    if (new File(rtPath).exists()) {
      classPath.append(File.pathSeparator).append(rtPath);
    } else {
      rtPath = System.getProperty("java.home") + File.separator + "jre" +
          File.separator + "lib" + File.separator + "rt.jar";
      if (new File(rtPath).exists()) {
        classPath.append(File.pathSeparator).append(rtPath);
      }
    }

    // Make sure class path does not contain empty string (home dir)
    String[] paths = classPath.toString().split(File.pathSeparator);

    List<String> entries = new ArrayList<>();

    for (int i = 0; i < paths.length; i++) {
      String path = paths[i];
      if (path != null && !path.trim().isEmpty()) {
        entries.add(path);
      }
    }

    return entries;
  }


  /**
   * Ignore processing packages, java.*.*. etc.
   */
  protected boolean ignorableImport(String packageName) {
    return (packageName.startsWith("java.") ||
            packageName.startsWith("javax."));
  }


  protected boolean ignorableSuggestionImport(String impName) {

    // TODO: propagate these from last result

    /*String impNameLc = impName.toLowerCase();

    for (ImportStatement impS : programImports) {
      if (impNameLc.startsWith(impS.getPackageName().toLowerCase())) {
        return false;
      }
    }

    for (ImportStatement impS : codeFolderImports) {
      if (impNameLc.startsWith(impS.getPackageName().toLowerCase())) {
        return false;
      }
    }*/

    final String include = "include";
    final String exclude = "exclude";

    if (impName.startsWith("processing")) {
      if (JavaMode.suggestionsMap.containsKey(include) && JavaMode.suggestionsMap.get(include).contains(impName)) {
        return false;
      } else if (JavaMode.suggestionsMap.containsKey(exclude) && JavaMode.suggestionsMap.get(exclude).contains(impName)) {
        return true;
      }
    } else if (impName.startsWith("java")) {
      if (JavaMode.suggestionsMap.containsKey(include) && JavaMode.suggestionsMap.get(include).contains(impName)) {
        return false;
      }
    }

    return true;
  }

  static private final Map<String, String> COMPILER_OPTIONS;
  static {
    Map<String, String> compilerOptions = new HashMap<>();

    JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, compilerOptions);

    // See http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Fguide%2Fjdt_api_options.htm&anchor=compiler

    final String[] generate = {
        JavaCore.COMPILER_LINE_NUMBER_ATTR,
        JavaCore.COMPILER_SOURCE_FILE_ATTR
    };

    final String[] ignore = {
        JavaCore.COMPILER_PB_UNUSED_IMPORT,
        JavaCore.COMPILER_PB_MISSING_SERIAL_VERSION,
        JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE,
        JavaCore.COMPILER_PB_REDUNDANT_TYPE_ARGUMENTS,
        JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION
    };

    final String[] warn = {
        JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT,
        JavaCore.COMPILER_PB_NULL_REFERENCE,
        JavaCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE,
        JavaCore.COMPILER_PB_REDUNDANT_NULL_CHECK,
        JavaCore.COMPILER_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT,
        JavaCore.COMPILER_PB_UNUSED_LABEL,
        JavaCore.COMPILER_PB_UNUSED_LOCAL,
        JavaCore.COMPILER_PB_UNUSED_OBJECT_ALLOCATION,
        JavaCore.COMPILER_PB_UNUSED_PARAMETER,
        JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER
    };

    for (String s : generate) compilerOptions.put(s, JavaCore.GENERATE);
    for (String s : ignore)   compilerOptions.put(s, JavaCore.IGNORE);
    for (String s : warn)     compilerOptions.put(s, JavaCore.WARNING);

    COMPILER_OPTIONS = Collections.unmodifiableMap(compilerOptions);
  }


  /**
   * Updates the error table in the Error Window.
   */
  protected void updateErrorTable(List<Problem> problems) {
    try {
      ErrorTable table = editor.getErrorTable();
      table.clearRows();

//      String[][] errorData = new String[problemsList.size()][3];
//      int index = 0;
//      for (int i = 0; i < problemsList.size(); i++) {
      Sketch sketch = editor.getSketch();
      for (Problem p : problems) {
        String message = p.getMessage();
        if (Preferences.getBoolean(JavaMode.SUGGEST_IMPORTS_PREF)) {
          if (p.getIProblem().getID() == IProblem.UndefinedType) {
            String[] args = p.getIProblem().getArguments();
            if (args.length > 0) {
              String missingClass = args[0];
              String[] si;
              synchronized (astGenerator) {
                si = astGenerator.getSuggestImports(missingClass);
              }
              if (si != null && si.length > 0) {
                p.setImportSuggestions(si);
//                errorData[index][0] = "<html>" + p.getMessage() +
//                  " (<font color=#0000ff><u>Import Suggestions available</u></font>)</html>";
                message += " (double-click for suggestions)";
              }
            }
          }
        }

        table.addRow(p, message,
                     sketch.getCode(p.getTabIndex()).getPrettyName(),
                     Integer.toString(p.getLineNumber() + 1));
        // Added +1 because lineNumbers internally are 0-indexed

//        //TODO: This is temporary
//        if (tempErrorLog.size() < 200) {
//          tempErrorLog.put(p.getMessage(), p.getIProblem());
//        }

      }
//      table.updateColumns();

//      DefaultTableModel tm =
//        new DefaultTableModel(errorData, XQErrorTable.columnNames);
//      editor.updateTable(tm);

    } catch (Exception e) {
      Messages.loge("Exception at updateErrorTable()", e);
      e.printStackTrace();
      cancel();
    }
  }


  /**
   * Updates editor status bar, depending on whether the caret is on an error
   * line or not
   */
  public void updateEditorStatus() {
//    if (editor.getStatusMode() == EditorStatus.EDIT) return;

    // editor.statusNotice("Position: " +
    // editor.getTextArea().getCaretLine());
    if (JavaMode.errorCheckEnabled) {
      LineMarker errorMarker = editor.findError(editor.getTextArea().getCaretLine());
      if (errorMarker != null) {
        if (errorMarker.getType() == LineMarker.WARNING) {
          editor.statusMessage(errorMarker.getProblem().getMessage(),
                               EditorStatus.CURSOR_LINE_WARNING);
        } else {
          editor.statusMessage(errorMarker.getProblem().getMessage(),
                               EditorStatus.CURSOR_LINE_ERROR);
        }
      } else {
        switch (editor.getStatusMode()) {
          case EditorStatus.CURSOR_LINE_ERROR:
          case EditorStatus.CURSOR_LINE_WARNING:
            editor.statusEmpty();
            break;
        }
      }
    }

//    // This line isn't an error line anymore, so probably just clear it
//    if (editor.statusMessageType == JavaEditor.STATUS_COMPILER_ERR) {
//      editor.statusEmpty();
//      return;
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
  protected int[] JavaToPdeOffsets(int line, int offset) {

    return new int[] { 0, 0 }; // TODO

    /*
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
        return new int[] { 0, 0 }; // TODO
      } else {

        // Some seriously ugly stray error, just can't find the source
        // line! Simply return first line for first tab.
        return  new int[] { 0, 1 };
      }

    }

    try {
      for (SketchCode sc : editor.getSketch().getCode()) {
        if (sc.isExtension("pde")) {
          int len;
          if (editor.getSketch().getCurrentCode().equals(sc)) {
            len = Util.countLines(sc.getDocumentText()) + 1;
          } else {
            len = Util.countLines(sc.getProgram()) + 1;
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
      System.err.println("Error inside ErrorCheckerService.JavaToPdeOffset()");
      e.printStackTrace();
    }
    return new int[] { codeIndex, x };
    */
  }


  protected String getPdeCodeAtLine(int tab, int linenumber){
    if(linenumber < 0) return null;
    editor.getSketch().setCurrentCode(tab);
    return editor.getTextArea().getLineText(linenumber);
  }


  /**
   * Calculates the tab number and line number of the error in that particular
   * tab. Provides mapping between pure java and pde code.
   *
   * @param javalineNumber
   *            - int
   * @return int[0] - tab number, int[1] - line number
   */
  protected int[] calculateTabIndexAndLineNumber(int javalineNumber) {

    return new int[] { 0, 0 }; // TODO

    /*

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
        return new int[] { 0, 0 }; // TODO
      } else {

        // Some seriously ugly stray error, just can't find the source
        // line! Simply return first line for first tab.
        return new int[] { 0, 1 };
      }

    }

    try {
      for (SketchCode sc : editor.getSketch().getCode()) {
        if (sc.isExtension("pde")) {
          int len;
          if (editor.getSketch().getCurrentCode().equals(sc)) {
            len = Util.countLines(sc.getDocumentText()) + 1;
          } else {
            len = Util.countLines(sc.getProgram()) + 1;
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
      System.err.println("Things got messed up in ErrorCheckerService.calculateTabIndexAndLineNumber()");
    }
    return new int[] { codeIndex, x };

    */
  }


  /**
   * Now defunct.
   * The super method that highlights any ASTNode in the pde editor =D
   * @param awrap
   * @return true - if highlighting happened correctly.
   */
  private boolean highlightNode(ASTNodeWrapper awrap){
    Messages.log("Highlighting: " + awrap);
    try {
      int pdeoffsets[] = awrap.getPDECodeOffsets(this);
      int javaoffsets[] = awrap.getJavaCodeOffsets(this);
      Messages.log("offsets: " +pdeoffsets[0] + "," +
          pdeoffsets[1]+ "," +javaoffsets[1]+ "," +
          javaoffsets[2]);
      scrollToErrorLine(editor, pdeoffsets[0],
                                            pdeoffsets[1],javaoffsets[1],
                                            javaoffsets[2]);
      return true;
    } catch (Exception e) {
      Messages.loge("Scrolling failed for " + awrap);
      // e.printStackTrace();
    }
    return false;
  }


  public boolean highlightNode(ASTNode node){
    ASTNodeWrapper awrap = new ASTNodeWrapper(node);
    return highlightNode(awrap);
  }


  // TODO: does this belong here?
  // Thread: EDT
  public void scrollToErrorLine(Problem p) {
    if (editor == null) return;
    if (p == null) return;

    // Switch to tab
    editor.toFront();
    editor.getSketch().setCurrentCode(p.getTabIndex());

    // Highlight the code
    int startOffset = p.getStartOffset();
    int stopOffset = p.getStopOffset();

    int length = editor.getTextArea().getDocumentLength();
    startOffset = PApplet.constrain(startOffset, 0, length);
    stopOffset = PApplet.constrain(stopOffset, 0, length);
    editor.getTextArea().select(startOffset, stopOffset);

    // Scroll to error line
    editor.getTextArea().scrollToCaret();
    editor.repaint();
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
  protected static boolean scrollToErrorLine(Editor edt, int tabIndex, int lineNoInTab, int lineStartOffset, int length) {
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
      Messages.log(lineStartOffset + " LSO,len " + length);

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
  protected static boolean checkIfImportsChanged(List<ImportStatement> prevImports,
                                                 List<ImportStatement> imports) {
    if (imports.size() != prevImports.size()) {
      return true;
    } else {
      int count = imports.size();
      for (int i = 0; i < count; i++) {
        if (!imports.get(i).isSameAs(prevImports.get(i))) {
          return true;
        }
      }
    }
    return false;
  }

  protected int pdeImportsCount;

  public int getPdeImportsCount() {
    return pdeImportsCount;
  }

  public void handleErrorCheckingToggle() {
    if (!JavaMode.errorCheckEnabled) {
      Messages.log(editor.getSketch().getName() + " Error Checker disabled.");
      editor.getErrorPoints().clear();
      latestResult.problems.clear();
      updateErrorTable(Collections.<Problem>emptyList());
      updateEditorStatus();
      editor.getTextArea().repaint();
      editor.repaintErrorBar();
    } else {
      Messages.log(editor.getSketch().getName() + " Error Checker enabled.");
      request();
    }
  }


  private static class NameEnvironmentImpl implements INameEnvironment {

    private final ClassLoader classLoader;

    NameEnvironmentImpl(ClassLoader classLoader) {
      this.classLoader = classLoader;
    }


    @Override
    public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
      return readClassFile(CharOperation.toString(compoundTypeName), classLoader);
    }


    @Override
    public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
      String fullName = CharOperation.toString(packageName);
      if (typeName != null) {
        if (fullName.length() > 0) fullName += ".";
        fullName += new String(typeName);
      }
      return readClassFile(fullName, classLoader);
    }


    @Override
    public boolean isPackage(char[][] parentPackageName, char[] packageName) {
      String fullName = CharOperation.toString(parentPackageName);
      if (packageName != null) {
        if (fullName.length() > 0) fullName += ".";
        fullName += new String(packageName);
      }
      if (readClassFile(fullName, classLoader) != null) return false;
      try {
        return (classLoader.loadClass(fullName) == null);
      } catch (ClassNotFoundException e) {
        return true;
      }
    }


    @Override
    public void cleanup() { }


    private static NameEnvironmentAnswer readClassFile(String fullName, ClassLoader classLoader) {
      String classFileName = fullName.replace('.', '/') + ".class";

      InputStream is = classLoader.getResourceAsStream(classFileName);
      if (is == null) return null;

      byte[] buffer = new byte[8192];
      ByteArrayOutputStream os = new ByteArrayOutputStream(buffer.length);
      try {
        int bytes;
        while ((bytes = is.read(buffer, 0, buffer.length)) > 0) {
          os.write(buffer, 0, bytes);
        }
        os.flush();
        ClassFileReader classFileReader =
            new ClassFileReader(os.toByteArray(), fullName.toCharArray(), true);
        return new NameEnvironmentAnswer(classFileReader, null);
      } catch (IOException | ClassFormatException e) {
        return null;
      }
    }
  }
}
