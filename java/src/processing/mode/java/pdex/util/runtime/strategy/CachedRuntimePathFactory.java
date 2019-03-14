/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2019 The Processing Foundation

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
import java.util.concurrent.atomic.AtomicReference;


/**
 * Runtime path factory which caches the results of another runtime path factory.
 *
 * <p>
 * Runtime path factory which decorates another {RuntimePathFactoryStrategy} that caches the
 * results of another runtime path factory. This is a lazy cached getter so the value will not be
 * resolved until it is requested.
 * </p>
 */
public class CachedRuntimePathFactory implements RuntimePathFactoryStrategy {

  private AtomicReference<List<String>> cachedResult;
  private RuntimePathFactoryStrategy innerStrategy;

  /**
   * Create a new cache around {RuntimePathFactoryStrategy}.
   *
   * @param newInnerStrategy The strategy to cache.
   */
  public CachedRuntimePathFactory(RuntimePathFactoryStrategy newInnerStrategy) {
    cachedResult = new AtomicReference<>(null);
    innerStrategy = newInnerStrategy;
  }

  /**
   * Invalidate the cached path so that, when requested next time, it will be rebuilt from scratch.
   */
  public void invalidateCache() {
    cachedResult.set(null);
  }

  /**
   * Return the cached classpath or, if not cached, build a classpath using the inner strategy.
   *
   * <p>
   * Return the cached classpath or, if not cached, build a classpath using the inner strategy.
   * Note that this getter will not check to see if mode, imports, or sketch have changed. If a
   * cached value is available, it will be returned without examining the identity of the
   * parameters.
   * </p>
   *
   * @param mode The {JavaMode} for which the classpath should be built.
   * @param imports The sketch (user) imports.
   * @param sketch The sketch for which a classpath is to be returned.
   * @return Newly generated classpath.
   */
  @Override
  public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports, Sketch sketch) {
    return cachedResult.updateAndGet((cachedValue) ->
      cachedValue == null ? innerStrategy.buildClasspath(mode, imports, sketch) : cachedValue
    );
  }

}
