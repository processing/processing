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

import processing.app.Library;
import processing.app.Sketch;
import processing.app.SketchException;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;
import processing.mode.java.pdex.util.runtime.RuntimePathUtil;

import java.io.File;
import java.util.List;
import java.util.StringJoiner;


/**
 * Runtime path factory to generate sketch classpath entries for third party libraries.
 *
 * <p>
 * Runtime path factory to generate classpath entries for third party libraries that are required
 * for running the sketch.
 * </p>
 */
public class LibrarySketchRuntimePathFactory implements RuntimePathFactoryStrategy {

  @Override
  public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports, Sketch sketch) {
    StringJoiner classPathBuilder = new StringJoiner(File.pathSeparator);

    imports.stream()
        .map(ImportStatement::getPackageName)
        .filter(pckg -> !isIgnorable(pckg))
        .map(pckg -> {
          try {
            return mode.getLibrary(pckg);
          } catch (SketchException e) {
            return null;
          }
        })
        .filter(lib -> lib != null)
        .map(Library::getClassPath)
        .forEach(cp -> classPathBuilder.add(cp));

    return RuntimePathUtil.sanitizeClassPath(classPathBuilder.toString());
  }

  /**
   * Determine if a package is ignorable.
   *
   * @param packageName The name of the package to evaluate.
   * @return True if the package is part of standard Java (like java.lang.*). False otherwise.
   */
  private boolean isIgnorable(String packageName) {
    return (packageName.startsWith("java.") || packageName.startsWith("javax."));
  }

}
