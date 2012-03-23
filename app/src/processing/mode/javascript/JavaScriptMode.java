package processing.mode.javascript;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import processing.app.Base;
import processing.app.Editor;
import processing.app.EditorState;
import processing.app.Mode;
import processing.app.Sketch;
import processing.app.SketchException;
import processing.app.syntax.PdeKeywords;
import processing.core.PApplet;

import processing.mode.java.JavaMode;

/**
 * JS Mode for Processing based on Processing.js. Comes with a server as
 *	replacement for the normal runner.
 */
public class JavaScriptMode extends Mode
{
	public boolean showSizeWarning = true;
	
	private JavaScriptEditor jsEditor;

	// create a new editor with the mode
	public Editor createEditor( Base base, String path, EditorState state )
	{
		jsEditor = new JavaScriptEditor( base, path, state, this );

		return jsEditor;
	}

	public Editor getEditor() {
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
	// find included example subdirs
	File[] inclExamples = examplesFolder.listFiles(new java.io.FileFilter(){
		public boolean accept (File f) {
			// only the subfolders
			return f.isDirectory();
		}
	});
	java.util.Arrays.sort(inclExamples);
	
	// add JavaMode examples as these are supposed to run in JSMode
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
		return inclExamples; // js examples only
	
	File jExamples = jMode.getContentFile("examples");
	File[] jModeExamples = new File[] {
      new File(jExamples, "Basics"),
      new File(jExamples, "Topics"),
      new File(jExamples, "3D") /*,
      new File(jExamples, "Books")*/
    };
	
	// merge them all
	File[] finalExamples = new File[inclExamples.length + jModeExamples.length];
	for ( int i = 0; i < inclExamples.length; i++ )
		finalExamples[i] = inclExamples[i];
	for ( int i = 0; i < jModeExamples.length; i++ )
		finalExamples[inclExamples.length+i] = jModeExamples[i];
	
    return finalExamples;
  }
  
  
  public String getDefaultExtension() 
  {
    return "pde";
  }

  // all file extensions it supports
  public String[] getExtensions () 
  {
    return new String[] {"pde", "js"};
  }

  public String[] getIgnorable () 
  {
    return new String[] {
	  "applet",
      "applet_js",
	  JavaScriptBuild.EXPORTED_FOLDER_NAME
    };
  }
  
  
  // ------------------------------------------------
  
  public boolean handleExport(Sketch sketch) throws IOException, SketchException
  {
    JavaScriptBuild build = new JavaScriptBuild(sketch);
    return build.export();
  }
  
  //public boolean handleExportApplet(Sketch sketch) throws SketchException, IOException { }
  
  //public boolean handleExportApplication(Sketch sketch) throws SketchException, IOException { }
}
