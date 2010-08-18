package processing.plugin.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Plugin;

/**
 * The plug-in runtime class containing the core (UI-free) support for Processing 
 * sketches.
 * <p>
 * Like all plug-in runtime classes (subclasses of <code>Plugin</code>), this class 
 * is automatically instantiated by the platform when the plug-in gets activated. 
 * Clients must not attempt to instantiate plug-in runtime classes directly.
 * </p>
 * <p>
 * The single instance of this class can be accessed from any plug-in declaring the 
 * Processing core plug-in as a prerequisite via 
 * <code>ProcessingCore.getProcessingCore()</code>. The Processing model plug-in 
 * will be activated automatically if it is not already active.
 * </p>
 */
public final class ProcessingCore extends Plugin {

	/* The shared instance */
	private static Plugin PROCESSING_CORE_PLUGIN = null;
	
	/** 
	 * The plug-in identifier of the Processing core support
	 * (value <code>"processing.plugin.core"</code>).
	 */
	public static final String PLUGIN_ID = "processing.plugin.core";
	
	/**
	 * The identifier for the Processing builder
	 * (value <code>"processing.plugin.core.processingbuilder"</code>).
	 */
	public static final String BUILDER_ID = PLUGIN_ID + ".sketchBuilder";
	
	/**
	 * The identifier for the Processing nature
	 * (value <code>"processing.plugin.core.processingnature"</code>).
	 * The presence of this nature indicates that it is a Processing Sketch.
	 */
	public static final String NATURE_ID = PLUGIN_ID + ".sketchNature";
	
	/** 
	 * Problem marker for processing build chain issues 
	 * (value <code>"processing.plugin.core.p5marker"</code>).
	 * These are just vanilla problem markers wrapped for easy identification. 
	 */
	public static final String MARKER_ID = ProcessingCore.PLUGIN_ID + ".p5marker";
	
	/**
	 * Creates the Processing core plug-in.
	 * <p>
	 * The plug-in instance is created automatically by the eclipse platform. 
	 * Clients must not call.
	 * </p>
	 * <p>
	 * Patterned after the JDT, this is public, but I'm not sure why. I thought
	 * singletons were supposed to have private constructors. [lonnen 09 09 2010]
	 */
	public ProcessingCore(){
		super();
		PROCESSING_CORE_PLUGIN = this;
	}
	
	/* 	public void start(BundleContext context) throws Exception { */
	/* 	public void stop(BundleContext context) throws Exception { */

	/** Returns the single instance of the Processing core plug-in runtime class.
	 *
	 *  @return the single instance of the Processing core plug-in runtime class
	 */
	public static ProcessingCore getProcessingCore(){
		return (ProcessingCore) PROCESSING_CORE_PLUGIN;
	}
	
	/**
	 * Adds the Sketch builder to a project
	 * <p>
	 * The preferred way to do this is with the Sketch nature. Even though this is
	 * public clients should consider using the Sketch nature instead of directly
	 * adding the builder.
	 * 
	 * @param project the project having the builder added to it
	 */
	public static void addBuilderToProject(IProject project){
		
		if (!project.isOpen())
				return;
		
		IProjectDescription description;
		try{
			description = project.getDescription();
		} catch (Exception e){
			ProcessingLog.logError(e);
			return;
		}
		
		// Look for builders already associated with the project
		ICommand[] cmds = description.getBuildSpec();
		for (int j = 0; j < cmds.length; j++){
			if (cmds[j].getBuilderName().equals(BUILDER_ID))
				return;
		}
		
		//Associate builder with project.
		ICommand newCmd = description.newCommand();
		newCmd.setBuilderName(BUILDER_ID);
		List<ICommand> newCmds = new ArrayList<ICommand>();
		newCmds.addAll(Arrays.asList(cmds));
		newCmds.add(newCmd);
		description.setBuildSpec(
			(ICommand[]) newCmds.toArray(new ICommand[newCmds.size()]));
		try{
			project.setDescription(description,null);
		} catch (CoreException e){
			ProcessingLog.logError(e);
		}		
	}
	
	/**
	 * Remove the Sketch builder from the project
	 * 
	 * If the builder is being managed by the sketch nature, calling this may cause
	 * problems with the nature life cycle. 
	 *
	 * 
	 * @param project the project whose build spec we are to modify
	 * @see processing.plugin.core.builder.SketchNature
	 */
	public static void removeBuilderFromProject(IProject project){
		
		if (!project.isOpen())
				return;
		
		IProjectDescription description;
		try{
			description = project.getDescription();
		} catch (Exception e){
			ProcessingLog.logError(e);
			return;
		}
		
		// Look for the builder
		int index = -1;
		ICommand[] cmds = description.getBuildSpec();
		for (int j = 0; j < cmds.length; j++){
			if (cmds[j].getBuilderName().equals(BUILDER_ID)){
				index = j;
				break;
			}
		}
		if (index == -1)
			return;
		
		//Remove builder with project.
		List<ICommand> newCmds = new ArrayList<ICommand>();
		newCmds.addAll(Arrays.asList(cmds));
		newCmds.remove(index);
		description.setBuildSpec(
			(ICommand[]) newCmds.toArray(new ICommand[newCmds.size()]));
		try{
			project.setDescription(description,null);
		} catch (CoreException e){
			ProcessingLog.logError(e);
		}				
	}
	
	
	
}
