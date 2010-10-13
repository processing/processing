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
package processing.plugin.core.model;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;

import processing.plugin.core.ProcessingCore;
import processing.plugin.core.ProcessingCorePreferences;
import processing.plugin.core.ProcessingLog;
import processing.plugin.core.ProcessingUtilities;

/** 
 * Provides access to Processing libraries.
 * <p>
 * Provides some static methods for detecting libraries and locating library folders.
 * <p>
 * Instantiation of the model is handled by the ProcessingCore, and clients requiring
 * access to the LibraryModel should use that singleton instance to get to things to
 * keep things from getting out of sync. An instance of the model provides a convenient
 * lookup table for the packages.
 */
public class LibraryModel {	// naming is hard.

	/** Maps imported packages to LibraryFolder objects */
	static HashMap<String, LibraryFolder> importToLibraryTable;

	/**
	 * Try to get the user defined library folder from the sketchbook location in preferences.
	 * 
	 * @return a File handle to the sketchbook library folder or null if it doesn't exist
	 */
	public static File getSketchBookLibsFolder() {
		IPath sketchbook = ProcessingCorePreferences.current().getSketchbookPath();
		if (sketchbook == null) return null;
		File userLibs = new File(sketchbook.append("libraries").toOSString());
		return (userLibs.exists()) ? userLibs: null;
	}

	/**
	 * Returns the folder containing the Processing core libraries, which are bundled 
	 * with the plugin. If they cannot be found, log an exception and return null.
	 * This indicates something has gone very wrong, and we should be wary.
	 *  
	 * @return File containing the core libraries folder or null
	 */
	public static File getCoreLibsFolder() {
		URL fileLocation = ProcessingCore.getCore().getPluginResource("libraries");
		try {
			File folder = new File(FileLocator.toFileURL(fileLocation).getPath());
			if (folder.exists()) return folder;
		} catch (Exception e) {
			ProcessingLog.logError("Couldn't get Core libraries folder",e);
		}
		return null;
	}

	/** @return true if the folder is the root of a valid Processing library folder structure. */
	public static boolean isLibrary(File rootFolder){
		if (rootFolder == null) return false;
		if (!rootFolder.isDirectory()) return false;

		String name = rootFolder.getName();

		File libraryFolder = new File(rootFolder, "library");
		File libraryJar = new File( libraryFolder, name + ".jar" );

		if (!libraryJar.exists()) return false;
		if (!ProcessingUtilities.sanitizeName(name).equals(name)) {
			ProcessingLog.logInfo(
					"The library \"" + name + "\" is being ignored. " +
					"Library names must contain only basic letters and numbers. " +
					"(ASCII only and no spaces, and it cannot start with a number)" 
			);
			return false;
		}
		return true;
	}

	// I'm not sure these are used right now.
	// In the PDE they are used for GUI stuff that isn't in place here
	//ArrayList<LibraryFolder> coreLibraries;
	//ArrayList<LibraryFolder> contribLibraries;

	/** Creating the model builds the library list. */
	public LibraryModel(){
		this.rebuildLibraryList();
	}

	/** Rebuild the library import tables from scratch. */
	public void rebuildLibraryList(){
		importToLibraryTable = new HashMap<String, LibraryFolder>();
		try{
			// LibraryFolder.list() updates the import table as a side affect
			
			//coreLibraries = LibraryFolder.list(LibraryModel.getCoreLibsFolder());
			//contribLibraries = LibraryFolder.list(LibraryModel.getSketchBookLibsFolder());
			LibraryFolder.list(LibraryModel.getCoreLibsFolder());
			LibraryFolder.list(LibraryModel.getSketchBookLibsFolder());
		} catch (IOException e){
			ProcessingLog.logError("Unhappiness! "
					+ "An error occured while loading libraries, "
					+ " not all the books will be in place.", e
			);
		}
	}

	/**
	 * Access to the internal lookup table.
	 * 
	 * @param pkg a String containing a package name
	 * @return LibraryFolder for that package, or null if it can't be found 
	 */
	public LibraryFolder getLibraryFolder(String pkg){
		return importToLibraryTable.get(pkg);
	}
	
}
