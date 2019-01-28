package processing.mode.java.pdex.util.runtime;

import com.google.classpath.ClassPathFactory;
import processing.app.Messages;
import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;
import processing.mode.java.pdex.PreprocessedSketch;
import processing.mode.java.pdex.util.runtime.strategy.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;


public class RuntimePathBuilder {
  private final List<CachedRuntimePathFactory> libraryDependentCaches;
  private final List<CachedRuntimePathFactory> libraryImportsDependentCaches;
  private final List<CachedRuntimePathFactory> codeFolderDependentCaches;

  private final List<RuntimePathFactoryStrategy> sketchClassPathStrategies;
  private final List<RuntimePathFactoryStrategy> searchClassPathStrategies;

  private final ClassPathFactory classPathFactory;

  public RuntimePathBuilder() {
    classPathFactory  = new ClassPathFactory();

    // Declare caches to be built
    CachedRuntimePathFactory javaRuntimePathFactory;
    CachedRuntimePathFactory modeSketchPathFactory;
    CachedRuntimePathFactory modeSearchPathFactory;
    CachedRuntimePathFactory librarySketchPathFactory;
    CachedRuntimePathFactory librarySearchPathFactory;
    CachedRuntimePathFactory coreLibraryPathFactory;
    CachedRuntimePathFactory codeFolderPathFactory;

    // Create collections
    sketchClassPathStrategies = new ArrayList<>();
    searchClassPathStrategies = new ArrayList<>();

    libraryDependentCaches = new ArrayList<>();
    libraryImportsDependentCaches = new ArrayList<>();
    codeFolderDependentCaches = new ArrayList<>();

    // Create strategies
    List<RuntimePathFactoryStrategy> runtimeStrategies = new ArrayList<>();
    runtimeStrategies.add(new JavaRuntimePathFactory());
    runtimeStrategies.add(new JavaFxRuntimePathFactory());
    javaRuntimePathFactory = new CachedRuntimePathFactory(
        new RuntimePathFactoryStrategyCollection(runtimeStrategies)
    );

    modeSketchPathFactory = new CachedRuntimePathFactory(new ModeSketchRuntimePathFactory());
    modeSearchPathFactory = new CachedRuntimePathFactory(new ModeSearchRuntimePathFactory());

    librarySketchPathFactory = new CachedRuntimePathFactory(new LibrarySketchRuntimePathFactory());
    librarySearchPathFactory = new CachedRuntimePathFactory(new LibrarySearchRuntimePathFactory());

    coreLibraryPathFactory = new CachedRuntimePathFactory(new CoreLibraryRuntimePathFactory());
    codeFolderPathFactory = new CachedRuntimePathFactory(new CodeFolderRuntimePathFactory());

    // Assign strategies to collections for producing paths
    sketchClassPathStrategies.add(javaRuntimePathFactory);
    sketchClassPathStrategies.add(modeSketchPathFactory);
    sketchClassPathStrategies.add(librarySketchPathFactory);
    sketchClassPathStrategies.add(coreLibraryPathFactory);
    sketchClassPathStrategies.add(codeFolderPathFactory);

    searchClassPathStrategies.add(javaRuntimePathFactory);
    searchClassPathStrategies.add(modeSearchPathFactory);
    searchClassPathStrategies.add(librarySearchPathFactory);
    searchClassPathStrategies.add(coreLibraryPathFactory);
    searchClassPathStrategies.add(codeFolderPathFactory);

    // Assign strategies to collections for cache invalidation
    libraryDependentCaches.add(coreLibraryPathFactory);

    libraryImportsDependentCaches.add(librarySketchPathFactory);
    libraryImportsDependentCaches.add(librarySearchPathFactory);

    codeFolderDependentCaches.add(codeFolderPathFactory);
  }

  public void markLibrariesChanged() {
    invalidateAll(libraryDependentCaches);
  }

  public void markLibraryImportsChanged() {
    invalidateAll(libraryImportsDependentCaches);
  }

  public void markCodeFolderChanged() {
    invalidateAll(codeFolderDependentCaches);
  }

  public void prepareClassPath(PreprocessedSketch.Builder result, JavaMode mode) {
    List<ImportStatement> programImports = result.programImports;
    Sketch sketch = result.sketch;

    prepareSketchClassPath(result, mode, programImports, sketch);
    prepareSearchClassPath(result, mode, programImports, sketch);
  }

  private void invalidateAll(List<CachedRuntimePathFactory> caches) {
    for (CachedRuntimePathFactory cache : caches) {
      cache.invalidateCache();
    }
  }

  private void prepareSketchClassPath(PreprocessedSketch.Builder result, JavaMode mode,
        List<ImportStatement> programImports, Sketch sketch) {

    Stream<String> sketchClassPath = sketchClassPathStrategies.stream()
        .flatMap((x) -> x.buildClasspath(mode, programImports, sketch).stream());

    String[] classPathArray = sketchClassPath.toArray(String[]::new);
    URL[] urlArray = Arrays.stream(classPathArray)
        .map(path -> {
          try {
            return Paths.get(path).toUri().toURL();
          } catch (MalformedURLException e) {
            Messages.loge("malformed URL when preparing sketch classloader", e);
            return null;
          }
        })
        .filter(Objects::nonNull)
        .toArray(URL[]::new);

    result.classLoader = new URLClassLoader(urlArray, null);
    result.classPath = classPathFactory.createFromPaths(classPathArray);
    result.classPathArray = classPathArray;
  }

  private void prepareSearchClassPath(PreprocessedSketch.Builder result, JavaMode mode,
        List<ImportStatement> programImports, Sketch sketch) {

    Stream<String> searchClassPath = searchClassPathStrategies.stream()
        .flatMap((x) -> x.buildClasspath(mode, programImports, sketch).stream());

    result.searchClassPathArray = searchClassPath.toArray(String[]::new);
  }

}
