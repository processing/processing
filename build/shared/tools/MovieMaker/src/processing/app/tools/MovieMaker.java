package processing.app.tools;

import java.io.IOException;

import processing.app.Base;
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
    if (Base.isMacOS()) {
      // For OS X, run out of process, so that Quaqua doesn't hose the layout.
      // http://code.google.com/p/processing/issues/detail?id=836
      String classPath =
          getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
      try {
        Runtime.getRuntime().exec(new String[] {
          "java", "-cp", classPath, "processing.app.tools.MovieMakerFrame"
        });
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      System.out.println("null frame, yeah baby");
      MovieMakerFrame.main(null);
    }
  }


  public void init(Editor editor) {
  }
}