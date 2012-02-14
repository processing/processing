package processing.mode.coffeescript;

import java.io.File;

import processing.mode.javascript.JavaScriptMode;

import processing.app.Base;
import processing.app.Editor;
import processing.app.EditorState;

/**
 *	CoffeeScript mode.
 *	http://coffeescript.org/
 */

public class CoffeeScriptMode extends JavaScriptMode
{
	private CoffeeScriptEditor csEditor;
	
	public CoffeeScriptMode( Base base, File folder )
	{
		super( base, folder );
	}
	
	public Editor createEditor( Base base, String path, EditorState state )
	{
		if ( path != null && !path.endsWith(".coffee") ) 
		{
			String cPath = path.replace(".pde", ".coffee");
			File cFile = new File( cPath );
			if ( !cFile.exists() ) 
			{
				try {
					if ( !cFile.createNewFile() ) 
					{
						System.err.println( "CoffeeScriptMode: " + 
											"unable to create .coffee file for .pde file at:\n" + 
											cPath );
					}
				} catch ( java.io.IOException ioe ) {
					ioe.printStackTrace();
				}
			}
			path = cPath;
		}
	 	csEditor = new CoffeeScriptEditor( base, path, state, this );
		return csEditor;
	}
	
	public String getTitle ()
	{
		return "CoffeeScript";
	}
	
	public File[] getExampleCategoryFolders ()
	{
		File[] csExamples = examplesFolder.listFiles(new java.io.FileFilter(){
			public boolean accept (File f) {
				return f.isDirectory();
			}
		});
		java.util.Arrays.sort(csExamples);
		
		return csExamples;
	}
	
	public String getDefaultExtension () 
	{
		return "coffee";
	}

	public String[] getExtensions () 
	{
		return new String[] { "coffee", "js" };
	}

	// these come from JavaScript mode 
	//public String[] getIgnorable() { }
}