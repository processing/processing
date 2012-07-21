package processing.app.tools;

import java.io.IOException;

import processing.app.Editor;


/**
 * Launch the Movie Maker tool in another frame. On Mac OS X, the Quaqua
 * library causes a conflict with the layout manager used, so instead we'll
 * just launch out of process rather than futz with the L&F. Probably more
 * technically correct in terms of licensing for that code anyway.
 * @author fry
 */
public class MovieMaker implements Tool {

  public String getMenuTitle() {
    return "Movie Maker";
  }


  public void run() {
    String classPath =
      getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    System.out.println("cp is " + classPath);
    try {
      Runtime.getRuntime().exec(new String[] {
        "java", "-cp", classPath, "processing.app.tools.MovieMakerFrame"
      });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public void init(Editor editor) {
  }
}