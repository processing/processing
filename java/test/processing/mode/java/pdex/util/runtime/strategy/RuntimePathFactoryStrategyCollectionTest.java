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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


public class RuntimePathFactoryStrategyCollectionTest {

  private RuntimePathFactoryStrategyCollection factory;
  private JavaMode testMode;
  private List<ImportStatement> testImports;
  private Sketch testSketch;
  private List<String> classpath;

  @Before
  public void setUp() throws Exception {
    List<RuntimePathFactoryStrategy> innerFactories = new ArrayList<>();
    innerFactories.add(createInnerFactory("test1"));
    innerFactories.add(createInnerFactory("test2"));
    factory = new RuntimePathFactoryStrategyCollection(innerFactories);

    testMode = RuntimePathFactoryTestUtil.createTestJavaMode();
    testImports = RuntimePathFactoryTestUtil.createTestImports();
    testSketch = RuntimePathFactoryTestUtil.createTestSketch();

    classpath = factory.buildClasspath(testMode, testImports, testSketch);
  }

  private RuntimePathFactoryStrategy createInnerFactory(String retStr) {
    return (mode, imports, sketch) -> {
      List<String> retList = new ArrayList<>();
      retList.add(retStr);
      return retList;
    };
  }

  @Test
  public void testBuildClasspathLength() {
    assertEquals(2, classpath.size());
  }

  @Test
  public void testBuildClasspathContent() {
    assertEquals("test1", classpath.get(0));
    assertEquals("test2", classpath.get(1));
  }

}