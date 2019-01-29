package processing.mode.java.pdex.util.runtime.strategy;

import org.junit.Before;
import org.junit.Test;
import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class CodeFolderRuntimePathFactoryTest {

  private CodeFolderRuntimePathFactory factory;
  private JavaMode testMode;
  private List<ImportStatement> testImports;
  private Sketch testSketch;

  private List<String> classpath;

  @Before
  public void setUp() throws Exception {
    factory = new CodeFolderRuntimePathFactory();
    testMode = RuntimePathFactoryTestUtil.createTestJavaMode();
    testImports = RuntimePathFactoryTestUtil.createTestImports();
    testSketch = RuntimePathFactoryTestUtil.createTestSketch();

    classpath = factory.buildClasspath(testMode, testImports, testSketch);
  }

  @Test
  public void testBuildClasspathSize() {
    assertEquals(2, classpath.size());
  }

  @Test
  public void testBuildClasspathValues() {
    assertEquals("testdir" + File.separator + "file1.jar", classpath.get(0));
    assertEquals("testdir" + File.separator + "file3.zip", classpath.get(1));
  }

}