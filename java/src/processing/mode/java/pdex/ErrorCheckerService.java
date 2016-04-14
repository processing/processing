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
import com.google.classpath.RegExpResourceFilter;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
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
import processing.app.ui.EditorStatus;
import processing.app.ui.ErrorTable;
import processing.core.PApplet;
import processing.data.IntList;
import processing.data.StringList;
import processing.mode.java.JavaMode;
import processing.mode.java.JavaEditor;
import processing.mode.java.preproc.PdePreprocessor;
import processing.mode.java.preproc.PdePreprocessor.Mode;


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

      latestResult = null;

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
    PreprocessedSketch prevResult = latestResult;

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

    // TODO: convert unicode escapes to chars

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
      List<IProblem> syntaxProblems = Arrays.asList(syntaxCU.getProblems());

      // Update result
      result.mode = mode;
      result.syntaxMapping = syntaxMapping;
      result.compilationUnit = syntaxCU;
      result.preprocessedCode = syntaxStage;
      result.hasSyntaxErrors = syntaxProblems.stream().anyMatch(IProblem::isError);
      List<Problem> mappedSyntaxProblems =
          mapProblems(syntaxProblems, result.tabStarts, result.pdeCode,
                      result.syntaxMapping);
      result.problems.addAll(mappedSyntaxProblems);
    }}

    { // Prepare ClassLoader and ClassPath
      final URLClassLoader classLoader;
      final ClassPath classPath;

      boolean importsChanged = prevResult == null ||
          prevResult.classPath == null || prevResult.classLoader == null ||
          checkIfImportsChanged(programImports, prevResult.programImports) ||
          checkIfImportsChanged(codeFolderImports, prevResult.codeFolderImports);

      if (!importsChanged) {
        classLoader = prevResult.classLoader;
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
        classLoader = new URLClassLoader(classPathArray, null);
        // ClassPath
        String[] pathArray = paths.toArray(new String[paths.size()]);
        classPath = classPathFactory.createFromPaths(pathArray);
      }

      // Update result
      result.classPath = classPath;
      result.classLoader = classLoader;
    }


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

        // Compile it
        List<IProblem> compilationProblems =
            compileAndReturnProblems(className, javaStageChars,
                                     COMPILER_OPTIONS, result.classLoader,
                                     result.classPath);

        // Update result
        result.compilationMapping = compilationMapping;
        result.preprocessedCode = javaStage;
        result.compilationUnit = compilationCU;

        List<Problem> mappedCompilationProblems =
            mapProblems(compilationProblems, result.tabStarts, result.pdeCode,
                        result.compilationMapping, result.syntaxMapping);

        if (Preferences.getBoolean(JavaMode.SUGGEST_IMPORTS_PREF)) {
          Map<String, List<Problem>> undefinedTypeProblems = mappedCompilationProblems.stream()
              // Get only problems with undefined types/names
              .filter(p -> {
                int id = p.getIProblem().getID();
                return id == IProblem.UndefinedType || id == IProblem.UndefinedName;
              })
              // Group problems by the missing type/name
              .collect(Collectors.groupingBy(p -> p.getIProblem().getArguments()[0]));

          // TODO: cache this, invalidate if code folder or libraries change
          final ClassPath cp = undefinedTypeProblems.isEmpty() ?
              null :
              buildImportSuggestionClassPath();

          // Get suggestions for each missing type, update the problems
          undefinedTypeProblems.entrySet().stream()
              .forEach(entry -> {
                String missingClass = entry.getKey();
                List<Problem> problems = entry.getValue();
                String[] suggestions = getImportSuggestions(cp, missingClass);
                problems.forEach(p -> p.setImportSuggestions(suggestions));
              });
        }

        result.problems.addAll(mappedCompilationProblems);

        result.hasCompilationErrors = mappedCompilationProblems.stream()
            .anyMatch(Problem::isError);
      }}
    }

    return result;
  }


  public ClassPath buildImportSuggestionClassPath() {
    // TODO: make sure search class path is complete,
    //       prepare it beforehand and reuse it
    //
    //       this in not the same as sketch class path!
    //       should include:
    //       - all contributed libraries
    //       - core libraries
    //       - code folder
    //       - mode search path
    //       - Java classpath


    JavaMode mode = (JavaMode) editor.getMode();

    StringBuilder classPath = new StringBuilder();

    Sketch sketch = editor.getSketch();

    if (sketch.hasCodeFolder()) {
      File codeFolder = sketch.getCodeFolder();
      String codeFolderClassPath = Util.contentsToClassPath(codeFolder);
      classPath.append(codeFolderClassPath);
    }

    // Also add jars specified in mode's search path
    // TODO: maybe we need mode.getCoreLibrary().getClassPath() here
    String searchPath = mode.getSearchPath();
    if (searchPath != null) {
      classPath.append(File.pathSeparator).append(searchPath);
    }

    for (Library lib : mode.coreLibraries) {
      classPath.append(File.pathSeparator).append(lib.getClassPath());
    }

    for (Library lib : mode.contribLibraries) {
      classPath.append(File.pathSeparator).append(lib.getClassPath());
    }

    String javaClassPath = System.getProperty("java.class.path");
    classPath.append(File.pathSeparator).append(javaClassPath);

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

    String path = Arrays.stream(paths)
        .filter(p -> p != null && !p.trim().isEmpty())
        .collect(Collectors.joining(File.pathSeparator));

    return classPathFactory.createFromPath(path);
  }


  public String[] getImportSuggestions(ClassPath cp, String className) {
    RegExpResourceFilter regf = new RegExpResourceFilter(
        Pattern.compile(".*"),
        Pattern.compile("(.*\\$)?" + className + "\\.class",
                        Pattern.CASE_INSENSITIVE));

    String[] resources = cp.findResources("", regf);
    return Arrays.stream(resources)
        // remove ".class" suffix
        .map(res -> res.substring(0, res.length() - 6))
        // replace path separators with dots
        .map(res -> res.replace('/', '.'))
        // replace inner class separators with dots
        .map(res -> res.replace('$', '.'))
        // sort, prioritize clases from java. package
        .sorted((o1, o2) -> {
          // put java.* first, should be prioritized more
          boolean o1StartsWithJava = o1.startsWith("java");
          boolean o2StartsWithJava = o2.startsWith("java");
          if (o1StartsWithJava != o2StartsWithJava) {
            if (o1StartsWithJava) return -1;
            return 1;
          }
          return o1.compareTo(o2);
        })
        .toArray(String[]::new);
  }


  protected static List<Problem> mapProblems(List<IProblem> problems,
                                             int[] tabStarts, String pdeCode,
                                             SourceMapping... mappings) {
    return problems.stream()
        // Filter Warnings if they are not enabled
        .filter(iproblem -> !(iproblem.isWarning() && !JavaMode.warningsEnabled))
        // Hide a useless error which is produced when a line ends with
        // an identifier without a semicolon. "Missing a semicolon" is
        // also produced and is preferred over this one.
        // (Syntax error, insert ":: IdentifierOrNew" to complete Expression)
        // See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=405780
        .filter(iproblem -> !iproblem.getMessage()
            .contains("Syntax error, insert \":: IdentifierOrNew\""))
        // Transform into our Problems
        .map(iproblem -> {
          int start = iproblem.getSourceStart();
          int stop = iproblem.getSourceEnd(); // inclusive

          // Apply mappings
          for (SourceMapping mapping : mappings) {
            start = mapping.getInputOffset(start);
            stop = mapping.getInputOffset(stop);
          }

          if (stop < start) {
            // Should not happen, just to be sure
            int temp = start;
            start = stop;
            stop = temp;
          }

          int pdeStart = PApplet.constrain(start, 0, pdeCode.length()-1);
          int pdeStop = PApplet.constrain(stop + 1, 1, pdeCode.length()); // +1 for exclusive end

          int tab = Arrays.binarySearch(tabStarts, pdeStart);
          if (tab < 0) {
            tab = -(tab + 1) - 1;
          }

          int tabStart = tabStarts[tab];

          // TODO: quick hack; make it smart, fast & beautiful later
          int line = Util.countLines(pdeCode.substring(tabStart, pdeStart)) - 1;

          Problem problem = new Problem(iproblem, tab, line);
          problem.setPDEOffsets(pdeStart - tabStart, pdeStop - tabStart);

          return problem;
        })
        .collect(Collectors.toList());
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
                                                        URLClassLoader classLoader,
                                                        ClassPath classPath) {
    final List<IProblem> problems = new ArrayList<>();

    ICompilerRequestor requestor = cr -> {
      if (cr.hasProblems()) Collections.addAll(problems, cr.getProblems());
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
        new Compiler(new NameEnvironmentImpl(classLoader, classPath),
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

    // TODO: eliminate duplication in buildImportSuggestionClassPath

    JavaMode mode = (JavaMode) editor.getMode();

    StringBuilder classPath = new StringBuilder();

    programImports.stream()
        .map(ImportStatement::getPackageName)
        .filter(pckg -> !ignorableImport(pckg))
        .map(pckg -> {
          try {
            return mode.getLibrary(pckg); // TODO: this may not be thread-safe
          } catch (SketchException e) {
            return null;
          }
        })
        .filter(lib -> lib != null)
        .map(Library::getClassPath)
        .forEach(cp -> classPath.append(File.pathSeparator).append(cp));

    if (sketch.hasCodeFolder()) {
      File codeFolder = sketch.getCodeFolder();
      String codeFolderClassPath = Util.contentsToClassPath(codeFolder);
      classPath.append(codeFolderClassPath);
    }

    // Also add jars specified in mode's search path
    // TODO: maybe we need mode.getCoreLibrary().getClassPath() here
    String searchPath = mode.getSearchPath();
    if (searchPath != null) {
      classPath.append(File.pathSeparator).append(searchPath);
    }

    for (Library lib : mode.coreLibraries) {
      classPath.append(File.pathSeparator).append(lib.getJarPath());
    }

    for (Library lib : mode.contribLibraries) {
      classPath.append(File.pathSeparator).append(lib.getJarPath());
    }

//    String javaClassPath = System.getProperty("java.class.path");
//    classPath.append(File.pathSeparator).append(javaClassPath);

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

    return Arrays.stream(paths)
        .filter(p -> p != null && !p.trim().isEmpty())
        .distinct()
        .collect(Collectors.toList());
  }


  /**
   * Ignore processing packages, java.*.*. etc.
   */
  protected boolean ignorableImport(String packageName) {
    return (packageName.startsWith("java.") ||
            packageName.startsWith("javax."));
  }


  protected boolean ignorableSuggestionImport(String impName) {

    String impNameLc = impName.toLowerCase();

    for (ImportStatement impS : latestResult.programImports) {
      if (impNameLc.startsWith(impS.getPackageName().toLowerCase())) {
        return false;
      }
    }

    for (ImportStatement impS : latestResult.codeFolderImports) {
      if (impNameLc.startsWith(impS.getPackageName().toLowerCase())) {
        return false;
      }
    }

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

      Sketch sketch = editor.getSketch();
      for (Problem p : problems) {
        String message = p.getMessage();
        if (Preferences.getBoolean(JavaMode.SUGGEST_IMPORTS_PREF) &&
            p.getImportSuggestions() != null &&
            p.getImportSuggestions().length > 0) {
          message += " (double-click for suggestions)";
        }

        table.addRow(p, message,
                     sketch.getCode(p.getTabIndex()).getPrettyName(),
                     Integer.toString(p.getLineNumber() + 1));
        // Added +1 because lineNumbers internally are 0-indexed
      }
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


  protected static int mapJavaToTab(PreprocessedSketch sketch, int offset) {
    int tab = Arrays.binarySearch(sketch.tabStarts, offset);
    if (tab < 0) {
      tab = -(tab + 1) - 1;
    }

    return sketch.tabStarts[tab];
  }


  protected static int mapJavaToProcessing(PreprocessedSketch sketch, int offset) {
    SourceMapping syntaxMapping = sketch.syntaxMapping;
    SourceMapping compilationMapping = sketch.compilationMapping;

    if (compilationMapping != null) {
      offset = compilationMapping.getInputOffset(offset);
    }

    if (syntaxMapping != null) {
      offset = syntaxMapping.getInputOffset(offset);
    }

    return offset;
  }


  protected static int mapProcessingToJava(PreprocessedSketch sketch, int offset) {
    SourceMapping syntaxMapping = sketch.syntaxMapping;
    SourceMapping compilationMapping = sketch.compilationMapping;

    if (syntaxMapping != null) {
      offset = syntaxMapping.getOutputOffset(offset);
    }

    if (compilationMapping != null) {
      offset = compilationMapping.getOutputOffset(offset);
    }

    return offset;
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
    private final ClassPath classPath;

    NameEnvironmentImpl(ClassLoader classLoader, ClassPath classPath) {
      this.classLoader = classLoader;
      this.classPath = classPath;
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
      return classPath.isPackage(fullName.replace('.', '/'));
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
