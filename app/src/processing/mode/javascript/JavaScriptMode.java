package processing.mode.javascript;

import java.io.File;
import java.io.IOException;

import processing.app.*;
import processing.mode.java.JavaMode;

import javax.swing.*;
import javax.swing.tree.*;

/**
 *	JS Mode for Processing based on Processing.js. Comes with a server as
 *	replacement for the normal runner.
 */
public class JavaScriptMode extends Mode
{
	// show that warning only once per run-cycle as we are 
	// continously exporting behind the scenes at every save
	public boolean showSizeWarning = true;
	
	private JavaScriptEditor jsEditor;
	private JavaMode defaultJavaMode;
	
	/**
	 *	Constructor
	 *
	 *	@param base the Processing editor base
	 *	@param folder the folder that this mode is started from
	 */
	public JavaScriptMode ( Base base, File folder )
	{
		super(base, folder);
		
//		try {
//			loadKeywords(); // in JavaMode, sets tokenMarker
//			loadAdditionalKeywords( 
//				new File(Base.getContentFile("modes/java"), "keywords.txt" ),
//				tokenMarker
//			);
//		} 
//		catch ( IOException e ) 
//		{
//			Base.showError( "Problem loading keywords",
//			                "Could not load keywords.txt, please re-install Processing.", e);
//		}
	}

	/**
	 *	Called to create the actual editor when needed (once per Sketch)
	 */
	public Editor createEditor( Base base, String path, EditorState state )
	{
		jsEditor = new JavaScriptEditor( base, path, state, this );

		return jsEditor;
	}

	/**
	 *	Called from Base to get the Editor for this mode.
	 */
	public Editor getEditor () 
	{
		return jsEditor;
	}

	public JavaMode getDefaultMode ()
	{
		if ( defaultJavaMode == null ) {
			for ( Mode m : base.getModeList() )
			{
				if ( m.getClass() == JavaMode.class )
				{
					defaultJavaMode = (JavaMode)m;
					break;
				}
			}
		}
		return defaultJavaMode;
	}

//	/**
//	 *	Loads default Java keywords, JS keywords 
//	 *	were already loaded in constructor.
//	 */
//	protected void loadAdditionalKeywords ( File keywords, PdeKeywords tokenMarker ) throws IOException
//	{
//		if ( keywordToReference == null )
//			keywordToReference = new HashMap<String, String>();
//		
//		BufferedReader reader = PApplet.createReader( keywords );
//		String line = null;
//		while ((line = reader.readLine()) != null) 
//		{
//			String[] pieces = PApplet.trim(PApplet.split(line, '\t'));
//		    if (pieces.length >= 2) 
//			{
//		    	String keyword = pieces[0];
//				String coloring = pieces[1];
//				if (coloring.length() > 0) {
//		        	tokenMarker.addColoring(keyword, coloring);
//		      	}
//		      	if (pieces.length == 3) {
//		        	String htmlFilename = pieces[2];
//		        	if (htmlFilename.length() > 0) {
//		          		keywordToReference.put(keyword, htmlFilename);
//		        	}
//		      	}
//			}
//		}
//	}
//	
//	/**
//	 * load the keywords from file, copied from JavaMode.java
//	 */
//	protected void loadKeywords() throws IOException 
//	{
//	    File file = new File(folder, "keywords.txt");
//	    BufferedReader reader = PApplet.createReader(file);
//
//	    tokenMarker = new PdeKeywords();
//	    keywordToReference = new HashMap<String, String>();
//
//	    String line = null;
//    	while ((line = reader.readLine()) != null) {
//      		String[] pieces = PApplet.trim(PApplet.split(line, '\t'));
//      		if (pieces.length >= 2) {
//        		String keyword = pieces[0];
//        		String coloring = pieces[1];
//
//        		if (coloring.length() > 0) {
//          			tokenMarker.addColoring(keyword, coloring);
//        		}
//        		if (pieces.length == 3) {
//          			String htmlFilename = pieces[2];
//          			if (htmlFilename.length() > 0) {
//            			keywordToReference.put(keyword, htmlFilename);
//          			}
//        		}
//      		}
//    	}
//  	}
	
  public File[] getKeywordFiles() {
    return new File[] { 
      Base.getContentFile("modes/java/keywords.txt"),
      new File(folder, "keywords.txt")
    };
  }

  
//	/**
//	 *	Override getTokenMarker in Mode
//	 */
//	public TokenMarker getTokenMarker () 
//	{
//		if ( tokenMarker == null )
//			tokenMarker = new PdeKeywords();
//		return tokenMarker;
//	}

  
	/**
	 *	Return pretty title of this mode for menu listing and such
	 */
	public String getTitle()
	{
		return "JavaScript";
	}

  // public EditorToolbar createToolbar(Editor editor) { }

  // public Formatter createFormatter() { }

  //  public Editor createEditor(Base ibase, String path, int[] location) { }
  
  // ------------------------------------------------

  /**
   *	Fetch and return examples from JS and Java mode
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
	JavaMode jMode = getDefaultMode();
	if ( jMode == null )
		return inclExamples; // js examples only
	
	File jExamples = jMode.getContentFile("examples");
	File[] jModeExamples = new File[] {
      new File(jExamples, "Basics"),
      //new File(jExamples, "Topics"),
      //new File(jExamples, "3D") ,
      //new File(jExamples, "Books")
    };
	
	// merge them all
	File[] finalExamples = new File[inclExamples.length + jModeExamples.length];
	for ( int i = 0; i < inclExamples.length; i++ )
		finalExamples[i] = inclExamples[i];
	for ( int i = 0; i < jModeExamples.length; i++ )
		finalExamples[inclExamples.length+i] = jModeExamples[i];
	
	java.util.Arrays.sort(finalExamples);

    return finalExamples;
  }

	/**
	 *	Overriding this from Mode.java to remove "Contributed Libraries"
	 */

	public JTree buildExamplesTree()
	{
		JTree superTree = super.buildExamplesTree();

		DefaultTreeModel model = (DefaultTreeModel) superTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		DefaultMutableTreeNode contribExamples = (DefaultMutableTreeNode) root.getLastChild();
		Object title = contribExamples.getUserObject();
		if ( title != null && title.getClass() == String.class && ((String)title).equals("Contributed Libraries") )
		{
			root.remove( contribExamples );
		}

		return superTree;
	}
  
    /**
	 *	Return the default extension for this mode, same as Java
	 */
	public String getDefaultExtension() 
	{
		return "pde";
	}

	/**
	 *	Return allowed extensions
	 */
	public String[] getExtensions () 
	{
		return new String[] { "pde", "js" };
	}

	/**
	 *	Return list of file- / folder-names that should be ignored when 
	 *	sketch is being copied or saved as 
	 */
	public String[] getIgnorable () 
	{
		return new String[] {
			"applet",
			"applet_js",
			JavaScriptBuild.EXPORTED_FOLDER_NAME
		};
	}

  	/**
  	 *	Override Mode.getLibrary to add our own discovery of JS-only libraries.
  	 *
  	 *	fjenett 20121202
  	 */
	public Library getLibrary ( String pkgName ) throws SketchException 
	{
		return super.getLibrary( pkgName );
	}
  
  
  // ------------------------------------------------
  
  	/**
  	 *	Build and export a sketch
  	 */
	public boolean handleExport(Sketch sketch) throws IOException, SketchException
	{
		JavaScriptBuild build = new JavaScriptBuild(sketch);
		return build.export();
	}

	//public boolean handleExportApplet(Sketch sketch) throws SketchException, IOException { }

	//public boolean handleExportApplication(Sketch sketch) throws SketchException, IOException { }
}
