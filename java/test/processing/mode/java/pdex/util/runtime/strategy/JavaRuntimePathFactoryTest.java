package processing.mode.java.pdex.util.runtime.strategy;

import org.junit.Before;
import org.junit.Test;
import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;
import processing.mode.java.pdex.util.runtime.RuntimeConst;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;


public class JavaRuntimePathFactoryTest {

  private JavaRuntimePathFactory factory;
  private JavaMode testMode;
  private List<ImportStatement> testImports;
  private Sketch testSketch;

  private List<String> classpath;

  @Before
  public void setUp() throws Exception {
    factory = new JavaRuntimePathFactory();
    testMode = RuntimePathFactoryTestUtil.createTestJavaMode();
    testImports = RuntimePathFactoryTestUtil.createTestImports();
    testSketch = RuntimePathFactoryTestUtil.createTestSketch();

    classpath = factory.buildClasspath(testMode, testImports, testSketch);
  }

  @Test
  public void testBuildClasspathSize() {
    assertEquals(RuntimeConst.STANDARD_MODULES.length, classpath.size());
  }

  @Test
  public void testBuildClasspathValues() {
    boolean foundTarget = false;
    for (String entry : classpath) {
      boolean justFound = entry.contains("java.base.jmod") && entry.contains("jmods");
      foundTarget = foundTarget || justFound;
    }

    assertTrue(foundTarget);
  }

}