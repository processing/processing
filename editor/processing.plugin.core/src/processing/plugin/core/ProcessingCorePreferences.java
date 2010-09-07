/**
 * Copyright (c) 2010 Chris Lonnen. All rights reserved.
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * Contributors:
 *     Chris Lonnen - initial API and implementation
 */
package processing.plugin.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;

/**
 * Utility class for controlling access to the Processing Core preference store.
 * <p>
 * Every expected preference has its own getter / setter
 */
@SuppressWarnings("deprecation")
public class ProcessingCorePreferences {
	//TODO this uses the depreciated Preferences store. Fix that.

	private static ProcessingCorePreferences current;
	
	// instantiate the plug in
	static { current = new ProcessingCorePreferences(); }
		
	/** Name of the sketchbook location preference for lookup. */
	protected static final String SKETCHBOOK = ProcessingCore.PLUGIN_ID + ".preferences.sketchbook";
	
	/** Returns the stored sketchbook path as a string. */
	public String getSketchbookPathAsString(){
		return this.getStore().getString(SKETCHBOOK);
	}
	
	/** Saves the path to the sketchbook. */
	public void setSketchbookPathWithString(String sketchbookPath){
		this.getStore().setValue(SKETCHBOOK, sketchbookPath);
		this.save();
	}
	
	/** Returns the path to the sketchbook or null if there is none. */
	public IPath getSketchbookPath(){
		return this.pathOrNull(this.getSketchbookPathAsString());
	}
	
	/** Set the sketchbook path using a path */
	public void setSketchbookPathWithPath(IPath sketchbookPath){
		this.getStore().setValue(SKETCHBOOK, sketchbookPath.toOSString());
	}
	
	/** Singleton pattern */
	private ProcessingCorePreferences(){}
	
	/** Return current preferences. */
	public static ProcessingCorePreferences current(){
		return current;
	}
	
	/** Save any preference changes */
	public void save(){
		ProcessingCore.getProcessingCore().savePluginPreferences();
	}
	
	/** Get the preferences store. */
	public Preferences getStore(){
		return ProcessingCore.getProcessingCore().getPluginPreferences();
	}
	
	/** Utility method that returns a path from a string or null */
	private IPath pathOrNull(String pathString){
		return ( pathString.length()== 0 ) ? null : new Path(pathString);
	}
}
