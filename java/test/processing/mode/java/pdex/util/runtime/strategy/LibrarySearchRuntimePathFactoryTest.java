package processing.mode.java.pdex.util.runtime.strategy;

import org.junit.Before;
import org.junit.Test;
import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class LibrarySearchRuntimePathFactoryTest {

  private LibrarySearchRuntimePathFactory factory;
  private JavaMode testMode;
  private List<ImportStatement> testImports;
  private Sketch testSketch;

  private List<String> classpath;

  @Before
  public void setUp() throws Exception {
    factory = new LibrarySearchRuntimePathFactory();
    testMode = RuntimePathFactoryTestUtil.createTestJavaMode();
    testImports = RuntimePathFactoryTestUtil.createTestImports();
    testSketch = RuntimePathFactoryTestUtil.createTestSketch();

    classpath = factory.buildClasspath(testMode, testImports, testSketch);
  }

  @Test
  public void testBuildClasspathSize() {
    assertEquals(3, classpath.size());
  }

  @Test
  public void testBuildClasspathValues() {
    assertTrue(classpath.get(0).contains("library3"));
    assertTrue(classpath.get(1).contains("java.library4"));
    assertTrue(classpath.get(2).contains("library5"));
  }

}