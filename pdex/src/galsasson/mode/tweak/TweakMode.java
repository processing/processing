package galsasson.mode.tweak;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import processing.app.Library;
import processing.app.RunnerListener;
import processing.app.Sketch;
import processing.app.Base;
import processing.app.Editor;
import processing.app.EditorState;
import processing.app.Mode;
import processing.app.SketchCode;
import processing.app.SketchException;
import processing.mode.java.JavaBuild;
import processing.mode.java.JavaMode;
import processing.mode.java.runner.Runner;

/**
 * Mode for enabling real-time modifications to numbers in the code.
 *
 */
public class TweakMode extends JavaMode {

	public boolean dumpModifiedCode;
	
	public TweakMode(Base base, File folder) 
	{
		super(base, folder);

		// needed so that core libraries like opengl, etc. are loaded.
		for (Mode m : base.getModeList()) {
			if (m.getClass() == JavaMode.class) {
				JavaMode jMode = (JavaMode) m;
				librariesFolder = jMode.getLibrariesFolder();
				rebuildLibraryList(); 
				break;
			}
		}

		// Fetch examples and reference from java mode
		examplesFolder = Base.getContentFile("modes/java/examples");
		referenceFolder = Base.getContentFile("modes/java/reference");

		dumpModifiedCode = false;
	}

    /**
     * Return the pretty/printable/menu name for this mode. This is separate
     * from the single word name of the folder that contains this mode. It could
     * even have spaces, though that might result in sheer madness or total
     * mayhem.
     */
    @Override
    public String getTitle() {
        return "tweak";
    }

    /**
     * Create a new editor associated with this mode.
     */
    @Override
    public Editor createEditor(Base base, String path, EditorState state) {
    	return new TweakEditor(base, path, state, this);
    }

    /**
     * Returns the default extension for this editor setup.
     */
    /*
    @Override
    public String getDefaultExtension() {
        return null;
    }
    */

    /**
     * Returns a String[] array of proper extensions.
     */
    /*
    @Override
    public String[] getExtensions() {
        return null;
    }
    */

    /**
     * Get array of file/directory names that needn't be copied during "Save
     * As".
     */
    /*
    @Override
    public String[] getIgnorable() {
        return null;
    }
    */
    
    /**
     * Retrieve the ClassLoader for JavaMode. This is used by Compiler to load
     * ECJ classes. Thanks to Ben Fry.
     *
     * @return the class loader from java mode
     */
    @Override
    public ClassLoader getClassLoader() {
        for (Mode m : base.getModeList()) {
            if (m.getClass() == JavaMode.class) {
                JavaMode jMode = (JavaMode) m;
                return jMode.getClassLoader();
            }
        }
        return null;  // badness
    }

	@Override
	public Runner handleRun(Sketch sketch, RunnerListener listener) throws SketchException 
	{
		return handlePresentOrRun(sketch, listener, false);
	}
	
	@Override
	public Runner handlePresent(Sketch sketch, RunnerListener listener) throws SketchException
	{
		return handlePresentOrRun(sketch, listener, true);
	}
	
	public Runner handlePresentOrRun(Sketch sketch, RunnerListener listener, boolean present) throws SketchException 
	{
		final TweakEditor editor = (TweakEditor)listener;
		final boolean toPresent = present;
		
		if (!verifyOscP5()) {
			editor.deactivateRun();
			return null;
		}
		
		boolean launchInteractive = false;

		if (isSketchModified(sketch)) {
			editor.deactivateRun();
			Base.showMessage("Save", "Please save the sketch before running in Tweak Mode.");
			return null;	
		}
		
		/* first try to build the unmodified code */
		JavaBuild build = new JavaBuild(sketch);		
		String appletClassName = build.build(false);
		if (appletClassName == null) {
			// unmodified build failed, so fail
			return null;
		}

		/* if compilation passed, modify the code and build again */
		editor.initBaseCode();
		// check for "// tweak" comment in the sketch 
		boolean requiresTweak = SketchParser.containsTweakComment(editor.baseCode);
		// parse the saved sketch to get all (or only with "//tweak" comment) numbers
		final SketchParser parser = new SketchParser(editor.baseCode, requiresTweak);
		
		// add our code to the sketch
		launchInteractive = editor.automateSketch(sketch, parser.allHandles);
		
		build = new JavaBuild(sketch);
		appletClassName = build.build(false);
		
		if (appletClassName != null) {
			final Runner runtime = new Runner(build, listener);
			new Thread(new Runnable() {				
				public void run() {
					runtime.launch(toPresent);  // this blocks until finished
              
					// executed when the sketch quits
					editor.initEditorCode(parser.allHandles, false);
					editor.stopInteractiveMode(parser.allHandles);
				}
				
			}).start();
          
			if (launchInteractive) { 

				// replace editor code with baseCode 
				editor.initEditorCode(parser.allHandles, false);				
				editor.updateInterface(parser.allHandles, parser.colorBoxes);
				editor.startInteractiveMode();
			}

			return runtime;
		}
        
		return null;    	
	}
	
	private boolean verifyOscP5()
	{
		for (Library l : contribLibraries) {
			if (l.getName().equals("oscP5")) {
				return true;
			}
		}
		
		// could not find oscP5 library
		Base.showWarning("Tweak Mode", "Tweak Mode needs the 'oscP5' library.\n"
				+ "Please install this library by clicking \"Sketch --> Import Library --> Add Library ...\" and choose 'ocsP5'", null);
		
		return false;
	}

	private boolean isSketchModified(Sketch sketch)
	{
		for (SketchCode sc : sketch.getCode()) {
			if (sc.isModified()) {
				return true;
			}
		}
		return false;
	}	
}
