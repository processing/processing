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
package processing.plugin.core.exporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import processing.plugin.core.ProcessingCore;
import processing.plugin.core.ProcessingLog;
import processing.plugin.core.ProcessingUtilities;
import processing.plugin.core.builder.SketchProject;

/** Static export functions. */
public class Exporter {

	/* Common init code */
	static{}
	
	/** 
	 * Attempts to export a SketchProject as an applet.
	 * <p> 
	 * This operation can be long running, but does not have an optional progress
	 * monitor because that would require mixing UI into the non-UI code. It also
	 * knocks the workspace out of sync with the internal file system. Clients 
	 * will probably (unless the method bails really early) need to trigger a
	 * workspace refresh of the project.
	 * 
	 * @return boolean whether the export was successful
	 */
	public static boolean exportApplet(SketchProject sp){
		if (sp == null) return false;
		if (!sp.getProject().isAccessible()) return false;

		try{
			sp.fullBuild(null);
		} catch (CoreException e){
			ProcessingLog.logError(e);
			return false;
		}
		if (!sp.wasLastBuildSuccessful()){
			ProcessingLog.logError("Could not export " + sp.getProject().getName() + 
					". There were erros building the project.", null);
			return false;
		}
				
		IFile code = sp.getMainFile();
		if (code == null) return false;	
		String codeContents = ProcessingUtilities.readFile(code);
		
		IFolder exportFolder = sp.getAppletFolder(true); // true to nuke the folder contents, if they exist

		HashMap<String,Object> zipFileContents = new HashMap<String,Object>();
		
		// Get size and renderer info from the project
		int wide = sp.getWidth();
		int high = sp.getHeight();
		String renderer = sp.getRenderer();
		
		// Grab the Javadoc-style description from the main code		
		String description ="";
		String[] javadoc = ProcessingUtilities.match(codeContents, "/\\*{2,}(.*)\\*+/");
		if (javadoc != null){
			StringBuffer dbuffer = new StringBuffer();
			String[] pieces = ProcessingUtilities.split(javadoc[1], '\n');
			for (String line : pieces){
				// if this line starts with * characters, remove em
				String[] m = ProcessingUtilities.match(line, "^\\s*\\*+(.*)");
				dbuffer.append(m != null ? m[1] : line);
				dbuffer.append('\n');
			}
			description = dbuffer.toString();
			//System.out.println(description);
		}
		
		// Copy the source files to the target, since we like to encourage people to share their code
		// Get links for each copied code file
		StringBuffer sources = new StringBuffer();
		try{
			for(IResource r : sp.getProject().members()){
				if(!(r instanceof IFile)) continue;
				if(r.getName().startsWith(".")) continue;
				if("pde".equalsIgnoreCase(r.getFileExtension())){
					try{
						r.copy(exportFolder.getFullPath().append(r.getName()), true, null);
						sources.append("<a href=\"" + r.getName() + "\">" +
							r.getName().subSequence(0, r.getName().lastIndexOf(".")-1)
							+ "</a> ");
					} catch (CoreException e) {
						ProcessingLog.logError("Sketch source files could not be included in export of "
						+ sp.getProject().getName() +". Trying to continue export anyway.", e);
					}
				}
			}
		} catch (CoreException e){
			ProcessingLog.logError(e); // problem getting members
		}
		
		// Use separate jarfiles
		boolean separateJar = true; 
		// = Preferences.getBoolean("export.applet.separate_jar_files)||
		// codeFolder.exists() ||
		// (libraryPath.length() != 0);
		
		// Copy the loading gif to the applet
		String LOADING_IMAGE = "loading.gif";
		IFile loadingImage = sp.getProject().getFile(LOADING_IMAGE); // user can specify their own loader
		try {
			loadingImage.copy(exportFolder.getFullPath().append(LOADING_IMAGE), true, null);
		} catch (CoreException e) {
			// This will happen when the copy fails, which we expect if there is no
			// image file. It isn't worth reporting.
			try {
				File exportResourcesFolder = new File(ProcessingCore.getProcessingCore().getPluginResourceFolder().getCanonicalPath(), "export");
				File loadingImageCoreResource = new File(exportResourcesFolder, LOADING_IMAGE);
				ProcessingUtilities.copyFile(loadingImageCoreResource, new File(exportFolder.getLocation().toFile(), LOADING_IMAGE));
			} catch (Exception ex) {
				// This is not expected, and should be reported, because we are about to bail
				ProcessingLog.logError("Could not access the Processing Plug-in Core resources. " +
						"Export aborted.", ex);
				return false;
			}
		}
		
	    // Create new .jar file
	    FileOutputStream zipOutputFile;
		try {
			zipOutputFile = new FileOutputStream(new File(exportFolder.getLocation().toFile(), sp.getProject().getName() + ".jar"));
		} catch (FileNotFoundException fnfe) {
			ProcessingLog.logError(" ",fnfe);
			return false;
		}
	    ZipOutputStream zos = new ZipOutputStream(zipOutputFile);
	    ZipEntry entry;

	    StringBuffer archives = new StringBuffer();
	    archives.append(sp.getProject().getName() + ".jar");		

	    //addmanifest(zos);
	    
		// add the contents of the code folder to the jar
		IFolder codeFolder = sp.getCodeFolder();
		if (codeFolder != null){
			try{
				for(IResource r : codeFolder.members()){
					if(!(r instanceof IFile)) continue;
					if(r.getName().startsWith(".")) continue;
					if ("jar".equalsIgnoreCase(r.getFileExtension()) || 
							"zip".equalsIgnoreCase(r.getFileExtension())){
						r.copy(exportFolder.getFullPath().append(r.getName()), true, null);
	        			//System.out.println("Copied the file to " + exportFolder.getFullPath().toString() + " .");
					}
				}
			} catch (CoreException e){
				ProcessingLog.logError("Code Folder entries could not be included in export." +
						"Export for " + sp.getProject().getName() + " may not function properly.", e);
			}
		}

		// snag the opengl library path so we can test for it later
		File openglLibrary = new File(ProcessingCore.getProcessingCore().getCoreLibsFolder(), "opengl/library/opengl.jar");
		String openglLibraryPath = openglLibrary.getAbsolutePath();
		boolean openglApplet = false;			
		
		// add the library jar files to the folder and detect if opengl is in use
		ArrayList<IPath> sketchLibraryImportPaths = sp.getLibraryPaths();
		if(sketchLibraryImportPaths != null){
			for(IPath path : sketchLibraryImportPaths){
				if (path.toOSString().equals(openglLibraryPath)) 
					openglApplet = true;
				
				// for each exportFile in library.getAppletExports()
				
				
//				File libraryFolder = new File(path.toOSString());
//				if (path.toOSString().equalsIgnoreCase(openglLibraryPath)) openglApplet=true;
//				File exportSettings = new File(libraryFolder, "export.txt");
//				HashMap<String,String> exportTable = ProcessingUtilities.readSettings(exportSettings);
//				String appletList = (String) exportTable.get("applet");
//				String exportList[] = null;
//				if(appletList != null){
//					exportList = ProcessingUtilities.splitTokens(appletList, ", ");
//				} else {
//					exportList = libraryFolder.list();
//				}
//				for (String s : exportList){
//					if (s.equals(".") || s.equals("..")) continue;
//					
//					s = ProcessingUtilities.trim(s);
//					if (s.equals("")) continue;
//					
//					File exportFile = new File( libraryFolder, s);
//					if(!exportFile.exists()) {
//						ProcessingLog.logError("Export File " + s + " does not exist.", null);
//					} else if (exportFile.isDirectory()) {
//						ProcessingLog.logInfo("Ignoring sub-folder \"" + s + "\"");
//					} else if ( exportFile.getName().toLowerCase().endsWith(".zip") ||
//								exportFile.getName().toLowerCase().endsWith(".jar")){
//						// the PDE checks for separate jar boolean, but if we're here we have
//						// met the conditions that require it
////						File exportFile = new File(codeFolder, s);
//					}
//					
//				}
			}
		}

		
		
		//DEBUG
		ProcessingLog.logInfo("Could not export " + sp.getProject().getName() 
				+ " because the exporting applets is not finished.");
		return false;
	}

	/**
	 * Unimplemented. Does nothing.
	 *
	 * @return boolean always false
	 */
	public static boolean exportApplication(SketchProject sp){ 
		ProcessingLog.logInfo("Could not export " + sp.getProject().getName() 
				+ " because exporting applications has not been implemented.");
		return false; 
	}
	
}