package org.processing.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.processing.editor.ProcessingEditorPlugin;
import org.processing.editor.ProcessingLog;

public class ProcessingSketchNature implements IProjectNature {

	private static final String NATURE_ID = "org.processing.editor.processingSketchNature";
	private IProject project;
	
	/**
	 * Access method for this natures project
	 */
	public IProject getProject() {
		return project;
	}
	
	/**
	 * Sets the project this nature is managing
	 */
	public void setProject(IProject project) {
		this.project = project;
	}
	
	/**
	 * associate the processing sketch builder with the project
	 */
	public void configure() throws CoreException {
		ProcessingSketchAuditor.addBuilderToProject(project);
		new Job("Processing Sketch Audit"){
			protected IStatus run(IProgressMonitor monitor){
				try{
					project.build(
							ProcessingSketchAuditor.FULL_BUILD,
							ProcessingSketchAuditor.BUILDER_ID,
							null,
							monitor);
				} catch (CoreException e){
					ProcessingLog.logError(e);
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	/**
	 * dissociate the processing sketch builder from the project
	 * and remove the markers it generated.
	 */
	public void deconfigure() throws CoreException {
		ProcessingSketchAuditor.removeBuilderFromProject(project);
		ProcessingSketchAuditor.deleteProblemMarkers(project);
	}
	
	public static void addNature(IProject project){
		// Cannot modify closed projects.
		if (!project.isOpen())
			return;
		
		// Get the description
		IProjectDescription description;
		
		try {
			description = project.getDescription();
		}
		catch (CoreException e){
			ProcessingLog.logError(e);
			return;
		}
		
		// If the project already has this nature we're done, otherwise keep going
		List<String> newIds = new ArrayList<String>();
		newIds.addAll(Arrays.asList(description.getNatureIds()));
		int index = newIds.indexOf(NATURE_ID);
		if (index != -1)
				return;
		
		// Add the nature
		newIds.add(NATURE_ID);
		description.setNatureIds(newIds.toArray(new String[newIds.size()]));
		
		// Save the description
		try {
			project.setDescription(description, null);
		} catch(CoreException e){
			ProcessingLog.logError(e);
		}
	}

	/** 
	 * Tests a project to see if it has the Processing nature
	 * 
	 * IProject project to test
	 * returns true if the project has the processing nature 
	 */
	public static boolean hasNature(IProject project){
		try{
			return project.isOpen() && project.hasNature(NATURE_ID);
		} catch (CoreException e){
			ProcessingLog.logError(e);
			return false;
		}
	}
	
	public static void removeNature(IProject project){
		// Cannot modify closed projects
		if (!project.isOpen())
			return;
		
		// Get the description
		IProjectDescription description;
		try {
			description = project.getDescription();
		} catch (CoreException e){
			ProcessingLog.logError(e);
			return;
		}
		
		// Determine if the project has the nature
		List<String> newIds = new ArrayList<String>();
		newIds.addAll(Arrays.asList(description.getNatureIds()));
		int index = newIds.indexOf(NATURE_ID);
		if (index == -1)
				return;
		
		// Remove the nature
		newIds.remove(index);
		description.setNatureIds(newIds.toArray(new String[newIds.size()]));
		
		// Save the description
		try {
			project.setDescription(description,null);
		} catch (CoreException e){
			ProcessingLog.logError(e);
		}
	}
	
}
