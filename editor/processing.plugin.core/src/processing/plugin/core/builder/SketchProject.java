package processing.plugin.core.builder;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

import processing.plugin.core.ProcessingCore;
import processing.plugin.core.ProcessingCorePreferences;
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

		// Setup the folders
		IFolder codeFolder = project.getFolder("code");
		IFolder dataFolder = project.getFolder("data");
		IFolder buildFolder = project.getFolder("bin"); // TODO relocate to MyPlugin.getPlugin().getStateLocation().getFolder("bin")
		IFolder appletFolder = project.getFolder("applet");
		IFolder javaBuildFolder = project.getFolder("compile");
		
		File coreResources = getCoreResourcesFolder(); // includes core Libs
		File sketchbookLibs = getSketchBookLibsFolder(project);
		
		if(!codeFolder.exists())
			buildFolder.create(IResource.NONE, true, null);
		if(!dataFolder.exists())
			dataFolder.create(IResource.NONE, true, null);
		if(!buildFolder.exists())
			buildFolder.create(IResource.NONE, true, null);
		if(!appletFolder.exists())
			appletFolder.create(IResource.NONE, true, null);
		if(!javaBuildFolder.exists())
			javaBuildFolder.create(IResource.NONE, true, null);
		
		// Setup the Java project underlying the Sketch
		IJavaProject jproject = JavaCore.create(project);
		
		// Get a default VM to toss in the mix
		IPath containerPath = new Path(JavaRuntime.JRE_CONTAINER);
		IVMInstall vm = JavaRuntime.getDefaultVMInstall();
		IPath vmPath = containerPath.append(vm.getVMInstallType().getId()).append(vm.getName());
				
		// Setup dynamic classpath containers so we don't have to recalculate them and set them every time	
		List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
		
		entries.add(JavaCore.newContainerEntry(vmPath.makeAbsolute())); // JVM
		entries.add(JavaCore.newSourceEntry(buildFolder.getFullPath().makeAbsolute())); // java source
		entries.add(JavaCore.newContainerEntry(codeFolder.getFullPath().makeAbsolute())); // data source
		if(coreResources != null)
			entries.add(JavaCore.newContainerEntry(new Path(coreResources.getAbsolutePath()))); // core libs container
		if(sketchbookLibs != null)
			entries.add(JavaCore.newContainerEntry(new Path(sketchbookLibs.getAbsolutePath()))); // sketchbook libs container

		// casting doesn't work so we have to explicitly unpack the list.
		IClasspathEntry[] classpathEntries = new IClasspathEntry[entries.size()];
		for(int i=0; i< entries.size(); i++ ){
			classpathEntries[i] = entries.get(i);
		}
		
		// Combine all of these entries and set the raw classpath of the project.
		// None of these should require further modification because they are dynamic
		// Also provide an explicit output folder and a null progress monitor
		jproject.setRawClasspath( classpathEntries, javaBuildFolder.getFullPath(), null);
		
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
		
		// refresh the local space, folders were created
		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		
		// schedule and run a build once its added.
		/*new Job("Build Sketch"){
			protected IStatus run(IProgressMonitor monitor){
				try{
					project.build(
							IncrementalProjectBuilder.FULL_BUILD,
							SketchBuilder.BUILDER_ID,
							null,
							monitor);
				} catch (CoreException e){
					ProcessingLog.logError(e);
				}
				return Status.OK_STATUS;
			}
		}.schedule();*/

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
	 * Sets a persistent property in the pojects property store
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
	

	/**
	 * Finds the folder containing the Processing core libraries, which are bundled with the
	 * plugin. This folder doesn't exist in the workspace, so we return it as a File, not IFile. 
	 * If something goes wrong, logs an error and returns null.
	 *  
	 * @return File containing the core libraries folder or null
	 */
	public File getCoreLibsFolder() {
		URL fileLocation = ProcessingCore.getProcessingCore().getPluginResource("libraries");
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
	 * Resolves the plug-in resources folder to a File and returns it. This will include the
	 * Processing libraries and the core libraries folder.
	 * 
	 * @return File reference to the core resources
	 */
	public File getCoreResourcesFolder(){
		URL fileLocation = ProcessingCore.getProcessingCore().getPluginResource("");
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
	 * Find the folder containing the users libraries, which should be in the sketchbook.
	 * Looks in the user's preferences first, then look relative to the sketch location.
	 * 
	 * @return File containing the Sketch book library folder, or null if it can't be located
	 */
	public File getSketchBookLibsFolder(IProject proj) {
		IPath sketchbook = ProcessingCorePreferences.current().getSketchbookPath();
		if (sketchbook == null)
			sketchbook = findSketchBookLibsFolder(proj);
		if (sketchbook == null)
			return null;
		return new File(sketchbook.toOSString());
	}

	/**
	 * Tries to locate the sketchbook library folder relative to the project path
	 * based on the default sketch / sketchbook setup. If such a folder exists, loop
	 * through its contents until a valid library is found and then return the path
	 * to the sketchbook. If no valid libraries are found (empty folder, improper 
	 * sketchbook setup), or if no valid folder is found, return null.
	 * 
	 * @return IPath containing the location of the new library folder, or null
	 */
	public IPath findSketchBookLibsFolder(IProject proj) {
		try{
			IPath guess = proj.getLocation().removeLastSegments(1).append("libraries");
			File folder = new File(guess.toOSString());
			if(folder.isDirectory())
				for( File file : folder.listFiles()){
					if(file.isDirectory())
						if (ProcessingCore.isLibrary(file))
							return guess;
				}
		} catch (Exception e){
			ProcessingLog.logError(e);
		}
		return null;
	}
	
}
