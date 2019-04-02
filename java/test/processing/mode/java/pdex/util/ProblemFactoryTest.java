package processing.mode.java.pdex.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import processing.app.Problem;
import processing.app.ui.Editor;
import processing.mode.java.preproc.issue.PdePreprocessIssue;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ProblemFactoryTest {

  private PdePreprocessIssue pdePreprocessIssue;
  private List<Integer> tabStarts;
  private Editor editor;

  @Before
  public void setUp() {
    pdePreprocessIssue = new PdePreprocessIssue(8, 2, "test");

    tabStarts = new ArrayList<>();
    tabStarts.add(5);

    editor = Mockito.mock(Editor.class);
    Mockito.when(editor.getLineStartOffset(3)).thenReturn(10);
  }

  @Test
  public void buildWithEditor() {
    Problem problem = ProblemFactory.build(pdePreprocessIssue, tabStarts, editor);

    Assert.assertEquals(3, problem.getLineNumber());
    Assert.assertEquals("test", problem.getMessage());
    Assert.assertEquals(10, problem.getStartOffset());
    Assert.assertEquals(12, problem.getStopOffset());
  }

  @Test
  public void buildWithoutEditor() {
    Problem problem = ProblemFactory.build(pdePreprocessIssue, tabStarts);

    Assert.assertEquals(3, problem.getLineNumber());
    Assert.assertEquals("test", problem.getMessage());
  }

}