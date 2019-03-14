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

package processing.mode.java.pdex.util.runtime;

import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.StringJoiner;

import static org.junit.Assert.*;


public class RuntimePathUtilTest {

  @Test
  public void sanitizeClassPath() {
    StringJoiner testStrJoiner = new StringJoiner(File.pathSeparator);
    testStrJoiner.add("test1");
    testStrJoiner.add("");
    testStrJoiner.add("test2");

    List<String> classPath = RuntimePathUtil.sanitizeClassPath(testStrJoiner.toString());
    assertEquals(2, classPath.size());
    assertEquals("test1", classPath.get(0));
    assertEquals("test2", classPath.get(1));
  }

  @Test
  public void sanitizeClassPathNoDuplicate() {
    StringJoiner testStrJoiner = new StringJoiner(File.pathSeparator);
    testStrJoiner.add("test1");
    testStrJoiner.add("");
    testStrJoiner.add("test2");
    testStrJoiner.add("test2");

    List<String> classPath = RuntimePathUtil.sanitizeClassPath(testStrJoiner.toString());
    assertEquals(2, classPath.size());
    assertEquals("test1", classPath.get(0));
    assertEquals("test2", classPath.get(1));
  }

}