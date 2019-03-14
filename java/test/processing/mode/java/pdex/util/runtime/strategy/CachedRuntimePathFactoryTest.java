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

import org.junit.Before;
import org.junit.Test;
import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;
import processing.mode.java.pdex.util.runtime.RuntimePathUtilTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


public class CachedRuntimePathFactoryTest {

  private CachedRuntimePathFactory cachedRuntimePathFactory;
  private JavaMode testMode;
  private List<ImportStatement> testImports;
  private Sketch testSketch;

  @Before
  public void setUp() throws Exception {
    cachedRuntimePathFactory = new CachedRuntimePathFactory(new RuntimePathFactoryStrategy() {

      private int calls = 0;

      @Override
      public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports,
            Sketch sketch) {

        String retVal = String.format("Test%d", calls);
        calls++;

        List<String> retList = new ArrayList<>();
        retList.add(retVal);
        return retList;
      }
    });

    testMode = RuntimePathFactoryTestUtil.createTestJavaMode();
    testImports = RuntimePathFactoryTestUtil.createTestImports();
    testSketch = RuntimePathFactoryTestUtil.createTestSketch();
  }

  @Test
  public void buildClasspath() {
    List<String> classpath = cachedRuntimePathFactory.buildClasspath(
        testMode,
        testImports,
        testSketch
    );

    assertEquals(1, classpath.size());
    assertEquals("Test0", classpath.get(0));
  }

  @Test
  public void invalidateCache() {
    cachedRuntimePathFactory.buildClasspath(
        testMode,
        testImports,
        testSketch
    );

    cachedRuntimePathFactory.invalidateCache();

    List<String> classpath = cachedRuntimePathFactory.buildClasspath(
        testMode,
        testImports,
        testSketch
    );

    assertEquals(1, classpath.size());
    assertEquals("Test1", classpath.get(0));
  }

}