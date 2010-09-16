/**
r * Copyright (c) 2010 Chris Lonnen. All rights reserved.
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
import org.eclipse.core.runtime.preferences.ConfigurationScope;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Utility class for controlling access to the Processing Core preference store.
 * <p>
 * Every expected preference has its own getter / setter
 */
public class ProcessingCorePreferences {

	private static ProcessingCorePreferences current;
		
	static { current = new ProcessingCorePreferences(); }
		
	protected static final String CORE_PREFERENCES = ProcessingCore.PLUGIN_ID + ".preferences";
	
	/** Name of the sketchbook location preference for lookup. */
	protected static final String SKETCHBOOK = CORE_PREFERENCES + ".sketchbook";
	
	/** singleton pattern means boring constructors */
	private ProcessingCorePreferences(){}
	
	/** access the singleton object */
	public static ProcessingCorePreferences current(){ return current; }
	
	/** Get the preferences store. */
	public Preferences getStore(){ return new ConfigurationScope().getNode(CORE_PREFERENCES); }
	
	/** alias to save the preferences changes */
	public void setStore(){ this.save(); }
	
	/** save any preference changes */
	public void save(){
		try{
			this.getStore().flush();
		} catch (BackingStoreException bse){
			ProcessingLog.logError("Could not save Processing Core Preferences.", bse);	
		}
	}
	
	/** Returns the stored sketchbook path as a string. */
	public String getSketchbookPathAsString(){	return this.getStore().get(SKETCHBOOK, null); }
	
	/** Set the sketchbook path using a path */
	public void setSketchbookPathWithPath(IPath sketchbookPath){
		this.setSketchbookPathWithString(sketchbookPath.toOSString());
	}
	
	/** Saves the path to the sketchbook. */
	public void setSketchbookPathWithString(String sketchbookPath){
		this.getStore().put(SKETCHBOOK, sketchbookPath);
		this.save();
	}
	
	/** Returns the path to the sketchbook or null if there is none. */
	public IPath getSketchbookPath(){ return this.pathOrNull(this.getSketchbookPathAsString()); }

	
	/** Utility method that returns a path from a string or null */
	private IPath pathOrNull(String pathString){
		if (pathString == null) return null;
		return ( pathString.length() == 0 ) ? null : new Path(pathString);
	}
}
