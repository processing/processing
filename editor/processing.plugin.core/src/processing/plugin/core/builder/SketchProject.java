package processing.plugin.core.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
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

		// Now make sure the build order is right. 
		// Adding the Java nature just screwed it up and configure() is done running
		List<ICommand> newCmds = new ArrayList<ICommand>();
		newCmds.addAll(Arrays.asList(description.getBuildSpec()));
		int ploc = -1;
		for (int i = 0; i < newCmds.size(); i++){
			if (newCmds.get(i).getBuilderName().equals(SketchBuilder.BUILDER_ID))
				ploc = i;
		}
		if (ploc > 0) { // set processing builder to be first up
			ICommand command = newCmds.remove(ploc);
			newCmds.add(0, command);
			description.setBuildSpec(
				(ICommand[]) newCmds.toArray(new ICommand[newCmds.size()])
			);
		}
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
		getBuildFolder();
		getJavaBuildFolder();
		
		
		// Check the description to see if it already has the builder
		IProjectDescription description = project.getDescription();
		List<ICommand> newCmds = new ArrayList<ICommand>();
		newCmds.addAll(Arrays.asList(description.getBuildSpec()));

		// Remove all instances of the builder that may be in the build order
		int ploc = 0; // builder ID location
		while(ploc > -1){
			ploc = -1; // -1 is not found.
			for (int i = 0; i < newCmds.size(); i++){
				if (newCmds.get(i).getBuilderName().equals(SketchBuilder.BUILDER_ID))
					ploc = i;
			}
			if (ploc > -1)	newCmds.remove(ploc);
		}
		
		//  Now that we're sure it's gone, add a single instance to the front of the list
		ICommand command = description.newCommand();
		command.setBuilderName(SketchBuilder.BUILDER_ID);
		newCmds.add(0, command);
		description.setBuildSpec(
				(ICommand[]) newCmds.toArray(new ICommand[newCmds.size()]));
		project.setDescription(description,null);

		// refresh the local space, folders may have been created
		updateClasspathEntries(null, null); // we don't have anything for the classpath yet, but we can set some basics.
//		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	}

	/**
	 * Dissociate the processing sketch builder from this nature's
	 * project and remove the markers it generated.
	 */
	public void deconfigure() throws CoreException {
		if (!project.isOpen())
			return;

		IProjectDescription description = project.getDescription();
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
	 * Return a vanilla File handle to the output folder for the Java compiler or null if it cannot be created. 
	 * <p>
	 * Users shouldn't be messing with this folder unless they know what they're doing.
	 * Unfortunately, JDT requires the build folder to be in the project file system 
	 * somewhere, so we can't hide it in a system temp folder like the PDE does.
	 */
	public IFolder getJavaBuildFolder(){
		try {
			IFolder preproc = getBuildFolder();
			if(preproc != null){
				IFolder parent = (IFolder) preproc.getParent();
				IFolder compile = parent.getFolder("compiler");
				if(!compile.exists())
					compile.create(IResource.NONE, true, null);
				return compile;
			}
		} catch (Exception e) {
			ProcessingLog.logError("Compiler folder could not be created.", e);
		}
		return null;
	}
	
	/**
	 * Returns a handle to the project specific build folder or null if it doesn't exist
	 * <p>
	 * Users shouldn't be messing with this unless they know what they're doing.
	 * Unfortunately, JDT requires the build folder to be in the project file system 
	 * somewhere, so we can't hide this in a system temp folder like the PDE does. 
	 */
	public IFolder getBuildFolder(){
		try {
			IFolder tempBuildFolder = project.getFolder("generated");
			if(!tempBuildFolder.exists())
				tempBuildFolder.create(IResource.NONE, true, null);
			IFolder preprocBuildFolder = tempBuildFolder.getFolder("preproc");
			if(!preprocBuildFolder.exists())
				preprocBuildFolder.create(IResource.NONE, true, null);
			return preprocBuildFolder;
		} catch (Exception e) {
			ProcessingLog.logError("Preproc folder could not be created.", e);
		}
		return null;
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
		
		// Processing Libraries
		IPath plibs = new Path(ProcessingCore.getProcessingCore().getCoreJarFile().getAbsolutePath());
		entries.add(JavaCore.newLibraryEntry( plibs, null, null, false ));
		
		// if we were given a list of source folders, add them to the list
		// this should include the build folder and the code folder, if it was necessary
		if(srcFolderPathList != null){
			for( IPath p : srcFolderPathList){
				if (p!=null) entries.add(JavaCore.newSourceEntry(p.makeAbsolute())); 
			}
		}
		
		if(libraryJarPathList != null){
			for(IPath p : libraryJarPathList){
				//System.out.println(p.toString());
				if (p != null){
					entries.add(
						JavaCore.newLibraryEntry(p.makeAbsolute(),
							null, // no source
							null, // no source
							false // not exported
						)
					);
				}
			}
		}

		// things are added in no particular order		
		IClasspathEntry[] classpathEntries = new IClasspathEntry[entries.size()];
		
		int i = 0;
		for (IClasspathEntry cpe : entries){
			classpathEntries[i++] = cpe;
//			System.out.println(cpe.toString());
		}
		
//		for(IClasspathEntry cpe : entries){
//			try{
//				//System.out.println("Trying: " + cpe.toString());
//				jproject.setRawClasspath(new IClasspathEntry[] {cpe}, null);
//			} catch (Exception e){
//				System.out.println("It was broken. Ugh.");
//			}
//		}		
		
		try {
			jproject.setOutputLocation( getJavaBuildFolder().getFullPath(), null);
			jproject.setRawClasspath(classpathEntries, null);
//			jproject.setRawClasspath(new IClasspathEntry[0], null);
		} catch (Exception e) {
			ProcessingLog.logError("There was a problem setting the compiler class path.", e);
		}
		
		Hashtable options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_ANNOTATION_SUPER_INTERFACE, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_AUTOBOXING, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_CHAR_ARRAY_IN_STRING_CONCATENATION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_DEPRECATION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_DISCOURAGED_REFERENCE, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_EMPTY_STATEMENT, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_FALLTHROUGH_CASE, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_FINAL_PARAMETER_BOUND, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_FINALLY_BLOCK_NOT_COMPLETING, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_HIDDEN_CATCH_BLOCK, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_INCOMPATIBLE_NON_INHERITED_INTERFACE_METHOD, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_INCOMPLETE_ENUM_SWITCH, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_INVALID_JAVADOC, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_INVALID_JAVADOC_TAGS, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_INVALID_JAVADOC_TAGS__DEPRECATED_REF, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_METHOD_WITH_CONSTRUCTOR_NAME, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS_VISIBILITY, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_MISSING_SERIAL_VERSION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_NULL_REFERENCE, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_REDUNDANT_NULL_CHECK, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_PARAMETER_ASSIGNMENT, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_SPECIAL_PARAMETER_HIDING_FIELD, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_SUPPRESS_WARNINGS, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_TYPE_PARAMETER_HIDING, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNDOCUMENTED_EMPTY_BLOCK, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNNECESSARY_ELSE, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNUSED_LABEL, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER_WHEN_IMPLEMENTING_ABSTRACT, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER_WHEN_OVERRIDING_CONCRETE, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_VARARGS_ARGUMENT_NEED_CAST, JavaCore.IGNORE);
		// its finally over! yeah!
		
		JavaCore.setOptions(options);		
	}
}