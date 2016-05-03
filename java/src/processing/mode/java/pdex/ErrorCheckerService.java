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
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import processing.app.Library;
import processing.app.Messages;
import processing.app.Preferences;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.SketchException;
import processing.app.Util;
import processing.data.IntList;
import processing.data.StringList;
import processing.mode.java.JavaEditor;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.PreprocessedSketch.SketchInterval;
import processing.mode.java.pdex.TextTransform.OffsetMapper;
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
   * ASTGenerator for operations on AST
   */
  protected final ASTGenerator astGenerator;

  /**
   * Error checking doesn't happen before this interval has ellapsed since the
   * last request() call.
   */
  private final static long errorCheckInterval = 650;

  private Thread errorCheckerThread;
  private final BlockingQueue<Boolean> requestQueue = new ArrayBlockingQueue<>(1);
  private ScheduledExecutorService scheduler;
  private volatile ScheduledFuture<?> scheduledUiUpdate = null;
  private volatile long nextUiUpdate = 0;

  private final Object requestLock = new Object();
  private boolean needsCheck = false;

  private AtomicBoolean codeFolderChanged = new AtomicBoolean(true);
  private AtomicBoolean librariesChanged = new AtomicBoolean(true);

  private CompletableFuture<PreprocessedSketch> preprocessingTask = new CompletableFuture<>();

  private CompletableFuture<?> lastCallback =
      new CompletableFuture() {{
        complete(null); // initialization block
      }};

  private final Consumer<PreprocessedSketch> errorHandlerListener = this::handleSketchProblems;

  private volatile boolean isEnabled = true;
  private volatile boolean isContinuousCheckEnabled = true;


  public ErrorCheckerService(JavaEditor editor) {
    this.editor = editor;
    astGenerator = new ASTGenerator(editor, this);
    isEnabled = !editor.hasJavaTabs();
    isContinuousCheckEnabled = JavaMode.errorCheckEnabled;
    registerDoneListener(errorHandlerListener);
  }


  private void mainLoop() {
    running = true;
    PreprocessedSketch prevResult = null;
    while (running) {
      try {
        try {
          requestQueue.take(); // blocking until check requested
        } catch (InterruptedException e) {
          running = false;
          break;
        }

        Messages.log("Starting preprocessing");

        prevResult = preprocessSketch(prevResult);

        synchronized (requestLock) {
          if (requestQueue.isEmpty()) {
            Messages.log("Completing preprocessing");
            preprocessingTask.complete(prevResult);
          }
        }
      } catch (Exception e) {
        Messages.loge("problem in error checker loop", e);
      }
    }

    astGenerator.getGui().disposeAllWindows();
  }


  public void start() {
    scheduler = Executors.newSingleThreadScheduledExecutor();
    errorCheckerThread = new Thread(this::mainLoop, "ECS");
    errorCheckerThread.start();
  }


  public void stop() {
    cancel();
    running = false;
    if (errorCheckerThread != null) {
      running = false;
      errorCheckerThread.interrupt();
    }
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


  public void notifySketchChanged() {
    if (!isEnabled) return;
    synchronized (requestLock) {
      if (preprocessingTask.isDone()) {
        preprocessingTask = new CompletableFuture<>();
        // Register callback which executes all listeners
        registerCallback(this::fireDoneListeners);
      }
      if (isContinuousCheckEnabled) {
        // Continuous check enabled, request
        nextUiUpdate = System.currentTimeMillis() + errorCheckInterval;
        requestQueue.offer(Boolean.TRUE);
      } else {
        // Continuous check not enabled, take note
        needsCheck = true;
      }
    }
  }


  public void notifyLibrariesChanged() {
    librariesChanged.set(true);
    Messages.log("Notify libraries changed");
    notifySketchChanged();
  }


  public void notifyCodeFolderChanged() {
    codeFolderChanged.set(true);
    Messages.log("Notify code folder changed");
    notifySketchChanged();
  }


  private void registerCallback(Consumer<PreprocessedSketch> callback) {
    if (!isEnabled) return;
    synchronized (requestLock) {
      lastCallback = preprocessingTask
          // Run callback after both preprocessing task and previous callback
          .thenAcceptBothAsync(lastCallback, (ps, a) -> callback.accept(ps))
          // Make sure exception in callback won't cancel whole callback chain
          .handleAsync((res, e) -> {
            if (e != null) Messages.loge("problem during preprocessing callback", e);
            return res;
          });
    }
  }


  public void acceptWhenDone(Consumer<PreprocessedSketch> callback) {
    if (!isEnabled) return;
    synchronized (requestLock) {
      // Continuous check not enabled, request check now
      if (needsCheck && !isContinuousCheckEnabled) {
        needsCheck = false;
        requestQueue.offer(Boolean.TRUE);
      }
      registerCallback(callback);
    }
  }



  /// LISTENERS ----------------------------------------------------------------


  private Set<Consumer<PreprocessedSketch>> doneListeners = new CopyOnWriteArraySet<>();


  public void registerDoneListener(Consumer<PreprocessedSketch> listener) {
    if (listener != null) doneListeners.add(listener);
  }


  public void unregisterDoneListener(Consumer<PreprocessedSketch> listener) {
    doneListeners.remove(listener);
  }


  private void fireDoneListeners(PreprocessedSketch ps) {
    for (Consumer<PreprocessedSketch> listener : doneListeners) {
      try {
        listener.accept(ps);
      } catch (Exception e) {
        Messages.loge("error when firing ecs listener", e);
      }
    }
  }


  /// --------------------------------------------------------------------------



  public void addDocumentListener(Document doc) {
    if (doc != null) doc.addDocumentListener(sketchChangedListener);
  }


  public ASTGenerator getASTGenerator() {
    return astGenerator;
  }


  protected final DocumentListener sketchChangedListener = new DocumentListener() {
    @Override
    public void insertUpdate(DocumentEvent e) {
      notifySketchChanged();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      notifySketchChanged();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
      notifySketchChanged();
    }
  };


  private PreprocessedSketch preprocessSketch(PreprocessedSketch prevResult) {

    boolean firstCheck = prevResult == null;

    PreprocessedSketch.Builder result = new PreprocessedSketch.Builder();

    List<ImportStatement> codeFolderImports = result.codeFolderImports;
    List<ImportStatement> programImports = result.programImports;

    JavaMode javaMode = (JavaMode) editor.getMode();
    Sketch sketch = result.sketch = editor.getSketch();
    String className = sketch.getName();

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
    result.tabStartOffsets = tabStartsList.array();

    String pdeStage = result.pdeCode = workBuffer.toString();


    boolean reloadCodeFolder = firstCheck || codeFolderChanged.getAndSet(false);
    boolean reloadLibraries = firstCheck || librariesChanged.getAndSet(false);

    // Core and default imports
    if (coreAndDefaultImports == null) {
      PdePreprocessor p = editor.createPreprocessor(null);
      coreAndDefaultImports = buildCoreAndDefaultImports(p);
    }
    result.coreAndDefaultImports.addAll(coreAndDefaultImports);

    // Prepare code folder imports
    if (reloadCodeFolder) {
      codeFolderImports.addAll(buildCodeFolderImports(sketch));
    } else {
      codeFolderImports.addAll(prevResult.codeFolderImports);
    }

    // TODO: convert unicode escapes to chars

    SourceUtils.scrubCommentsAndStrings(workBuffer);

    Mode sketchMode = PdePreprocessor.parseMode(workBuffer);

    // Prepare transforms to convert pde code into parsable code
    TextTransform toParsable = new TextTransform(pdeStage);
    toParsable.addAll(SourceUtils.insertImports(coreAndDefaultImports));
    toParsable.addAll(SourceUtils.insertImports(codeFolderImports));
    toParsable.addAll(SourceUtils.parseProgramImports(workBuffer, programImports));
    toParsable.addAll(SourceUtils.replaceTypeConstructors(workBuffer));
    toParsable.addAll(SourceUtils.replaceHexLiterals(workBuffer));
    toParsable.addAll(SourceUtils.wrapSketch(sketchMode, className, workBuffer.length()));

    { // Refresh sketch classloader and classpath if imports changed
      if (javaRuntimeClassPath == null) {
        javaRuntimeClassPath = buildJavaRuntimeClassPath();
        sketchModeClassPath = buildModeClassPath(javaMode, false);
        searchModeClassPath = buildModeClassPath(javaMode, true);
      }

      if (reloadLibraries) {
        coreLibraryClassPath = buildCoreLibraryClassPath(javaMode);
      }

      boolean rebuildLibraryClassPath = reloadLibraries ||
          checkIfImportsChanged(programImports, prevResult.programImports);

      if (rebuildLibraryClassPath) {
        sketchLibraryClassPath = buildSketchLibraryClassPath(javaMode, programImports);
        searchLibraryClassPath = buildSearchLibraryClassPath(javaMode);
      }

      boolean rebuildClassPath = reloadCodeFolder || rebuildLibraryClassPath ||
          prevResult.classLoader == null || prevResult.classPath == null ||
          prevResult.classPathArray == null || prevResult.searchClassPath == null;

      if (reloadCodeFolder) {
        codeFolderClassPath = buildCodeFolderClassPath(sketch);
      }

      if (rebuildClassPath) {
        { // Sketch class path
          List<String> sketchClassPath = new ArrayList<>();
          sketchClassPath.addAll(javaRuntimeClassPath);
          sketchClassPath.addAll(sketchModeClassPath);
          sketchClassPath.addAll(sketchLibraryClassPath);
          sketchClassPath.addAll(coreLibraryClassPath);
          sketchClassPath.addAll(codeFolderClassPath);

          String[] classPathArray = sketchClassPath.stream().toArray(String[]::new);
          URL[] urlArray = Arrays.stream(classPathArray)
              .map(path -> {
                try {
                  return Paths.get(path).toUri().toURL();
                } catch (MalformedURLException e) {
                  Messages.loge("malformed URL when preparing sketch classloader", e);
                  return null;
                }
              })
              .filter(url -> url != null)
              .toArray(URL[]::new);
          result.classLoader = new URLClassLoader(urlArray, null);
          result.classPath = classPathFactory.createFromPaths(classPathArray);
          result.classPathArray = classPathArray;
        }

        { // Search class path
          List<String> searchClassPath = new ArrayList<>();
          searchClassPath.addAll(javaRuntimeClassPath);
          searchClassPath.addAll(searchModeClassPath);
          searchClassPath.addAll(searchLibraryClassPath);
          searchClassPath.addAll(coreLibraryClassPath);
          searchClassPath.addAll(codeFolderClassPath);

          String[] searchClassPathArray = searchClassPath.stream().toArray(String[]::new);
          result.searchClassPath = classPathFactory.createFromPaths(searchClassPathArray);
        }
      } else {
        result.classLoader = prevResult.classLoader;
        result.classPath = prevResult.classPath;
        result.searchClassPath = prevResult.searchClassPath;
        result.classPathArray = prevResult.classPathArray;
      }
    }

    // Transform code to parsable state
    String parsableStage = toParsable.apply();
    OffsetMapper parsableMapper = toParsable.getMapper();

    // Create intermediate AST for advanced preprocessing
    CompilationUnit parsableCU =
        makeAST(parser, parsableStage.toCharArray(), COMPILER_OPTIONS);

    // Prepare advanced transforms which operate on AST
    TextTransform toCompilable = new TextTransform(parsableStage);
    toCompilable.addAll(SourceUtils.addPublicToTopLevelMethods(parsableCU));
    toCompilable.addAll(SourceUtils.replaceColorAndFixFloats(parsableCU));

    // Transform code to compilable state
    String compilableStage = toCompilable.apply();
    OffsetMapper compilableMapper = toCompilable.getMapper();
    char[] compilableStageChars = compilableStage.toCharArray();

    // Create compilable AST to get syntax problems
    CompilationUnit compilableCU =
        makeAST(parser, compilableStageChars, COMPILER_OPTIONS);

    // Get syntax problems from compilable AST
    List<IProblem> syntaxProblems = Arrays.asList(compilableCU.getProblems());
    result.problems.addAll(syntaxProblems);
    result.hasSyntaxErrors = syntaxProblems.stream().anyMatch(IProblem::isError);

    // Generate bindings after getting problems - avoids
    // 'missing type' errors when there are syntax problems
    CompilationUnit bindingsCU =
        makeASTWithBindings(parser, compilableStageChars, COMPILER_OPTIONS,
                            className, result.classPathArray);

    // Show compilation problems only when there are no syntax problems
    if (!result.hasSyntaxErrors) {
      result.problems.clear(); // clear warnings, they will be generated again
      List<IProblem> bindingsProblems = Arrays.asList(bindingsCU.getProblems());
      result.problems.addAll(bindingsProblems);
      result.hasCompilationErrors = bindingsProblems.stream().anyMatch(IProblem::isError);
    }

    // Update builder
    result.offsetMapper = parsableMapper.thenMapping(compilableMapper);
    result.javaCode = compilableStage;
    result.compilationUnit = bindingsCU;

    // Build it
    return result.build();
  }


  private void handleSketchProblems(PreprocessedSketch ps) {
    // Process problems
    final List<Problem> problems = ps.problems.stream()
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
          int stop = iproblem.getSourceEnd() + 1; // make it exclusive
          SketchInterval in = ps.mapJavaToSketch(start, stop);
          int line = ps.tabOffsetToTabLine(in.tabIndex, in.startTabOffset);
          Problem p = new Problem(iproblem, in.tabIndex, line);
          p.setPDEOffsets(in.startTabOffset, in.stopTabOffset);
          return p;
        })
        .collect(Collectors.toList());

    // Handle import suggestions
    if (Preferences.getBoolean(JavaMode.SUGGEST_IMPORTS_PREF)) {
      Map<String, List<Problem>> undefinedTypeProblems = problems.stream()
          // Get only problems with undefined types/names
          .filter(p -> {
            int id = p.getIProblem().getID();
            return id == IProblem.UndefinedType ||
                id == IProblem.UndefinedName ||
                id == IProblem.UnresolvedVariable;
          })
          // Group problems by the missing type/name
          .collect(Collectors.groupingBy(p -> p.getIProblem().getArguments()[0]));

      if (!undefinedTypeProblems.isEmpty()) {
        final ClassPath cp = ps.searchClassPath;

        // Get suggestions for each missing type, update the problems
        undefinedTypeProblems.entrySet().stream()
            .forEach(entry -> {
              String missingClass = entry.getKey();
              List<Problem> affectedProblems = entry.getValue();
              String[] suggestions = getImportSuggestions(cp, missingClass);
              affectedProblems.forEach(p -> p.setImportSuggestions(suggestions));
            });
      }
    }

    if (scheduledUiUpdate != null) {
      scheduledUiUpdate.cancel(true);
    }
    // Update UI after a delay. See #2677
    long delay = nextUiUpdate - System.currentTimeMillis();
    Runnable uiUpdater = () -> {
      if (nextUiUpdate > 0 && System.currentTimeMillis() >= nextUiUpdate) {
        EventQueue.invokeLater(() -> {
          if (isContinuousCheckEnabled) {
            editor.setProblemList(problems);
          }
        });
      }
    };
    scheduledUiUpdate = scheduler.schedule(uiUpdater, delay,
                                           TimeUnit.MILLISECONDS);
  }



  /// IMPORTS -----------------------------------------------------------------

  private List<ImportStatement> coreAndDefaultImports;


  private static List<ImportStatement> buildCoreAndDefaultImports(PdePreprocessor p) {
    List<ImportStatement> result = new ArrayList<>();

    for (String imp : p.getCoreImports()) {
      result.add(ImportStatement.parse(imp));
    }
    for (String imp : p.getDefaultImports()) {
      result.add(ImportStatement.parse(imp));
    }

    return result;
  }


  private static List<ImportStatement> buildCodeFolderImports(Sketch sketch) {
    if (sketch.hasCodeFolder()) {
      File codeFolder = sketch.getCodeFolder();
      String codeFolderClassPath = Util.contentsToClassPath(codeFolder);
      StringList codeFolderPackages = Util.packageListFromClassPath(codeFolderClassPath);
      return StreamSupport.stream(codeFolderPackages.spliterator(), false)
          .map(ImportStatement::wholePackage)
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }



  /// CLASSPATHS ---------------------------------------------------------------


  private List<String> javaRuntimeClassPath;

  private List<String> sketchModeClassPath;
  private List<String> searchModeClassPath;

  private List<String> coreLibraryClassPath;

  private List<String> codeFolderClassPath;

  private List<String> searchLibraryClassPath;
  private List<String> sketchLibraryClassPath;


  private static List<String> buildCodeFolderClassPath(Sketch sketch) {
    StringBuilder classPath = new StringBuilder();

    // Code folder
    if (sketch.hasCodeFolder()) {
      File codeFolder = sketch.getCodeFolder();
      String codeFolderClassPath = Util.contentsToClassPath(codeFolder);
      classPath.append(codeFolderClassPath);
    }

    return sanitizeClassPath(classPath.toString());
  }


  private static List<String> buildModeClassPath(JavaMode mode, boolean search) {
    StringBuilder classPath = new StringBuilder();

    if (search) {
      String searchClassPath = mode.getSearchPath();
      if (searchClassPath != null) {
        classPath.append(File.pathSeparator).append(searchClassPath);
      }
    } else {
      Library coreLibrary = mode.getCoreLibrary();
      String coreClassPath = coreLibrary != null ?
          coreLibrary.getClassPath() : mode.getSearchPath();
      if (coreClassPath != null) {
        classPath.append(File.pathSeparator).append(coreClassPath);
      }
    }

    return sanitizeClassPath(classPath.toString());
  }


  private static List<String> buildCoreLibraryClassPath(JavaMode mode) {
    StringBuilder classPath = new StringBuilder();

    for (Library lib : mode.coreLibraries) {
      classPath.append(File.pathSeparator).append(lib.getClassPath());
    }

    return sanitizeClassPath(classPath.toString());
  }

  private static List<String> buildSearchLibraryClassPath(JavaMode mode) {
    StringBuilder classPath = new StringBuilder();

    for (Library lib : mode.contribLibraries) {
      classPath.append(File.pathSeparator).append(lib.getClassPath());
    }

    return sanitizeClassPath(classPath.toString());
  }


  private List<String> buildSketchLibraryClassPath(JavaMode mode,
                                                   List<ImportStatement> programImports) {
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

    return sanitizeClassPath(classPath.toString());
  }


  private List<String> buildJavaRuntimeClassPath() {
    StringBuilder classPath = new StringBuilder();

    // Java runtime
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

    return sanitizeClassPath(classPath.toString());
  }


  private static List<String> sanitizeClassPath(String classPathString) {
    // Make sure class path does not contain empty string (home dir)
    return Arrays.stream(classPathString.split(File.pathSeparator))
        .filter(p -> p != null && !p.trim().isEmpty())
        .distinct()
        .collect(Collectors.toList());
  }

  /// --------------------------------------------------------------------------



  public static String[] getImportSuggestions(ClassPath cp, String className) {
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


  protected static CompilationUnit makeAST(ASTParser parser,
                                           char[] source,
                                           Map<String, String> options) {
    parser.setSource(source);
    parser.setKind(ASTParser.K_COMPILATION_UNIT);
    parser.setCompilerOptions(options);
    parser.setStatementsRecovery(true);

    return (CompilationUnit) parser.createAST(null);
  }


  protected static CompilationUnit makeASTWithBindings(ASTParser parser,
                                                       char[] source,
                                                       Map<String, String> options,
                                                       String className,
                                                       String[] classPath) {
    parser.setSource(source);
    parser.setKind(ASTParser.K_COMPILATION_UNIT);
    parser.setCompilerOptions(options);
    parser.setStatementsRecovery(true);
    parser.setUnitName(className);
    parser.setEnvironment(classPath, null, null, false);
    parser.setResolveBindings(true);

    return (CompilationUnit) parser.createAST(null);
  }


  /**
   * Ignore processing packages, java.*.*. etc.
   */
  protected boolean ignorableImport(String packageName) {
    return (packageName.startsWith("java.") ||
            packageName.startsWith("javax."));
  }


  protected static boolean ignorableSuggestionImport(PreprocessedSketch ps, String impName) {

    String impNameLc = impName.toLowerCase();

    List<ImportStatement> programImports = ps.programImports;
    List<ImportStatement> codeFolderImports = ps.codeFolderImports;

    boolean isImported = Stream
        .concat(programImports.stream(), codeFolderImports.stream())
        .anyMatch(impS -> {
          String packageNameLc = impS.getPackageName().toLowerCase();
          return impNameLc.startsWith(packageNameLc);
        });

    if (isImported) return false;

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


  public void handlePreferencesChange() {
    isContinuousCheckEnabled = JavaMode.errorCheckEnabled;
    if (isContinuousCheckEnabled) {
      Messages.log(editor.getSketch().getName() + " Error Checker enabled.");
      notifySketchChanged();
    } else {
      Messages.log(editor.getSketch().getName() + " Error Checker disabled.");
    }
  }


  public void handleHasJavaTabsChange(boolean hasJavaTabs) {
    isEnabled = !hasJavaTabs;
    if (isEnabled) {
      notifySketchChanged();
    } else {
      preprocessingTask.cancel(false);
      if (astGenerator.getGui().showUsageBinding != null) {
        astGenerator.getGui().showUsageWindow.setVisible(false);
      }
    }
  }

}
