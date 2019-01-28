package processing.mode.java.pdex.util.runtime.strategy;

import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class CachedRuntimePathFactory implements RuntimePathFactoryStrategy {

  private AtomicReference<List<String>> cachedResult;
  private RuntimePathFactoryStrategy innerStrategy;

  public CachedRuntimePathFactory(RuntimePathFactoryStrategy newInnerStrategy) {
    cachedResult = new AtomicReference<>(null);
    innerStrategy = newInnerStrategy;
  }

  public void invalidateCache() {
    cachedResult.set(null);
  }

  @Override
  public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports, Sketch sketch) {
    return cachedResult.updateAndGet((cachedValue) ->
      cachedValue == null ? innerStrategy.buildClasspath(mode, imports, sketch) : cachedValue
    );
  }

}
