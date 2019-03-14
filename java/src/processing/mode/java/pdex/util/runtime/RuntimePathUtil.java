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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Common convenience functions for runtime path formulation.
 */
public class RuntimePathUtil {

  /**
   * Remove invalid entries in a classpath string.
   *
   * @param classPathString The classpath to clean.
   * @return The cleaned classpath entries without invalid entries.
   */
  public static List<String> sanitizeClassPath(String classPathString) {
    // Make sure class path does not contain empty string (home dir)
    return Arrays.stream(classPathString.split(File.pathSeparator))
        .filter(p -> p != null && !p.trim().isEmpty())
        .distinct()
        .collect(Collectors.toList());
  }

}
