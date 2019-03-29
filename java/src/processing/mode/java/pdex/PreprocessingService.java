/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-19 The Processing Foundation

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

import java.io.File;
import java.io.StringWriter;
import java.util.*;
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

import processing.app.*;
import processing.data.IntList;
import processing.data.StringList;
import processing.mode.java.JavaEditor;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.TextTransform.OffsetMapper;
import processing.mode.java.pdex.util.ProblemFactory;
import processing.mode.java.pdex.util.runtime.RuntimePathBuilder;
import processing.mode.java.preproc.PdePreprocessIssueException;
import processing.mode.java.preproc.PdePreprocessor;
import processing.mode.java.preproc.PdePreprocessor.Mode;


/**
 * The main error checking service
 */
public class PreprocessingService {

  private final static int TIMEOUT_MILLIS = 100;

  protected final JavaEditor editor;

  protected final ASTParser parser = ASTParser.newParser(AST.JLS8);

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
            runningCallbacks.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
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
    PdePreprocessor preProcessor = editor.createPreprocessor(editor.getSketch().getName());
    if (coreAndDefaultImports == null) {
      coreAndDefaultImports = buildCoreAndDefaultImports(preProcessor);
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

    Mode sketchMode;
    try {
      sketchMode = preProcessor.write(
          new StringWriter(),
          result.scrubbedPdeCode
      ).programType;
    } catch (PdePreprocessIssueException e) {
      result.hasSyntaxErrors = true;
      result.otherProblems.add(ProblemFactory.build(e.getIssue(), tabStartsList, editor));
      return result.build();
    } catch (SketchException e) {
      sketchMode = Mode.STATIC;
    }

    // Prepare transforms to convert pde code into parsable code
    TextTransform toParsable = new TextTransform(pdeStage);
    toParsable.addAll(SourceUtils.insertImports(coreAndDefaultImports));
    toParsable.addAll(SourceUtils.insertImports(codeFolderImports));
    toParsable.addAll(SourceUtils.parseProgramImports(workBuffer, programImports));
    toParsable.addAll(SourceUtils.replaceTypeConstructors(workBuffer));
    toParsable.addAll(SourceUtils.replaceHexLiterals(workBuffer));
    toParsable.addAll(SourceUtils.wrapSketch(sketchMode, className, workBuffer.length()));

    { // Refresh sketch classloader and classpath if imports changed
      if (reloadLibraries) {
        runtimePathBuilder.markLibrariesChanged();
      }

      boolean rebuildLibraryClassPath = reloadLibraries ||
          checkIfImportsChanged(programImports, prevResult.programImports);

      if (rebuildLibraryClassPath) {
        runtimePathBuilder.markLibraryImportsChanged();
      }

      boolean rebuildClassPath = reloadCodeFolder || rebuildLibraryClassPath ||
          prevResult.classLoader == null || prevResult.classPath == null ||
          prevResult.classPathArray == null || prevResult.searchClassPathArray == null;

      if (reloadCodeFolder) {
        runtimePathBuilder.markCodeFolderChanged();
      }

      if (rebuildClassPath) {
        runtimePathBuilder.prepareClassPath(result, javaMode);
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

  private RuntimePathBuilder runtimePathBuilder = new RuntimePathBuilder();

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


  static private final Map<String, String> COMPILER_OPTIONS;
  static {
    Map<String, String> compilerOptions = new HashMap<>();

    compilerOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_11);
    compilerOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_11);
    compilerOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_11);

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
