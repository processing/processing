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


/**
 * Strategy which generates part of the classpath and/or module path.
 *
 * <p>
 * Strategy for factories each of which generate part of the classpath and/or module path required
 * by a sketch through user supplied requirements, mode (as in JavaMode) requirements, or transitive
 * requirements imposed by third party libraries.
 * </p>
 */
public interface RuntimePathFactoryStrategy {

  /**
   * Create classpath and/or module path entries.
   *
   * @param mode The mode engaged by the user like JavaMode.
   * @param programImports The program imports imposed by the user within their sketch.
   * @param sketch The sketch provided by the user.
   * @return List of classpath and/or module path entries.
   */
  List<String> buildClasspath(JavaMode mode, List<ImportStatement> programImports, Sketch sketch);

}
