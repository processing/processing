package processing.mode.java.pdex.util.runtime.strategy;

import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;

import java.util.List;
import java.util.stream.Collectors;

public class RuntimePathFactoryStrategyCollection implements RuntimePathFactoryStrategy {

  private final List<RuntimePathFactoryStrategy> strategies;

  public RuntimePathFactoryStrategyCollection(List<RuntimePathFactoryStrategy> newStrategies) {
    strategies = newStrategies;
  }

  @Override
  public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports, Sketch sketch) {
    return strategies.stream()
        .flatMap((strategy) -> strategy.buildClasspath(mode, imports, sketch).stream())
        .collect(Collectors.toList());
  }

}
