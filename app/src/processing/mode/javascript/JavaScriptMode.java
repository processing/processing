package processing.mode.javascript;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import processing.app.Base;
import processing.app.Editor;
import processing.app.Mode;
import processing.app.Sketch;
import processing.app.syntax.PdeKeywords;
import processing.core.PApplet;

/**
 * JS Mode is very simple. Since P.js is dependent on a browser there is
 * no runner, just an export so that users can debug in the browser of their
 * choice. 
 */
public class JavaScriptMode extends Mode {
  

  // create a new editor with the mode
  public Editor createEditor(Base base, String path, int[] location) {
    return new JavaScriptEditor(base, path, location, this);
  }

  
  public JavaScriptMode(Base base, File folder) {
    super(base, folder);
    
    try {
      loadKeywords();
    } catch (IOException e) {
      Base.showError("Problem loading keywords",
                     "Could not load keywords.txt, please re-install Processing.", e);
    }
  }

  
  protected void loadKeywords() throws IOException {
    File file = new File(folder, "keywords.txt");
    BufferedReader reader = PApplet.createReader(file);

    tokenMarker = new PdeKeywords();
    keywordToReference = new HashMap<String, String>();

    String line = null;
    while ((line = reader.readLine()) != null) {
      String[] pieces = PApplet.trim(PApplet.split(line, '\t'));
      if (pieces.length >= 2) {
        String keyword = pieces[0];
        String coloring = pieces[1];

        if (coloring.length() > 0) {
          tokenMarker.addColoring(keyword, coloring);
        }
        if (pieces.length == 3) {
          String htmlFilename = pieces[2];
          if (htmlFilename.length() > 0) {
            keywordToReference.put(keyword, htmlFilename);
          }
        }
      }
    }
  }

  
  // pretty printable name of the mode
  public String getTitle() {
    return "JavaScript";
  }

  
  // public EditorToolbar createToolbar(Editor editor) { }

  
  // public Formatter createFormatter() { }

  
  //  public Editor createEditor(Base ibase, String path, int[] location) { }

  
  // ------------------------------------------------

  
  //TODO Add examples
  protected File[] getExampleCategoryFolders() {
    return new File[] {};
    /*
    return new File[] { 
      new File(examplesFolder, "Basics"),
      new File(examplesFolder, "Topics"),
      new File(examplesFolder, "3D"),
      new File(examplesFolder, "Books")
    };
      */
  }
  
  
  public String getDefaultExtension() {
    return "pde";
  }

  
  // all file extensions it supports
  public String[] getExtensions() {
    return new String[] {"pde", "pjs"};
  }

  
  public String[] getIgnorable() {
    return new String[] {
      "applet_js" // not sure what color to paint this bike shed
    };
  }
  
  
  // ------------------------------------------------
  
  
  public boolean handleExport(Sketch sketch) throws IOException {
    JavaScriptBuild build = new JavaScriptBuild(sketch);
    return build.export();
  }
  
  
  //public boolean handleExportApplet(Sketch sketch) throws SketchException, IOException { }
  
  
  //public boolean handleExportApplication(Sketch sketch) throws SketchException, IOException { }
}
