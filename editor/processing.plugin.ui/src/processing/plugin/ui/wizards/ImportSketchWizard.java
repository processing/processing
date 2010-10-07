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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import processing.plugin.core.builder.SketchProject;

/**
 * A wizard to import an existing Processing sketch project.
 * <p>
 * Contains a single page the requests the directory of the sketch
 * and runs a check to make sure that the sketch contains a *.pde
 * file with the same name as the directory before allowing import.
 * Also, warns the user if it does not appear to be in a sketchbook,
 * but does not require it to be.
 */
public class ImportSketchWizard extends Wizard implements IImportWizard {

	/** The single page in the wizard */
	private ImportSketchWizardPage page;
	
	/** The project to be created */
	private IProject project;
	
	/** A boring ol' constructor */
	public ImportSketchWizard() {}

	/** Does nothing */
	public void init(IWorkbench workbench, IStructuredSelection selection) {}

	/** Initialize the single page and add it to the wizard. */
	public void addPages(){
		setWindowTitle("New Sketch Wizard");
		page = new ImportSketchWizardPage();
		addPage(page);
	}
	
	/**
	 * When the finish button is clicked, create the project.
	 * <p>
	 * The wizard page validates the fields before the finish button 
	 * is enabled, so there is no validation here. The creation of the
	 * project is wrapped in a runnable object so it can be monitored 
	 * and potentially canceled by the user if it runs long and ties
	 * up the system.
	 */
	public boolean performFinish() {
		final IPath sketchPath = page.getSketchPath();
		final String sketchName = sketchPath.lastSegment();
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(sketchName);
		
		final IProjectDescription sketchDescription = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
		sketchDescription.setLocation(sketchPath);
		
		WorkspaceModifyOperation op = new WorkspaceModifyOperation(){
			protected void execute(IProgressMonitor monitor) throws CoreException{
				createNewProject(sketchDescription, project, monitor);
			}
		};
		
		try{
			getContainer().run(true, true, op);
		} catch (InvocationTargetException e){
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error!", realException.getMessage());
			return false;
		} catch (InterruptedException e){ // "shut it down"
			return false;
		}
		return true;
	}

	/** Creates the project in the workspace */
	protected void createNewProject(IProjectDescription description, IProject proj,
			IProgressMonitor monitor) throws CoreException, OperationCanceledException{
		monitor.beginTask("Creating a new Sketch", 2);
		try{
			proj.create(description, new SubProgressMonitor(monitor, 1));			
			proj.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1));
		} finally{
			monitor.done();
		}
		SketchProject.addSketchNature(proj);
	}
	
	
}