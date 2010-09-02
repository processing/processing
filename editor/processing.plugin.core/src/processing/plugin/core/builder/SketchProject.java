package processing.plugin.core.builder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

import processing.plugin.core.ProcessingCore;
import processing.plugin.core.ProcessingLog;

public class SketchProject implements IProjectNature {

	/** value: <code>"processing.plugin.core.processingnature"</code> */
	public static final String NATURE_ID = ProcessingCore.PLUGIN_ID + ".sketchNature";

	/** The basic project entry being managed */
	protected IProject project;


	/** 
	 * Return the SketchProject associated with the given IProject, or null
	 * if the project is not associated with a SketchProject.
	 */
	public static SketchProject forProject(IProject project){
		IProjectNature nature = null;
		try{
			nature = project.getNature(NATURE_ID);
		} catch (CoreException e){
			return null;
		}
		return (SketchProject) nature;
	}

	/** True if the project has the SketchProject nature, false otherwise. */
	public static boolean isSketchProject(IProject project){
		return SketchProject.forProject(project) != null;
	}

	/** 
	 * Add the Sketch Project nature to the front of the nature list if the
	 * project doesn't already have it as a nature, or move it to the front
	 * of the nature list if it does.
	 */
	public static void addSketchNature(IProject project) throws CoreException{
		if (!project.isOpen())
			return;

		if (SketchProject.isSketchProject(project)){
			IProjectDescription description = project.getDescription();
			List<String> newIds = new ArrayList<String>();
			newIds.addAll(Arrays.asList(description.getNatureIds()));
			newIds.remove(NATURE_ID);
			newIds.add(0,NATURE_ID);
			description.setNatureIds(newIds.toArray(new String[newIds.size()]));
			return;
		}

		IProjectDescription description = project.getDescription();

		List<String> newIds = new ArrayList<String>();
		newIds.add(NATURE_ID); // front of the line
		newIds.add(JavaCore.NATURE_ID); // add the nature ID afterwards
		newIds.addAll(Arrays.asList(description.getNatureIds()));
		description.setNatureIds(newIds.toArray(new String[newIds.size()]));

		project.setDescription(description, null);
	}

	/** Removes the sketch and java natures from a project */
	public static void removeSketchNature(IProject project) throws CoreException{
		if (!project.isOpen())
			return;

		// doesn't have the nature, we're done
		if (!SketchProject.isSketchProject(project))
			return;

		IProjectDescription description = project.getDescription();

		List<String> newIds = new ArrayList<String>();
		newIds.addAll(Arrays.asList(description.getNatureIds()));
		newIds.remove(newIds.indexOf(NATURE_ID));
		if (newIds.contains(JavaCore.NATURE_ID))
			newIds.remove(newIds.indexOf(JavaCore.NATURE_ID));
		description.setNatureIds(newIds.toArray(new String[newIds.size()]));

		project.setDescription(description,null);
	}

	/** Access method for this nature's project */
	public IProject getProject() {
		return project;
	}

	/** 
	 * Should not be triggered by clients.
	 * Sent by the NatureManager when the nature is added to a project.
	 */
	public void setProject(IProject project) {
		this.project = project;
	}

	/** Access method for this nature's java project */
	public IJavaProject getJavaProject(){
		return JavaCore.create(project);
	}

	/** Associate the sketch builder with this nature's project */
	public void configure() throws CoreException {
		if (!project.isOpen())
			return;

		getCodeFolder();
		getDataFolder();
		
		// Check the description to see if it already has the builder
		IProjectDescription description = this.project.getDescription();
		List<ICommand> newCmds = new ArrayList<ICommand>();
		newCmds.addAll(Arrays.asList(description.getBuildSpec()));

		int ploc = -1; // builder ID location
		for (int i = 0; i < newCmds.size(); i++){
			if (newCmds.get(i).getBuilderName().equals(SketchBuilder.BUILDER_ID))
				ploc = i;
		}		

		if (ploc == 0) // its there and where we want it
			return;

		if (ploc > 0)
			newCmds.remove(ploc); // its not where we want it, remove it and add to the beginning

		ICommand command = description.newCommand();
		command.setBuilderName(SketchBuilder.BUILDER_ID);
		newCmds.add(0, command);
		description.setBuildSpec(
				(ICommand[]) newCmds.toArray(new ICommand[newCmds.size()]));
		project.setDescription(description,null);

		// refresh the local space, folders may have been created
		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	}

	/**
	 * Dissociate the processing sketch builder from this nature's
	 * project and remove the markers it generated.
	 */
	public void deconfigure() throws CoreException {
		if (!project.isOpen())
			return;

		IProjectDescription description = this.project.getDescription();
		ICommand[] cmds = description.getBuildSpec();
		for (int i=0; i<cmds.length; i++){
			if (cmds[i].getBuilderName().equals(SketchBuilder.BUILDER_ID)){
				List<ICommand> newCmds = new ArrayList<ICommand>();
				newCmds.addAll(Arrays.asList(cmds));
				newCmds.remove(i);
				description.setBuildSpec(newCmds.toArray(new ICommand[newCmds.size()]));
			}
		}

		// clean up your tokens
		SketchBuilder.deleteP5ProblemMarkers(project);
	}

	/** Returns a qualified name for the property relative to this plug-in */
	public QualifiedName getPropertyName(String localName){
		return new QualifiedName(ProcessingCore.PLUGIN_ID, localName);
	}

	/** 
	 * Sets a persistent property in the poject's property store
	 * If there is an exception it will be reported and the project
	 * will not persist.
	 **/
	public void setPersistentProperty(String localName, String value){
		try{
			this.project.setPersistentProperty(this.getPropertyName(localName), value);
		} catch (CoreException e){
			ProcessingLog.logError(e);
		}		
	}

	/** Trigger a full build of the project being managed */
	public void fullBuild(IProgressMonitor monitor) throws CoreException{
		project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
	}
	
	/** Return the sketch's code folder or null if it cannot be retrieved */
	public IFolder getCodeFolder(){
		try{
			IFolder code = project.getFolder("code");
			if(!code.exists())
				code.create(IResource.NONE, true, null);
			return code;
		} catch (Exception e){
			ProcessingLog.logError("Code folder could not be created.", e);
			return null;
		}
	}

	/** Return the sketch's code folder or null if it cannot be retrieved */
	public IFolder getDataFolder(){
		try{
			IFolder data = project.getFolder("data");
			if(!data.exists())
				data.create(IResource.NONE, true, null);
			return data;
		} catch (Exception e){
			ProcessingLog.logError("Data folder could not be created.", e);
			return null;
		}
	}
	
	/** Return the sketch's code folder or null if it cannot be retrieved. */
	public IFolder getAppletFolder(){
		try{
			IFolder applet = project.getFolder("applet");
			if(!applet.exists())
				applet.create(IResource.NONE, true, null);
			return project.getFolder("applet");
		} catch (Exception e){
			ProcessingLog.logError("Applet folder could not be created.", e);
			return null;
		}
	}

	/** 
	 * Return a vanilla File handle to the output folder for the Java compiler
	 * inside the cached build folder, or null if it cannot be created.  
	 */
	public File getJavaBuildFolder(){
		File build = getBuildFolder();
		if(build != null){
			File compile = new File(build, "compile");
			compile.mkdirs();
			if(compile.exists())
				return compile;
		}
		ProcessingLog.logError("Could not create the temporary build (compile) folder.", null);
		return null;
	}
	
	/**
	 * Returns a vanilla File handle to the project specific build folder, which is located inside
	 * the temp directory of the plug-in's local cache, or null if it doesn't exist
	 */
	public File getBuildFolder(){
		//File tempBuildFolder = File.createTempFile("")
		File tempBuildFolder = new File(ProcessingCore.getProcessingCore().getPluginTempFolder(), project.getName());
		tempBuildFolder.mkdirs();
		if(tempBuildFolder.exists())
			return tempBuildFolder;
		ProcessingLog.logError("Could not create the temporary build (preprocess) folder.", null);
		return null;
		//return project.getFolder("bin").getFullPath().toFile();
	}

	/**
	 * Register a new set of class path entries from the provided source folder list and library jars.
	 * This method completely replaces the old class path.
	 * 
	 * @param srcFolderPathList A list of absolute paths to source folders
	 * @param libraryJarPathList A list of absolute paths to source folders
	 */
	public void updateClasspathEntries( IPath[] srcFolderPathList,	IPath[] libraryJarPathList) {
		IJavaProject jproject = this.getJavaProject();
		
		// Get a default VM to toss in the mix
		IPath containerPath = new Path(JavaRuntime.JRE_CONTAINER);
		IVMInstall vm = JavaRuntime.getDefaultVMInstall();
		IPath vmPath = containerPath.append(vm.getVMInstallType().getId()).append(vm.getName());

		// Duplicate entries cause errors, so prep them with a set
		HashSet<IClasspathEntry> entries = new HashSet<IClasspathEntry>();
		
		// VM
		entries.add(JavaCore.newContainerEntry(vmPath.makeAbsolute())); // JVM
		
		// if we were given a list of source folders, add them to the list
		// this should include the build folder and the code folder, if it was necessary
		if(srcFolderPathList != null){
			for( IPath p : srcFolderPathList){
				entries.add(JavaCore.newSourceEntry(p.makeAbsolute())); 
			}
		}
		
		if(libraryJarPathList != null){
			for(IPath p : libraryJarPathList){
				//System.out.println(p.toString());
				entries.add(
					JavaCore.newLibraryEntry(p.makeAbsolute(),
						null, // no source
						null, // no source
						false // not exported
					)
				);
			}
		}

		// things are added in no particular order
		
		
		IClasspathEntry[] classpathEntries = new IClasspathEntry[entries.size()];
		
		int i = 0;
		for (IClasspathEntry cpe : entries) classpathEntries[i++] = cpe;

		// Combine all of these entries and set the raw classpath of the project.
		// None of these should require further modification because they are dynamic
		// Also provide an explicit output folder and a null progress monitor
		try {
			jproject.setRawClasspath( classpathEntries, new Path(this.getJavaBuildFolder().getCanonicalPath()), null);
		} catch (Exception e) {
			ProcessingLog.logError("There was a problem setting the compiler class path.", e);
		}
		
	}

}