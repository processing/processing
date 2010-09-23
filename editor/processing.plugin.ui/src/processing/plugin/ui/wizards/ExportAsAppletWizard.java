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
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

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
	 * Tries to export the sketch as an applet
	 * returns whether or not it was successful
	 */
	public boolean exportAsApplet(SketchProject sp) {
		if (!sp.wasLastBuildSuccessful()) return false;
		if (!sp.getProject().isAccessible()) return false;
		
		IFile code = sp.getMainFile();
		if (code == null) return false;
		
		// true to nuke the folder contents, if they exist
		IFolder exportFolder = sp.getAppletFolder(true);
		
		// if the code folder has stuff, move it to the applet folder
		IFolder codeFolder = sp.getCodeFolder();
		if(codeFolder != null){
			try{
		        String includes = Utilities.contentsToClassPath(codeFolder.getFullPath().toFile());
		        String[] codeList = Utilities.splitTokens(includes,File.separator);
		        for (String path : codeList){
		        	System.out.println("Found file: " + path + ".");
		        	if (path.toLowerCase().endsWith(".jar") ||
		        			path.toLowerCase().endsWith(".zip")) {
		        		IFile exportFile = codeFolder.getFile(new Path(path));
		        		if (exportFile.isAccessible()){
		        			exportFile.copy(exportFolder.getFullPath(), true, null);
		        			System.out.println("Copied the file to " + exportFolder.getFullPath().toString() + " .");
		        		}
		        	} 
		        }
			} catch (CoreException e) {
				ProcessingLog.logError("Could not export. CoreException while packaging the code folder.", e);
				return false;
			}
		}
		////
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

		
//		int wide = this.getWidth();
//		int high = this.getHeight();
//		
//		String codeContents = Utilities.readFile(code);
//		
//		String description ="";
//		String[] javadoc = Utilities.match(codeContents, "/\\*{2,}(.*)\\*+/");
//		if (javadoc != null){
//			StringBuffer dbuffer = new StringBuffer();
//			String[] pieces = Utilities.split(javadoc[1], '\n');
//			for (String line : pieces){
//				// if this line starts with * characters, remove em
//				String[] m = Utilities.match(line, "^\\s*\\*+(.*)");
//				dbuffer.append(m != null ? m[1] : line);
//				dbuffer.append('\n');
//			}
//			description = dbuffer.toString();
//			ProcessingLog.logInfo(description);
//		}
		
		//DEBUG
		ProcessingLog.logError("Could not export " + sp.getProject().getName() + " because the exporter is not finished.", null);
		return false;
	}

}
