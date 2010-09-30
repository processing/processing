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
package processing.plugin.ui.wizards;

//import java.util.Iterator;
//import org.eclipse.core.resources.IProject;
//import org.eclipse.core.resources.IResource;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import processing.plugin.core.ProcessingCore;
import processing.plugin.core.ProcessingLog;
import processing.plugin.core.builder.SketchProject;
import processing.plugin.core.builder.Utilities;
import processing.plugin.ui.ProcessingPlugin;


/**
 * An export wizard for processing projects.
 * <p>
 * The single page presents users with a list of open sketch Projects in 
 * the workspace and the user can check one or all of them to export.
 */
public class ExportAsAppletWizard extends Wizard implements IExportWizard {
	
	/** single page */
	private ExportAsAppletSelectProjectsWizardPage page;

	public ExportAsAppletWizard() {}

	/** 
	 * Used to figure out what we're exporting. 
	 * <p>
	 * If more than one item is selected, the first one listed
	 * is the one that gets exported. Just to make sure, the wizard
	 * page prompts with the name of the sketch before continuing. 
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
//		workbench.getActiveWorkbenchWindow().getActivePage();
//		Iterator iter = selection.iterator();
//		while (iter.hasNext()){
//			Object selectedElement = iter.next();
//			if (selectedElement instanceof IProject) {
//				IProject proj = (IProject) selectedElement;
//				if(SketchProject.isSketchProject(proj)){
//
//				}
//			} else if (selectedElement instanceof IResource){
//				IProject proj = ((IResource) selectedElement).getProject();
//				if(SketchProject.isSketchProject(proj)){
//					setProject(proj);
//					break;
//				}
//			}
		// Nowadays the page takes care of it. User selects from whatever is open.
	
	}		

	public void addPages(){
		page = new ExportAsAppletSelectProjectsWizardPage("Export Sketch Wizard");
		addPage(page);
	}

	public boolean performFinish() {
//		return SketchProject.forProject(page.getProject()).exportAsApplet();
		ArrayList<String> couldNotExport = new ArrayList<String>();
		for(SketchProject sp : page.getSelectedProjects()){
//			System.out.println(sp.getProject().getName());
			if (!exportAsApplet(sp)) couldNotExport.add(sp.getProject().getName());
		}
		
		if (couldNotExport.size() > 0)
			for(String s : couldNotExport) ProcessingLog.logInfo( "Unable to export " + s + ".");
			
		return true;
	}
	
	/** 
	 * Tries to export the sketch as an applet, returns whether it was successful.
	 * <p>
	 * This method relies on non-workspace resources, and so invoking it knocks
	 * things out of sync with the file system. It triggers a workspace refresh
	 * of the project after it is finished to realign things.
	 */
	public boolean exportAsApplet(SketchProject sp) {
		if (sp == null) return false;
		if (!sp.wasLastBuildSuccessful()) return false;
		if (!sp.getProject().isAccessible()) return false;
		
		IFile code = sp.getMainFile();
		if (code == null) return false;	
		String codeContents = Utilities.readFile(code);
		
		IFolder exportFolder = sp.getAppletFolder(true); // true to nuke the folder contents, if they exist
		
		// Get size and renderer info from the project
		int wide = sp.getWidth();
		int high = sp.getHeight();
		String renderer = sp.getRenderer();
		
		// Grab the Javadoc-style description from the main code		
		String description ="";
		String[] javadoc = Utilities.match(codeContents, "/\\*{2,}(.*)\\*+/");
		if (javadoc != null){
			StringBuffer dbuffer = new StringBuffer();
			String[] pieces = Utilities.split(javadoc[1], '\n');
			for (String line : pieces){
				// if this line starts with * characters, remove em
				String[] m = Utilities.match(line, "^\\s*\\*+(.*)");
				dbuffer.append(m != null ? m[1] : line);
				dbuffer.append('\n');
			}
			description = dbuffer.toString();
			System.out.println(description);
		}
		
		//Copy the source files to the target, since we like to encourage people to share their code
		try{
			for(IResource r : sp.getProject().members()){
				if(!(r instanceof IFile)) continue;
				if(r.getName().startsWith(".")) continue;
				if("pde".equalsIgnoreCase(r.getFileExtension())){
					r.copy(exportFolder.getFullPath().append(r.getName()), true, null);
					System.out.println("Copied the source file " + r.getName() 
							+ " to " + exportFolder.getFullPath().toString());
				}
			}
		} catch (CoreException e){
			ProcessingLog.logError("Sketch source files could not be included in export of "
					+ sp.getProject().getName() +". Trying to continue export anyway. ", e);
		}
		
		// Copy the loading gif to the applet
		String LOADING_IMAGE = "loading.gif";
		IFile loadingImage = sp.getProject().getFile(LOADING_IMAGE); // user can specify their own loader
		try {
			loadingImage.copy(exportFolder.getFullPath().append(LOADING_IMAGE), true, null);
		} catch (CoreException e) {
			// This will happen when the copy fails, which we expect if there is no
			// image file. It isn't worth reporting.
			File resourceFolder = ProcessingCore.getProcessingCore().getPluginResourceFolder();
			try {
				File exportResourcesFolder = new File(resourceFolder, "export");
				File loadingImageCoreResource = new File(exportResourcesFolder, LOADING_IMAGE);
				Utilities.copyFile(loadingImageCoreResource, new File(exportFolder.getFullPath().toString(), LOADING_IMAGE));
			} catch (Exception ex) {
				// This is not expected, and should be reported, because we are about to bail
				ProcessingLog.logError("Could not access the Processing Plug-in Core resources. " +
						"Export aborted.", ex);
				return false;
			}
		}
		
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
		
		// Get the compiled source and package it as a jar
//		Shell parentShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
//	    JarPackageData codePackageData = new JarPackageData();
//	    codePackageData.setJarLocation(sp.getAppletFolder().getFullPath().append(sp.getProject().getName() + ".jar"));
//	    codePackageData.setSaveManifest(true);
//        description.setManifestMainClass(mainType);
//        description.setElements(filestoExport);
//        IJarExportRunnable runnable= description.createJarExportRunnable(parentShell);
//        try {
//            new ProgressMonitorDialog(parentShell).run(true,true, runnable);
//        } catch (InvocationTargetException e) {
//            // An error has occurred while executing the operation
//        } catch (InterruptedException e) {
//            // operation has been canceled.
//        }

		// java.io has changed things, so force the workspace to refresh or everything will disappear
		try {
			sp.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			ProcessingLog.logError("The workspace could not refresh after the export wizard ran. " +
					"You may need to manually refresh the workspace to continue.", e);
		}
		
		//DEBUG
		ProcessingLog.logError("Could not export " + sp.getProject().getName() + " because the exporter is not finished.", null);
		return false;
	}

}
