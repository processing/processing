package processing.plugin.core.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import processing.plugin.core.ProcessingCore;
import processing.plugin.core.ProcessingLog;

public class SketchNature implements IProjectNature {

	/** The project this <code>SketchNature</code> is modifying */
	protected IProject project;
	
	/** Access method for this natures project */
	public IProject getProject() {
		return project;
	}
	
	/** Set the project this nature is managing */
	public void setProject(IProject project) {
		this.project = project;
	}
	
	/** Associate the sketch builder with this nature's project */
	public void configure() throws CoreException {
		ProcessingCore.addBuilderToProject(project);
		new Job("Build Sketch"){
			protected IStatus run(IProgressMonitor monitor){
				try{
					project.build(
							IncrementalProjectBuilder.FULL_BUILD,
							ProcessingCore.BUILDER_ID,
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
	 * Dissociate the processing sketch builder from this nature's
	 * project and remove the markers it generated.
	 */
	public void deconfigure() throws CoreException {
		ProcessingCore.removeBuilderFromProject(project);
		SketchBuilder.deleteP5ProblemMarkers(project);
	}
	
	/**
	 * Adds the sketch nature to the project if it is accessible and
	 * does not already have the sketch nature.
	 * 
	 * @param project
	 */
	public static void addNature(IProject project){
		// Cannot modify closed projects.
		if (!project.isOpen())
			return;
		
		try { 
			// If it has the nature already, we're done
			if (project.hasNature(ProcessingCore.NATURE_ID))
				return;
			
			IProjectDescription description = project.getDescription();;
			
			List<String> newIds = new ArrayList<String>();
			newIds.addAll(Arrays.asList(description.getNatureIds()));
			newIds.add(ProcessingCore.NATURE_ID);
			description.setNatureIds(newIds.toArray(new String[newIds.size()]));
			
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
			return project.hasNature(ProcessingCore.NATURE_ID);
		} catch (CoreException e){
			// project doesn't exist or is not open
			return false;
		}
	}
	
	/**
	 * Removes the nature from the project if it has it
	 * 
	 * @param project
	 */
	public static void removeNature(IProject project){
		// Cannot modify closed projects
		if (!project.isOpen())
			return;
		try{
			// doesn't have the nature, we're done
			if (!project.hasNature(ProcessingCore.NATURE_ID))
				return;
			
			IProjectDescription description = project.getDescription();
			
			List<String> newIds = new ArrayList<String>();
			newIds.addAll(Arrays.asList(description.getNatureIds()));
			newIds.remove(newIds.indexOf(ProcessingCore.NATURE_ID));
			description.setNatureIds(newIds.toArray(new String[newIds.size()]));
			
			project.setDescription(description,null);

		} catch (CoreException e){
			ProcessingLog.logError(e);
			return;
		}
	}
}
