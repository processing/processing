package processing.mode.javascript;

import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

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

import processing.mode.java.JavaMode;

/**
 * JS Mode for Processing based on Processing.js. Comes with a server as
 *	replacement for the normal runner.
 */
public class JavaScriptMode extends Mode
{
	private JavaScriptEditor jsEditor;

  // create a new editor with the mode
  public Editor createEditor( Base base, String path, int[] location )
  {
	jsEditor = new JavaScriptEditor( base, path, location, this );
    return jsEditor;
  }

  public JavaScriptMode( Base base, File folder )
  {
    super(base, folder);
    
    try {
      loadKeywords();
    } catch (IOException e) {
      Base.showError("Problem loading keywords",
                     "Could not load keywords.txt, please re-install Processing.", e);
    }
  }

  protected void loadKeywords() throws IOException
  {
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
  public String getTitle()
  {
    return "JavaScript";
  }

  // public EditorToolbar createToolbar(Editor editor) { }

  // public Formatter createFormatter() { }

  //  public Editor createEditor(Base ibase, String path, int[] location) { }
  
  // ------------------------------------------------

  /**
   * For now just add JavaMode examples.
   */
  public File[] getExampleCategoryFolders()
  {
	JavaMode jMode = null;
	for ( Mode m : base.getModeList() )
	{
		if ( m.getClass() == JavaMode.class )
		{
			jMode = (JavaMode)m;
			break;
		}
	}
	if ( jMode == null )
		return new File[0];
	
	File jExamples = jMode.getContentFile("examples");
    return new File[] { 
      new File(jExamples, "Basics"),
      new File(jExamples, "Topics"),
      new File(jExamples, "3D"),
      new File(jExamples, "Books")
    };
  }
  
  
  public String getDefaultExtension() 
  {
    return "pde";
  }

  // all file extensions it supports
  public String[] getExtensions() 
  {
    return new String[] {"pde", "js"};
  }

  public String[] getIgnorable() 
  {
    return new String[] {
	  "applet",
      "applet_js", 
	  "template_js"
    };
  }
  
  
  // ------------------------------------------------
  
  public boolean handleExport(Sketch sketch) throws IOException 
  {
    JavaScriptBuild build = new JavaScriptBuild(sketch);
    return build.export();
  }
  
  //public boolean handleExportApplet(Sketch sketch) throws SketchException, IOException { }
  
  //public boolean handleExportApplication(Sketch sketch) throws SketchException, IOException { }
}
