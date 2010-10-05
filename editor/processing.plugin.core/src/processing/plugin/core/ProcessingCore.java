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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Plugin;

import processing.plugin.core.ProcessingUtilities;
 
/**
 * The plug-in activator enabling the core (UI-free) support for Processing sketches.
 * <p>
 * The single instance of this class can be accessed from any plug-in declaring the 
 * Processing core plug-in as a prerequisite via 
 * <code>ProcessingCore.getProcessingCore()</code>. The Processing core plug-in 
 * will be activated and instantiated automatically if it is not already active.
 * </p>
 */
public final class ProcessingCore extends Plugin {

	/** The plug-in identifier of the Processing core support */
	public static final String PLUGIN_ID = "processing.plugin.core";

	/** shared plugin object */
	private static ProcessingCore plugin;

	/** shared resource bundle */
	private ResourceBundle resourceBundle;

	/**
	 * Creates the Processing core plug-in.
	 * <p>
	 * The plug-in instance is created automatically by the eclipse platform. 
	 * Clients must not call.
	 * </p>
	 * <p>
	 * Patterned after the JDT, this is public, but I'm not sure why. I thought
	 * singletons were supposed to have private constructors. [lonnen 09 09 2010]
	 */
	public ProcessingCore(){
		super();
		plugin = this;
		try{
			this.resourceBundle = ResourceBundle.getBundle(PLUGIN_ID + ".CorePluginResources");
		} catch (MissingResourceException x){
			this.resourceBundle = null;
		}
	}

	// special initialization and shutdown goes here
	/* 	public void start(BundleContext context) throws Exception {} */
	/* 	public void stop(BundleContext context) throws Exception {} */

	/** 
	 * Gets a URL to a file or folder in the plug-in's Resources folder.
	 * Returns null if the path results in a bad URL.
	 * 
	 * @param path relative path from the Resources folder
	 */
	public URL getPluginResource(String path){
		try{
			return new URL(this.getBundle().getEntry("/"), "Resources/" + path);
		} catch (MalformedURLException e){
			return null;
		}
	}

	/**
	 * Returns a URL to a file or folder in the plug-in's Resources folder.
	 * Returns null if the path results in a bad URL.
	 * 
	 * @param path relative from the Resources folder
	 */
	public URL getPluginResource(IPath path){
		return getPluginResource(path.toOSString());
	}
	
	/**
	 * Resolves the plug-in resources folder to a File and returns it. This will include the
	 * Processing libraries and the core libraries folder.
	 * 
	 * @return File reference to the core resources
	 */
	public File getPluginResourceFolder(){
		URL fileLocation = getPluginResource("");
		try {
			File folder = new File(FileLocator.toFileURL(fileLocation).getPath());
			if (folder.exists())
				return folder;
		} catch (Exception e) {
			ProcessingLog.logError(e);
		}
		return null;
	}

	/** Returns a file handle to the plug-in's local cache folder. */
	public File getBuiltInCacheFolder(){
		return new File(this.getStateLocation().toString());
	}

	/** Returns a file handle to the temp folder in the plug-in's local cache*/
	public File getPluginTempFolder(){
		return new File(getBuiltInCacheFolder(), "temp");
	}

	/** Returns the plug-in's resource bundle */
	public ResourceBundle getResourceBundle(){
		return resourceBundle;
	}

	/** 
	 * Get the workspace the platform workspace.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/** Returns the single instance of the Processing core plug-in runtime class. */
	public static ProcessingCore getProcessingCore(){
		return plugin;
	}
	
	/** Returns true if the resource is a Processing file */	
	public static boolean isProcessingFile(IResource resource){
		if (resource.getType() == IResource.FILE)
			return isProcessingFile(resource.getName());
		return false;
	}

	/** Returns true if the file is a Processing file  */
	public static boolean isProcessingFile(IFile resource) {
		return isProcessingFile(resource.getName());
	}

	/** Returns true if the file has a Processing extension */
	public static boolean isProcessingFile(String filename){
		return filename.endsWith(".pde");
	}

	/** Returns true if the IFolder is a Processing library root folder */
	public static boolean isLibrary(IFolder rootFolder){
		return isLibrary(rootFolder.getFullPath().toFile());
	}

	/** 
	 * Returns true if the folder is a Processing library root folder and
	 * only complains if there is an error.
	 */
	public static boolean isLibrary(File rootFolder){
		return isLibrary(rootFolder, false);
	}

	/**
	 * Returns true if the folder is a Processing library root folder.
	 * When complain is false only errors are logged and reported. When 
	 * complain is true the standard PDE warning for improperly named
	 * libraries will also be reported.
	 */
	public static boolean isLibrary(File rootFolder, boolean complain){
		if (rootFolder == null) return false;
		if(!rootFolder.isDirectory()) return false;

		String name = rootFolder.getName();
		try {
			File libraryJar = new File(rootFolder.getCanonicalPath() + 
					File.separatorChar + "library" + File.separatorChar + 
					name + ".jar");
			if (libraryJar.exists())
				if (ProcessingUtilities.sanitizeName(name).equals(name)){
					return true;
				} else {
					if(complain){
						String mess =
							"The library \"" + name + "\" cannot be used.\n" +
							"Library names must contain only basic letters and numbers.\n" +
							"(ASCII only and no spaces, and it cannot start with a number)";
						ProcessingLog.logInfo("Ignoring bad library " + name + "\n" + mess);
					}
				}
		} catch (IOException e) {
			ProcessingLog.logError("Problem checking library " +
					name + ", could not resolve canonical path.", e);
		}

		return false;	
	}

	/**
	 * Finds the folder containing the Processing core libraries, which are bundled with the
	 * plugin. This folder doesn't exist in the workspace, so we return it as a File, not IFile. 
	 * If something goes wrong, logs an error and returns null.
	 *  
	 * @return File containing the core libraries folder or null
	 */
	public File getCoreLibsFolder() {
		URL fileLocation = getPluginResource("libraries");
		try {
			File folder = new File(FileLocator.toFileURL(fileLocation).getPath());
			if (folder.exists())
				return folder;
		} catch (Exception e) {
			ProcessingLog.logError(e);
		}
		return null;
	}
	
	/**
	 * Finds and retrieves core.jar in the resource bundle.
	 * 
	 * @return File core.jar
	 */
	public File getCoreJarFile(){
		URL coreLoc = getPluginResource("lib");
		try{
			File folder = new File(FileLocator.toFileURL(coreLoc).getPath());
			if (folder.exists()){
				File core = new File(folder, "core.jar");
				if (core.exists())
					return core;
			}
		}catch (Exception e) {
			ProcessingLog.logError(e);
		}
		ProcessingLog.logInfo("Something went wrong getting the core.jar file from the Processing plug-in. All sketches will break without this. Please reinstall the plug-in.");
		return null;
	}

}
