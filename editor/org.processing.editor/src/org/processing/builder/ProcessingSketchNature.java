package org.processing.builder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.processing.editor.ProcessingLog;

public class ProcessingSketchNature implements IProjectNature {

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
		
	}

}
