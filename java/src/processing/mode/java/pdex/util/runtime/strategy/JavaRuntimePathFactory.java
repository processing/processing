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

package processing.mode.java.pdex.util.runtime.strategy;

import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;
import processing.mode.java.pdex.util.runtime.RuntimeConst;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;


/**
 * Runtime path factory which enumerates the modules as part of the java runtime.
 */
public class JavaRuntimePathFactory implements RuntimePathFactoryStrategy {

  @Override
  public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports, Sketch sketch) {
    return Arrays.stream(RuntimeConst.STANDARD_MODULES)
        .map(this::buildForModule)
        .collect(Collectors.toList());
  }

  /**
   * Build a classpath entry for a module.
   *
   * @param moduleName The name of the module like "java.base.jmod".
   * @return The fully qualified classpath entry like ".../Processing.app/Contents/PlugIns/
   *    *    adoptopenjdk-11.0.1.jdk/Contents/Home/jmods/java.base.jmod"
   */
  private String buildForModule(String moduleName) {
    StringJoiner jmodPathJoiner = new StringJoiner(File.separator);
    jmodPathJoiner.add(System.getProperty("java.home"));
    jmodPathJoiner.add("jmods");
    jmodPathJoiner.add(moduleName);
    return jmodPathJoiner.toString();
  }

}
