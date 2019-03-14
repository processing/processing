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


/**
 * Builder which generates runtime paths using a series of caches.
 *
 * <p>
 * Builder which helps generate classpath (and module path) entries for sketches using stateful
 * and individually invalidate-able caches to prevent duplicate work.
 * </p>
 */
public class RuntimePathBuilder {
  private final List<CachedRuntimePathFactory> libraryDependentCaches;
  private final List<CachedRuntimePathFactory> libraryImportsDependentCaches;
  private final List<CachedRuntimePathFactory> codeFolderDependentCaches;

  private final List<RuntimePathFactoryStrategy> sketchClassPathStrategies;
  private final List<RuntimePathFactoryStrategy> searchClassPathStrategies;

  private final ClassPathFactory classPathFactory;

  /**
   * Create a new runtime path builder with empty caches.
   */
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

  /**
   * Invalidate all of the runtime path caches associated with sketch libraries.
   */
  public void markLibrariesChanged() {
    invalidateAll(libraryDependentCaches);
  }

  /**
   * Invalidate all of the runtime path caches associated with sketch library imports.
   */
  public void markLibraryImportsChanged() {
    invalidateAll(libraryImportsDependentCaches);
  }

  /**
   * Invalidate all of the runtime path caches associated with the code folder having changed.
   */
  public void markCodeFolderChanged() {
    invalidateAll(codeFolderDependentCaches);
  }

  /**
   * Generate a classpath and inject it into a {PreprocessedSketch.Builder}.
   *
   * @param result The {PreprocessedSketch.Builder} into which the classpath should be inserted.
   * @param mode The {JavaMode} for which the classpath should be generated.
   */
  public void prepareClassPath(PreprocessedSketch.Builder result, JavaMode mode) {
    List<ImportStatement> programImports = result.programImports;
    Sketch sketch = result.sketch;

    prepareSketchClassPath(result, mode, programImports, sketch);
    prepareSearchClassPath(result, mode, programImports, sketch);
  }

  /**
   * Invalidate all of the caches in a provided collection.
   *
   * @param caches The caches to invalidate so that, when their value is requested again, the value
   *    is generated again.
   */
  private void invalidateAll(List<CachedRuntimePathFactory> caches) {
    for (CachedRuntimePathFactory cache : caches) {
      cache.invalidateCache();
    }
  }

  /**
   * Prepare the classpath required for the sketch's execution.
   *
   * @param result The PreprocessedSketch builder into which the classpath and class loader should
   *    be injected.
   * @param mode The JavaMode for which a sketch classpath should be generated.
   * @param programImports The imports listed by the sketch (user imports).
   * @param sketch The sketch for which the classpath is being generated.
   */
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

  /**
   * Prepare the classpath for searching in case of import suggestions.
   *
   * @param result The PreprocessedSketch builder into which the search classpath should be
   *    injected.
   * @param mode The JavaMode for which a sketch classpath should be generated.
   * @param programImports The imports listed by the sketch (user imports).
   * @param sketch The sketch for which the classpath is being generated.
   */
  private void prepareSearchClassPath(PreprocessedSketch.Builder result, JavaMode mode,
        List<ImportStatement> programImports, Sketch sketch) {

    Stream<String> searchClassPath = searchClassPathStrategies.stream()
        .flatMap((x) -> x.buildClasspath(mode, programImports, sketch).stream());

    result.searchClassPathArray = searchClassPath.toArray(String[]::new);
  }

}
