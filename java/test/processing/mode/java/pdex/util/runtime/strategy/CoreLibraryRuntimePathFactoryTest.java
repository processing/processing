package processing.mode.java.pdex.util.runtime.strategy;

import org.junit.Before;
import org.junit.Test;
import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class CoreLibraryRuntimePathFactoryTest {

  private CoreLibraryRuntimePathFactory factory;
  private JavaMode testMode;
  private List<ImportStatement> testImports;
  private Sketch testSketch;
  private List<String> classPath;

  @Before
  public void setUp() throws Exception {
    factory = new CoreLibraryRuntimePathFactory();
    testMode = RuntimePathFactoryTestUtil.createTestJavaMode();
    testImports = RuntimePathFactoryTestUtil.createTestImports();
    testSketch = RuntimePathFactoryTestUtil.createTestSketch();

    classPath = factory.buildClasspath(testMode, testImports, testSketch);
  }

  @Test
  public void testBuildClasspathSize() {
    assertEquals(2, classPath.size());
  }

  @Test
  public void testBuildClasspathValue() {
    assertEquals("library1", classPath.get(0));
    assertEquals("library2", classPath.get(1));
  }


}