package processing.mode.java.pdex.util.runtime.strategy;

import org.junit.Before;
import org.junit.Test;
import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;

import java.util.List;

import static org.junit.Assert.*;

public class ModeSketchRuntimePathFactoryTest {

  private ModeSketchRuntimePathFactory factory;
  private JavaMode testMode;
  private List<ImportStatement> testImports;
  private Sketch testSketch;
  private List<String> classpath;

  @Before
  public void setUp() throws Exception {
    factory = new ModeSketchRuntimePathFactory();
    testMode = RuntimePathFactoryTestUtil.createTestJavaMode();
    testImports = RuntimePathFactoryTestUtil.createTestImports();
    testSketch = RuntimePathFactoryTestUtil.createTestSketch();

    classpath = factory.buildClasspath(testMode, testImports, testSketch);
  }

  @Test
  public void buildClasspathLength() {
    assertEquals(3, classpath.size());
  }

  @Test
  public void buildClasspathValues() {
    assertEquals("library6", classpath.get(0));
    assertEquals("javax.library7", classpath.get(1));
    assertEquals("library8", classpath.get(2));
  }

}