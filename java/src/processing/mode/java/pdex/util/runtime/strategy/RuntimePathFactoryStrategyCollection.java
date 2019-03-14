/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 20119 The Processing Foundation

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

package processing.mode.java.pdex.util.runtime.strategy;

import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Strategy which concatenates paths generated from a collection of RuntimePathFactoryStrategies.
 */
public class RuntimePathFactoryStrategyCollection implements RuntimePathFactoryStrategy {

  private final List<RuntimePathFactoryStrategy> strategies;

  /**
   * Create a new path concatenation operation.
   *
   * @param newStrategies
   */
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
