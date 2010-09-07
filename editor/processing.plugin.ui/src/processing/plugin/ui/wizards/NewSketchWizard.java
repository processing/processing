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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import processing.plugin.core.builder.SketchProject;
import processing.plugin.ui.wizards.NewSketchWizardPage;

/**
 * A wizard to create a new Processing sketch project.
 * <p>
 * Contains a single page the requests the name of the sketch
 * and the sketch book folder to put the sketch in.
 * 
 * @author lonnen
 */
public class NewSketchWizard extends Wizard implements INewWizard {
	
	/** The single page in the wizard */
	private NewSketchWizardPage page;
	/** The project to be created */
	private IProject project;

	/** Constructor */
	public NewSketchWizard(){}
	
	/** There is not init code. */
	public void init(IWorkbench workbench, IStructuredSelection selection) {}
	
	/**
	 * When the finish button is called, create the project
	 * <p>
	 * The wizard page ensures that the user cannot click finish
	 * without a valid directory and sketch name. The creation of the
	 * project is wrapped in a runnable object so it can be monitored 
	 * and potentially canceled by the user.
	 */
	public boolean performFinish() {
		
		final String sketchName = page.getSketchName();
		final IPath sketchbookPath = page.getSketchbookLoc().append(sketchName);
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(sketchName);
		
		final IProjectDescription sketchDescription = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
		sketchDescription.setLocation(sketchbookPath);
		
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
	

	/**
	 * Creates the project in the workspace
	 * 
	 * @param description
	 * @param proj
	 * @param monitor
	 * @throws CoreException
	 * @throws OperationCanceledException
	 */
	protected void createNewProject(IProjectDescription description, IProject proj,
			IProgressMonitor monitor) throws CoreException, OperationCanceledException{
		monitor.beginTask("Creating a new Sketch", 400);
		
		try{
			// create the project root
			proj.create(description, new SubProgressMonitor(monitor, 100));			
			proj.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 100));

			if (monitor.isCanceled()){throw new OperationCanceledException();}

			IContainer container = (IContainer) proj;
			//create the default sketch file
			addFileToContainer(container, new Path(project.getName() + ".pde"), null, new SubProgressMonitor(monitor, 100));
		} finally{
			monitor.done();
		}
		
		// adding the nature will setup the folder structure
		SketchProject.addSketchNature(proj);
	}

	/**
	 * Adds a new file to a project.
	 * If no content stream is provided, a default file will be loaded
	 * with setup and draw methods.
	 * 
	 * @param container where to add the file
	 * @param path the path to the file
	 * @param contentStream what to put in the file
	 * @param monitor report the progress back to the user
	 * @throws CoreException if there are problems with the resource
	 */
	private void addFileToContainer(IContainer container, Path path, InputStream contentStream, IProgressMonitor monitor) throws CoreException{
		final IFile file = container.getFile(path);
		if (contentStream == null){
			contentStream = new ByteArrayInputStream(
					("void setup(){ \n" +
					"\tsize(200,200); \n" +
					"} \n \n" +
					"void draw(){ \n" +
					"\tellipse(50,50,80,80); \n" +
					"} ").getBytes());
		}
		if(file.exists()){
			file.setContents(contentStream, true, true, monitor);
		} else{ 
			file.create(contentStream, true, monitor); 
		}
		monitor.done();
	}
	
	/**
	 * Initializes the single page and adds it to the wizard.
	 */
	public void addPages(){
		setWindowTitle("New Sketch Wizard");
		page = new NewSketchWizardPage();
		addPage(page);
	}
		
}
