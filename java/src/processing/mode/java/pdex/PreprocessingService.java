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

import com.google.classpath.ClassPathFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.swing.text.BadLocationException;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import processing.app.Library;
import processing.app.Messages;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.SketchException;
import processing.app.Util;
import processing.data.IntList;
import processing.data.StringList;
import processing.mode.java.JavaEditor;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.TextTransform.OffsetMapper;
import processing.mode.java.preproc.PdePreprocessor;
import processing.mode.java.preproc.PdePreprocessor.Mode;


/**
 * The main error checking service
 */
public class PreprocessingService {

  protected final JavaEditor editor;

  protected final ASTParser parser = ASTParser.newParser(AST.JLS8);

  private final ClassPathFactory classPathFactory = new ClassPathFactory();

  private final Thread preprocessingThread;
  private final BlockingQueue<Boolean> requestQueue = new ArrayBlockingQueue<>(1);

  private final Object requestLock = new Object();

  private final AtomicBoolean codeFolderChanged = new AtomicBoolean(true);
  private final AtomicBoolean librariesChanged = new AtomicBoolean(true);

  private volatile boolean running;
  private CompletableFuture<PreprocessedSketch> preprocessingTask = new CompletableFuture<>();

  private CompletableFuture<?> lastCallback =
      new CompletableFuture<Object>() {{
        complete(null); // initialization block
      }};

  private volatile boolean isEnabled = true;


  public PreprocessingService(JavaEditor editor) {
    this.editor = editor;
    isEnabled = !editor.hasJavaTabs();

    // Register listeners for first run
    whenDone(this::fireListeners);

    preprocessingThread = new Thread(this::mainLoop, "ECS");
    preprocessingThread.start();
  }


  private void mainLoop() {
    running = true;
    PreprocessedSketch prevResult = null;
    CompletableFuture<?> runningCallbacks = null;
    Messages.log("PPS: Hi!");
    while (running) {
      try {
        try {
          requestQueue.take(); // blocking until requested
        } catch (InterruptedException e) {
          running = false;
          break;
        }

        Messages.log("PPS: Starting");

        prevResult = preprocessSketch(prevResult);

        // Wait until callbacks finish before firing new wave
        // If new request arrives while waiting, break out and start preprocessing
        while (requestQueue.isEmpty() && runningCallbacks != null) {
          try {
            runningCallbacks.get(10, TimeUnit.MILLISECONDS);
            runningCallbacks = null;
          } catch (TimeoutException e) { }
        }

        synchronized (requestLock) {
          if (requestQueue.isEmpty()) {
            runningCallbacks = lastCallback;
            Messages.log("PPS: Done");
            preprocessingTask.complete(prevResult);
          }
        }
      } catch (Exception e) {
        Messages.loge("problem in preprocessor service loop", e);
      }
    }
    Messages.log("PPS: Bye!");
  }


  public void dispose() {
    cancel();
    running = false;
    preprocessingThread.interrupt();
  }


  public void cancel() {
    requestQueue.clear();
  }


  public void notifySketchChanged() {
    if (!isEnabled) return;
    synchronized (requestLock) {
      if (preprocessingTask.isDone()) {
        preprocessingTask = new CompletableFuture<>();
        // Register callback which executes all listeners
        whenDone(this::fireListeners);
      }
      requestQueue.offer(Boolean.TRUE);
    }
  }


  public void notifyLibrariesChanged() {
    Messages.log("PPS: notified libraries changed");
    librariesChanged.set(true);
    notifySketchChanged();
  }


  public void notifyCodeFolderChanged() {
    Messages.log("PPS: snotified code folder changed");
    codeFolderChanged.set(true);
    notifySketchChanged();
  }


  private CompletableFuture<?> registerCallback(Consumer<PreprocessedSketch> callback) {
    synchronized (requestLock) {
      lastCallback = preprocessingTask
          // Run callback after both preprocessing task and previous callback
          .thenAcceptBothAsync(lastCallback, (ps, a) -> callback.accept(ps))
          // Make sure exception in callback won't cancel whole callback chain
          .handleAsync((res, e) -> {
            if (e != null) Messages.loge("PPS: exception in callback", e);
            return res;
          });
      return lastCallback;
    }
  }


  public void whenDone(Consumer<PreprocessedSketch> callback) {
    if (!isEnabled) return;
    registerCallback(callback);
  }


  public void whenDoneBlocking(Consumer<PreprocessedSketch> callback) {
    if (!isEnabled) return;
    try {
      registerCallback(callback).get(3000, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      // Don't care
    }
  }



  /// LISTENERS ----------------------------------------------------------------


  private Set<Consumer<PreprocessedSketch>> listeners = new CopyOnWriteArraySet<>();


  public void registerListener(Consumer<PreprocessedSketch> listener) {
    if (listener != null) listeners.add(listener);
  }


  public void unregisterListener(Consumer<PreprocessedSketch> listener) {
    listeners.remove(listener);
  }


  private void fireListeners(PreprocessedSketch ps) {
    for (Consumer<PreprocessedSketch> listener : listeners) {
      try {
        listener.accept(ps);
      } catch (Exception e) {
        Messages.loge("error when firing preprocessing listener", e);
      }
    }
  }


  /// --------------------------------------------------------------------------


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
        if (sc.getDocument() != null) {
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

    result.scrubbedPdeCode = workBuffer.toString();

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
          prevResult.classPathArray == null || prevResult.searchClassPathArray == null;

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

          result.searchClassPathArray = searchClassPath.stream().toArray(String[]::new);
        }
      } else {
        result.classLoader = prevResult.classLoader;
        result.classPath = prevResult.classPath;
        result.searchClassPathArray = prevResult.searchClassPathArray;
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
    toCompilable.addAll(SourceUtils.preprocessAST(parsableCU));

    // Transform code to compilable state
    String compilableStage = toCompilable.apply();
    OffsetMapper compilableMapper = toCompilable.getMapper();
    char[] compilableStageChars = compilableStage.toCharArray();

    // Create compilable AST to get syntax problems
    CompilationUnit compilableCU =
        makeAST(parser, compilableStageChars, COMPILER_OPTIONS);

    // Get syntax problems from compilable AST
    result.hasSyntaxErrors |= Arrays.stream(compilableCU.getProblems())
        .anyMatch(IProblem::isError);

    // Generate bindings after getting problems - avoids
    // 'missing type' errors when there are syntax problems
    CompilationUnit bindingsCU =
        makeASTWithBindings(parser, compilableStageChars, COMPILER_OPTIONS,
                            className, result.classPathArray);

    // Get compilation problems
    List<IProblem> bindingsProblems = Arrays.asList(bindingsCU.getProblems());
    result.hasCompilationErrors = bindingsProblems.stream()
        .anyMatch(IProblem::isError);

    // Update builder
    result.offsetMapper = parsableMapper.thenMapping(compilableMapper);
    result.javaCode = compilableStage;
    result.compilationUnit = bindingsCU;

    // Build it
    return result.build();
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


  private static boolean checkIfImportsChanged(List<ImportStatement> prevImports,
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



  /// CLASSPATHS ---------------------------------------------------------------


  private List<String> javaRuntimeClassPath;

  private List<String> sketchModeClassPath;
  private List<String> searchModeClassPath;

  private List<String> coreLibraryClassPath;

  private List<String> codeFolderClassPath;

  private List<String> sketchLibraryClassPath;
  private List<String> searchLibraryClassPath;


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


  static private List<String> buildSketchLibraryClassPath(JavaMode mode,
                                                          List<ImportStatement> programImports) {
    StringBuilder classPath = new StringBuilder();


    // init map by setting package imports as keys and libraries to null
    HashMap<String, Library> pkg_libs = new HashMap<String, Library>();
    for(ImportStatement pgk : programImports){
      pkg_libs.put(pgk.getPackageName(), null);
    }
    
    // for each package import, find a library that makes sense
    // no more "duplicate library" conflicts"
    mode.getLibraries(pkg_libs);
    
    // checkout the generated mapping
    // some imports are still mapped to null, which is the case if not
    // a single library was found
    Iterator<Entry<String, Library>> iter = pkg_libs.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<String, Library> pkg_lib = iter.next();
      
      Library lib = pkg_lib.getValue();
      if(lib != null){
        classPath.append(File.pathSeparator).append(lib.getClassPath());
      }
    }

//    programImports.stream()
//        .map(ImportStatement::getPackageName)
//        .filter(pckg -> !ignorableImport(pckg))
//        .map(pckg -> mode.getLibrary(pckg))
//        .filter(lib -> lib != null)
//        .map(Library::getClassPath)
//        .forEach(cp -> classPath.append(File.pathSeparator).append(cp));

    return sanitizeClassPath(classPath.toString());
  }


  static private List<String> buildJavaRuntimeClassPath() {
    StringBuilder classPath = new StringBuilder();

    { // Java runtime
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
    }

    { // JavaFX runtime
      String jfxrtPath = System.getProperty("java.home") +
          File.separator + "lib" + File.separator + "ext" + File.separator + "jfxrt.jar";
      if (new File(jfxrtPath).exists()) {
        classPath.append(File.pathSeparator).append(jfxrtPath);
      } else {
        jfxrtPath = System.getProperty("java.home") + File.separator + "jre" +
            File.separator + "lib" + File.separator + "ext" + File.separator + "jfxrt.jar";
        if (new File(jfxrtPath).exists()) {
          classPath.append(File.pathSeparator).append(jfxrtPath);
        }
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



  private static CompilationUnit makeAST(ASTParser parser,
                                           char[] source,
                                           Map<String, String> options) {
    parser.setSource(source);
    parser.setKind(ASTParser.K_COMPILATION_UNIT);
    parser.setCompilerOptions(options);
    parser.setStatementsRecovery(true);

    return (CompilationUnit) parser.createAST(null);
  }


  private static CompilationUnit makeASTWithBindings(ASTParser parser,
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
  static private boolean ignorableImport(String packageName) {
    return (packageName.startsWith("java.") ||
            packageName.startsWith("javax."));
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


  public void handleHasJavaTabsChange(boolean hasJavaTabs) {
    isEnabled = !hasJavaTabs;
    if (isEnabled) {
      notifySketchChanged();
    } else {
      preprocessingTask.cancel(false);
    }
  }

}
