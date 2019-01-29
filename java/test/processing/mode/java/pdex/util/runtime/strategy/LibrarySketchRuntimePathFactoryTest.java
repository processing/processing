package processing.mode.java.pdex.util.runtime.strategy;

import org.junit.Before;
import org.junit.Test;
import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;


public class LibrarySketchRuntimePathFactoryTest {

  private LibrarySketchRuntimePathFactory factory;
  private JavaMode testMode;
  private List<ImportStatement> testImports;
  private Sketch testSketch;
  private List<String> classpathEntries;

  @Before
  public void setUp() throws Exception {
    factory = new LibrarySketchRuntimePathFactory();
    testMode = RuntimePathFactoryTestUtil.createTestJavaMode();
    testImports = RuntimePathFactoryTestUtil.createTestImports();
    testSketch = RuntimePathFactoryTestUtil.createTestSketch();

    classpathEntries = factory.buildClasspath(testMode, testImports, testSketch);
  }

  @Test
  public void testBuildClasspathSize() {
    assertEquals(2, classpathEntries.size());
  }

  @Test
  public void testBuildClasspathValues() {
    assertTrue(classpathEntries.get(0).contains("library3"));
    assertTrue(classpathEntries.get(1).contains("library5"));
  }

}