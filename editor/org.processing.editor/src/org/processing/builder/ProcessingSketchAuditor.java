package org.processing.builder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.processing.editor.ProcessingEditorPlugin;
import org.processing.editor.ProcessingLog;

import processing.app.Preferences;
import processing.app.preproc.PdePreprocessor;
import processing.app.preproc.PreprocessResult;

/**
 * builder for Processing sketches. 
 * <p>
 * Preprocesses processing sketch files into java, and then runs the java files
 * through the JDT compiler. Errors returned are reflected back on the
 * source files.
 * <p>
 * Uses the PDE's sketch folder setup, with an additional /bin/ folder to save the
 * build state and intermediate files.
 * 
 * @author lonnen
 *
 */
public class ProcessingSketchAuditor extends IncrementalProjectBuilder {
	
	public static final String BUILDER_ID = ProcessingEditorPlugin.PLUGIN_ID + ".procesingSketchBuilder";
	
	/**
	 * Adds the processing builder to a project
	 * 
	 * @param project the project whose build spec we are to modify
	 */
	public static void addBuilderToProject(IProject project){
		
		//cannot modify closed projects
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
	 * Remove the processing builder from the project
	 * 
	 * @param project the project whose build spec we are to modify
	 */
	public static void removeBuilderFromProject(IProject project){
		
		//cannot modify closed projects
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
	
	/**
	 * Triggers a compile of the project.
	 * <p>
	 * Decides whether a full or incremental build is appropriate. Right now,
	 * the answer is always a full build. @see incrementalBuild
	 * 
	 * @param kind the build type
	 * @param args build arguments
	 * @param monitor let the user know things are happening
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
//		if (kind == IncrementalProjectBuilder.FULL_BUILD){
//			fullBuild(monitor);
//		} else {
//			IResourceDelta delta = getDelta(getProject());
//			if (delta==null){
//				fullBuild(monitor);
//			} else {
//				incrementalBuild(delta, monitor);
//			}
//		}
		System.out.println("BUILD!");

		// every build is a full build for now, @see incrementalBuild
		fullBuild(monitor);
		return null;
	}

	/**
	 * re-compile only the files that have changed
	 * 
	 * should be much faster than a full build, but because the preprocessor
	 * mashes together all the source files for a sketch, there is only one
	 * resource that gets changed every time a sketch is compiled. Until the
	 * preprocessor is rewritten and can mark up multiple files, every recompile 
	 * request will result in a full build. To save a little time, the build
	 * method has been modified to reflect this and never call the incremental
	 * builder, so we avoid the hassle of crawling the resources to try and
	 * identify which ones will be recomputed -- its safe to assume all of the
	 * files will be used because even the ones that are unchanged will be 
	 * grabbed eventually.
	 * 
	 * @param delta an object containing the resource changes
	 * @param monitor let the user know things are happening
	 */
	@SuppressWarnings("unused")
	private void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) {
		System.out.println("Incremental build on "+delta);
		try{
			delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta){
					System.out.println("changed: "+delta.getResource().getRawLocation());
					return true; // true visits children of the resource
					// this is important for folders
				}
			});
		} catch (CoreException e) {
			e.printStackTrace(); // perhaps we should pass this on
			//perhaps we should do something with it instead
		}
	}

	/**
	 * completely rebuilds the sketch file
	 * 
	 * this code is essentially a translation of the processing.core sketch
	 * code to the Eclipse platform. The same code cannot just be reused
	 * because we need to use the Eclipse virtual file system to access files
	 * from inside Eclipse.
	 * 
	 * @param monitor let the user know things are happening
	 * @throws CoreException if there are problems accessing the files
	 */
	private void fullBuild(IProgressMonitor monitor) throws CoreException {
		
		monitor.beginTask("Full Project Build", 4); // no idea how much 'work' to do here
		IProject proj = getProject(); // get the project

		if(checkCancel(monitor))
			return;
		
		if(!deleteProblemMarkers(proj))
			return;
		
		// IResource.members() doesn't return the files in a consistent order
		// so we get the list at the beginning of each build and use folderContents
		// whenever we need to get access to the source files during the build.
		IResource[] folderContents = proj.members(); //TODO make this location a preference, link to sketchbook
		
		// 1. concatenate all .pde files to the 'main' pde
		
		StringBuffer bigCode = new StringBuffer(); // this will hold the program
		int bigCount = 0; // how many lines are we talking about here?
		
		// without a SketchCode object, this field needs to be tracked independently
		int[] preprocOffsets = new int[folderContents.length];
		
		for(int i = 0; i < folderContents.length; i++){
			IResource file = folderContents[i];
			if(file.getFileExtension() == "pde"){ // filters out only .pde files
				String content = readFile((IFile) file);
				preprocOffsets[i] = bigCount;
				bigCode.append(content);
				bigCode.append("\n");
				bigCount += getLineCount(content);
			}
		}

		monitor.worked(1);
		if(checkCancel(monitor))
			return;
		
		spoof_preferences();// fake the preferences object.
		PdePreprocessor preproc = new PdePreprocessor(proj.getName(), 4); //TODO make tab size a preference?
		
		//final File java = new File(buildPath, name + ".java");
		IFolder outputFolder = proj.getFolder("bin"); // just a handle to the resource //TODO make the derived resources folder a preference
		if (!outputFolder.exists())
			outputFolder.create(IResource.NONE, true, null);
		
		PreprocessResult result = null;
		try{
			IFile outputFile = outputFolder.getFile(proj.getName() + ".java"); 
				
			StringWriter outputFileContents = new StringWriter();
			result = preproc.write(outputFileContents, bigCode.toString());
			
			// Ugh. It wants an InputStream
			ByteArrayInputStream inStream = new ByteArrayInputStream(outputFileContents.toString().getBytes());
			
			outputFile.create(inStream, true, monitor); // force flag = true means this should overwrite any existing files
			outputFile.setDerived(true); // let the platform know this is a generated file
			
		}catch(antlr.RecognitionException re){
			//TODO define the RecognitionException problem marker 
			
			// first assume that it's the main file
			int errorFile = 0;
			int errorLine = re.getLine() - 1;
			
			// then search through for anyone else whose preprocName is null,
			// since they've also been combined into the main pde
			for(int i = 1; i < folderContents.length; i++){
				IResource file = folderContents[i];
				if(file.getFileExtension() == "pde" && (preprocOffsets[i] < errorLine)){ 
					errorFile = i;
				}
			}
			errorLine -= preprocOffsets[errorFile];
						
			//DEBUG
			System.out.println("error line - error file - offset");
	      	System.out.println(errorLine + " - " + errorFile + " - " + preprocOffsets[errorFile]);
			
			String msg = re.getMessage();		
			
			if (msg.equals("expecting RCURLY, found 'null'")) {
		        // This can be a problem since the error is sometimes listed as a line
		        // that's actually past the number of lines. For instance, it might
		        // report "line 15" of a 14 line program. Added code to highlightLine()
		        // inside Editor to deal with this situation (since that code is also
		        // useful for other similar situations).
		        msg = "Found one too many { characters without a } to match it.";
		    }
		    if (msg.indexOf("expecting RBRACK") != -1) {
		    	msg = "Syntax error, maybe a missing right ] character?";
			}
		    if (msg.indexOf("expecting SEMI") != -1) {
		        msg = "Syntax error, maybe a missing semicolon?";
		    }
		    if (msg.indexOf("expecting RPAREN") != -1) {
		        msg = "Syntax error, maybe a missing right parenthesis?";
		    }
		    if (msg.indexOf("preproc.web_colors") != -1) {
		        msg = "A web color (such as #ffcc00) must be six digits.";
		    }
		    
		    // if there is no friendly translation, just report what you can
  			reportProblem(msg, (IFile) folderContents[errorFile], errorLine, true);
		
		} catch (Exception e){
			ProcessingLog.logError(e);
		}
		
		monitor.worked(1);
		if(checkCancel(monitor))
			return;
		
		// copy any .java files to the output directory
		for(int i = 0; i < folderContents.length; i++){
			IResource file = folderContents[i];
			if(file.getFileExtension() == "java"){ // copy .java files into the build directory
				folderContents[i].copy(outputFolder.getProjectRelativePath(), IResource.DERIVED, monitor);
			} else if (file.getFileExtension() == "pde"){
				// The compiler and runner will need this to have a proper offset
				preprocOffsets[i] += result.headerOffset;
			}
		}
		
		boolean foundMain = preproc.getFoundMain(); // is this still necessary?
		
		
		monitor.worked(1);
		if(checkCancel(monitor))
			return;
		
		//to the java batch compiler!
		//org.eclipse.jdt.core.compiler.CompilationProgress progress = null;
		
//		String baseCommand[] = new String[]{
//				"-Xemacs",
//			    //"-noExit",  // not necessary for ecj
//			    "-source", "1.5",
//			    "-target", "1.5",
//			    "-classpath", sketch.getClassPath(),
//			    "-nowarn", // we're not currently interested in warnings (works in ecj)
//			    "-d", buildPath // output the classes in the buildPath					
//		};
		
		//org.eclipse.jdt.core.compiler.batch.BatchCompiler.compile("-verbose", new PrintWriter(System.out), new PrintWriter(System.err), progress);
		// do something with it
		
		// finally, let the monitor know things are done
		monitor.done();
	}

	/**
	 * Try to delete all of the existing problem markers
	 * <p>
	 * This should also catch all markers that inherit from IMarker.PROBLEM,
	 * which includes all of the special marker types for Processing.	 *
	 * 
	 * @param project the project to be stripped of problem markers
	 * @return true if all markers were deleted, false if some remain
	 */
	protected static boolean deleteProblemMarkers(IProject project) {
		//TODO change this to remove markers specific to the Processing builder only
		// though that requires making Processing specific markers first
		try{
			project.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_INFINITE);
			return true;
		} catch (CoreException e) {
			ProcessingLog.logError(e);
			return false;
		}
	}

	/**
	 * utility method to read in a file and return it as a string
	 * 
	 * @param file a resource handler for a file
	 * @return contents of the file
	 */
	private String readFile(IFile file) {
		if (!file.exists())
			return "";
		InputStream stream = null;
		try{
			stream = file.getContents();
			Reader reader = new BufferedReader(new InputStreamReader(stream));
			StringBuffer result = new StringBuffer(2048);
			char[] buf = new char[2048];
			while (true){
				int count = reader.read(buf);
				if (count < 0)
					break;
				result.append(buf, 0, count);
			}
			return result.toString();
		} catch (Exception e){ // IOException and CoreException
			ProcessingLog.logError(e);
			return "";
		} finally {
			try{
				if (stream != null)
					stream.close();
			} catch (IOException e){
				ProcessingLog.logError(e);
				return "";
			}
		}
	}
	
	private int getLineCount(String what){
	    int count = 1;
	    for (char c : what.toCharArray()) {
	      if (c == '\n') count++;
	    }
	    return count;
	}

	/**
	 * Generates a problem marker from the preprocessor output
	 * <p>
	 * The preprocessor only hands back a line and column. This method reports the whole
	 * line and the message like the PDE.
	 * 
	 * @param msg error message
	 * @param file where the problem occurred
	 * @param line_number what line did the problem occur on
	 * @param isError is this an error
	 */
	private void reportProblem( String msg, IFile file, int line_number, boolean isError){
		System.out.println( (isError ? "ERROR: " : "WARNING: ") + msg);
		try{
			IMarker marker = file.createMarker(IMarker.PROBLEM);
			marker.setAttribute(IMarker.MESSAGE, msg);
			marker.setAttribute(IMarker.LINE_NUMBER, line_number);
			marker.setAttribute(IMarker.SEVERITY, isError ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING);
		} catch(CoreException e){
			ProcessingLog.logError(e);
			return;
		}
		
	}
	
	/**
	 * Checks the progress monitor for interruption
	 * 
	 * @param monitor
	 * @return true if the monitor is interrupted, false otherwise
	 */
	private boolean checkCancel(IProgressMonitor monitor){
		if (monitor.isCanceled()){
			throw new OperationCanceledException();
		}
		if (isInterrupted()){
			return true;
		}
		return false;		
	}
	
	/**
	 * Sets up the Static processing.app.Preferences class. 
	 */
	private void spoof_preferences(){
		Preferences.set("editor.tabs.size", "4");
		Preferences.set("preproc.substitute_floats","true");
		Preferences.set("preproc.web_colors", "true");
		Preferences.set("preproc.color_datatype", "true");
		Preferences.set("preproc.enhanced_casting", "true");
		Preferences.set("preproc.substitute.unicode", "true");
		Preferences.set("preproc.output_parse.tree", "false");
		Preferences.set("export.application.fullscreen", "false");
		Preferences.set("run.present.bgcolor", "#666666");
		Preferences.set("export.application.stop", "true");
		Preferences.set("run.present.stop.color", "#cccccc");
		Preferences.set("run.window.bgcolor", "#ECE9D8");
		Preferences.set("preproc.imports.list", "java.applet.*,java.awt.Dimension,java.awt.Frame,java.awt.event.MouseEvent,java.awt.event.KeyEvent,java.awt.event.FocusEvent,java.awt.Image,java.io.*,java.net.*,java.text.*,java.util.*,java.util.zip.*,java.util.regex.*");
	}
	
}
