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
 * Runtime path factory which generates classpath entries for JavaFX / OpenFX.
 */
public class JavaFxRuntimePathFactory implements RuntimePathFactoryStrategy {

  @Override
  public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports, Sketch sketch) {
    return Arrays.stream(RuntimeConst.JAVA_FX_JARS)
        .map(this::buildEntry)
        .collect(Collectors.toList());
  }

  /**
   * Build a single classpath entry for OpenJFX.
   *
   * @param jarName The jar name like "javafx.base.jar" for which a fully qualified entry should be
   *    created.
   * @return The fully qualified classpath entry like ".../Processing.app/Contents/PlugIns/
   *    adoptopenjdk-11.0.1.jdk/Contents/Home/lib/javafx.base.jar"
   */
  private String buildEntry(String jarName) {
    StringJoiner joiner = new StringJoiner(File.separator);
    joiner.add(System.getProperty("java.home"));
    joiner.add("lib");
    joiner.add(jarName);

    return joiner.toString();
  }

}
