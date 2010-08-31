/*******************************************************************************
 * This program and the accompanying materials are made available under the 
 * terms of the Common Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.opensource.org/licenses/cpl1.0.php
 * 
 * Contributors:
 *     Red Robin - Design pattern
 *     Chris Lonnen - Initial API and implementation 
 *******************************************************************************/
package processing.plugin.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Container class for controlling access to the Procesing Core 
 * preference store. Modification of these preferences should only
 * be done by preferences pages in processing.plugin.ui plug-in, but
 * may be programatically accessed in the case of headless operation.
 */
public class ProcessingCorePreferences {

	private static ProcessingCorePreferences current;
	
	// instantiate the plug in
	static {
		current = new ProcessingCorePreferences();
	}
	
	// EVERY PREFERENCE SHOULD HAVE A SIMILAR SETUP
	
	/** Name of the sketchbook location preference for lookup. */
	protected static final String SKETCHBOOK = ProcessingCore.PLUGIN_ID + ".preferences.skethbook";
	
	/** Returns the stored sketchbook path as a string. */
	public String getSketchbookPathString(){
		return this.getStore().get(SKETCHBOOK, "");
	}
	
	/** Returns the path to the sketchbook or null if there is none. */
	public IPath getSketchbookPath(){
		return this.pathOrNull(this.getSketchbookPathString());
	}
	
	/** Saves the path to the sketchbook. */
	public void setSketchbookPath(String sketchbookPath){
		this.getStore().put(SKETCHBOOK, sketchbookPath);
		this.save();
	}
	
	// an identifier, get string value, and set new string value
	// also, directly getting something useful instead of a string (like a path)

	/** Singleton pattern */
	private ProcessingCorePreferences(){}
	
	/** Return current preferences. */
	public static ProcessingCorePreferences current(){
		return current;
	}
	
	/** Save any preference changes */
	public void save(){
		try{
			this.getStore().flush();
		} catch (BackingStoreException e){
			ProcessingLog.logError("Preferences could not be saved.", e);
		}
	}
	
	/** Get the preferences store. */
	protected Preferences getStore(){
		return new ConfigurationScope().getNode(ProcessingCore.PLUGIN_ID);
	}
	
	/** Utility method that returns a path from a string or null */
	protected IPath pathOrNull(String pathString){
		return ( pathString.length()== 0 ) ? null : new Path(pathString);
	}
}
