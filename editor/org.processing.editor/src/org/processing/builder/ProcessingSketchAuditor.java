package org.processing.builder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
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
import org.eclipse.core.runtime.QualifiedName;
import org.processing.editor.ProcessingEditorPlugin;
import org.processing.editor.ProcessingLog;

import processing.app.Preferences;
import processing.app.SketchCode;
import processing.app.preproc.PdePreprocessor;
import processing.app.preproc.PreprocessResult;
import processing.app.debug.Compiler;
import processing.core.PApplet;

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
	
	public static final String BUILDER_ID = ProcessingEditorPlugin.PLUGIN_ID + ".processingSketchBuilder";
	
	/* a collection of fields from the sketch object 
	 * the sketch object cannot reused because it relies
	 * on an editor
	 */
	private boolean foundMain = false;
	private File primaryFile; // main pde file
	private String name; // name of the sketch
	//private boolean modified;
	private IProject sketch; // sketch folder that contains the sketch
	private IFolder dataFolder; // data folder, may not exist
	private IFolder codeFolder; // code folder, may not exist
	private IFolder outputFolder; // equivalent to tempBuildFolder in Sketch.java
	private SketchCode current;
	private int currentIndex;
	private int codeCount; // number of sketchCode objects in the current sketch, same as code.lenth
	private SketchCode[] code;
	private String appletClassName;
	private String classPath;
	private String libraryPath; // not the Processing libs path, but the Java libs path for the compiler
	private ArrayList<File> importedLibraries; // list of library folders
		
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

		// every build is a full build for now, @see incrementalBuild
		fullBuild(monitor);
		return null;
	}

	/**
	 * Re-compile only the files that have changed.
	 * <p>
	 * Should be much faster than a full build, but because the preprocessor
	 * mashes together all the source files for a sketch, there is only one
	 * resource that gets changed every time a sketch is compiled. Until the
	 * preprocessor is rewritten to handle individual files, every recompile 
	 * request will result in a full build. To save a little time, the build
	 * method has been modified to reflect this and never call the incremental
	 * builder, so we avoid the hassle of crawling the resources to try and
	 * identify which ones will be recomputed -- its safe to assume all of the
	 * files will be used because even the ones that are unchanged will be 
	 * grabbed.
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
	 * Preprocess and Compile the current code.
	 * <p>
	 * This is an adaptation of the processing.core sketch code to the Eclipse 
	 * platform. Unfortunately the original code is so tightly integrated to the PDE 
	 * that the original code cannot be re-used.
	 * 
	 * @param monitor let the user know things are happening
	 * @throws CoreException if there are problems accessing the files
	 */
	private void fullBuild(IProgressMonitor monitor) throws CoreException {
		//PREPARE
		monitor.beginTask("Full Project Build", 400); // not sure how much work to use, but there are 4 majors steps to the process
		sketch = getProject();
		
		if(!sketch.isOpen()) { return; } // has to be open to access it, similar to 'exists'
		if(checkCancel(monitor)) { return; } // user hasn't interfered
		if(!deleteProblemMarkers(sketch)) { return; } // clean out the boogers from the last build
		
		// IResource.members() doesn't return the files in a consistent order
		// so we get the list at the beginning of each build and use folderContents
		// whenever we need to get access to the source files during the build.
		IResource[] folderContents = sketch.members();

		// get handles to the expected folders
		codeFolder = sketch.getFolder("code");
		dataFolder = sketch.getFolder("data");
		outputFolder = sketch.getFolder("bin");
		// we know we need the bin, so create it if it doesn't exist
		if (!outputFolder.exists())
			outputFolder.create(IResource.NONE, true, null);

		monitor.worked(100); 
		if(checkCancel(monitor)) { return; } 
		
		//PREPROCESS

		spoof_preferences(); // fake the preferences object and start the preprocessor
		PdePreprocessor preprocessor = new PdePreprocessor(sketch.getName(), 4);

		String[] codeFolderPackages = null;
		String classPath = outputFolder.getLocation().toOSString(); //build folder
		

		// check the contents of the code folder to see if there are files
		// that need to be added to the imports
		if (codeFolder.exists()){
			libraryPath = codeFolder.getLocationURI().toString();
			// get a list of .jar files in the code folder and its subfolders
			String codeFolderClassPath = Compiler.contentsToClassPath(codeFolder.getLocation().toFile());
			// append the jar files to the class path
			classPath += File.pathSeparator + codeFolderClassPath;
			// get the list of packages in those jars
			codeFolderPackages = Compiler.packageListFromClassPath(codeFolderClassPath);
			// debug
			//for(String s : codeFolderPackages){ System.out.println(s);}
		} else { libraryPath = ""; }
		
		// 1. concatenate all .pde files to the 'main' pde		
		//    store line number for starting point of each code bit
		
		StringBuffer bigCode = new StringBuffer();
		int bigCount = 0; // line count
				
		for(int i = 0; i < folderContents.length; i++){
			IResource file = folderContents[i];
			if(file instanceof IFile && file.getFileExtension().equalsIgnoreCase("pde")){ // filters out only .pde files
				setPreprocOffset((IFile) file, bigCount);
				String content = readFile((IFile) file);
				bigCode.append(content);
				bigCode.append("\n");
				bigCount += getLineCount(content);
			}
		}

		monitor.worked(10);
		if(checkCancel(monitor)) { return; } 
		
		PreprocessResult result = null; // any exception catch will pooch the rest of the build, should 'return;' to stop the build
		try{ 
			IFile outputFile = outputFolder.getFile(sketch.getName() + ".java"); 
			StringWriter stream = new StringWriter();
			result = preprocessor.write(stream, bigCode.toString(), codeFolderPackages);
			
			// Eclipse idiom for generating the java file and marking it as a generated file
			ByteArrayInputStream inStream = new ByteArrayInputStream(stream.toString().getBytes());
			if (outputFile.exists()){ 
				outputFile.setContents(inStream, true, false, monitor); 
			} else {
				outputFile.create(inStream, true, monitor); 
			}
			outputFile.setDerived(true);
		} catch (CoreException e){
			ProcessingLog.logError(e);
			return;
		} catch(antlr.RecognitionException re){
						
			// first assume that it's the main file
			int errorFile = 0;
			int errorLine = re.getLine() - 1;
			
			// then search through for anyone else whose preprocName is null,
			// since they've also been combined into the main pde
			for(int i = 1; i < folderContents.length; i++){
				IResource file = folderContents[i];
				if(file instanceof IFile && file.getFileExtension().equalsIgnoreCase("pde") && (getPreprocOffset((IFile) file) < errorLine)){ 
					errorFile = i;
				}
			}
			
			errorLine -= getPreprocOffset((IFile) folderContents[errorFile]);			
			
			//DEBUG
			//System.out.println("error line - error file - offset");
	      	//System.out.println(errorLine + " - " + errorFile + " - " + getPreprocOffset((IFile) folderContents[errorFile]));
			
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
  			return;
		} catch (antlr.TokenStreamRecognitionException tsre) {
			// while this seems to store line and column internally,
			// there doesn't seem to be a method to grab it..
			// so instead it's done using a regexp
			
			// System.out.println("and then she tells me " + tsre.toString());
			// TODO test this ... ^ could be a problem
		    String mess = "^line (\\d+):(\\d+):\\s";
		    
		    String[] matches = PApplet.match(tsre.toString(), mess);
		    if (matches != null){
		    	int errorLine = Integer.parseInt(matches[1]) - 1;
		    	int errorColumn = Integer.parseInt(matches[2]);
		    	
		    	int errorFile = 0;
		    	for(int i = 1; i < folderContents.length; i++){
					IResource file = folderContents[i];
					if(file instanceof IFile && file.getFileExtension().equalsIgnoreCase("pde") && (getPreprocOffset((IFile) file) < errorLine)){ 
						errorFile = i;
					}
				}
				errorLine -= getPreprocOffset((IFile) folderContents[errorFile]);
	  			reportProblem(tsre.getMessage(), (IFile) folderContents[errorFile], errorLine, true);
		    } else {
		    	try{ // tries to the default to the main class
		    		reportProblem( tsre.toString(), sketch.getFile(sketch.getName() + ".pde"), 0, true);
		    	} catch (Exception e) {	ProcessingLog.logError(e); return; } // file may not exist (could be the problem)
		    }
		    return;
		} catch (Exception e){
			// uncaught exception
			ProcessingLog.logError(e);
			return;
		}
		
		monitor.worked(10);
		if(checkCancel(monitor)) { return; } 

		//grab the imports from the code just preproc'd
		
		importedLibraries = new ArrayList<File>();
		for (String item : result.extraImports){
			// remove things up to the last dot
			int dot = item.lastIndexOf('.');
			String entry = (dot == -1) ? item : item.substring(0, dot);
			// TODO workaround to avoid using Base to get the library. consider something with prefs.
			File libFolder = null;
			if (libFolder.exists()){
				importedLibraries.add(libFolder);
				classPath += Compiler.contentsToClassPath(libFolder);
				libraryPath += File.pathSeparator + libFolder.getAbsolutePath();
			}
		}
		
		String javaClassPath = System.getProperty("java.class.path");
		// Remove quotes if any ... an annoying ( and frequent ) Windows problem
		if (javaClassPath.startsWith("\"") && javaClassPath.endsWith("\"")) {
			javaClassPath = javaClassPath.substring(1,javaClassPath.length()-1);
		}
		classPath += File.pathSeparator + javaClassPath;
		
		monitor.worked(10);
		if(checkCancel(monitor)) { return; } 
		
		for(int i = 0; i < folderContents.length; i++){
			IResource file = folderContents[i];
			if(file instanceof IFile && file.getFileExtension().equalsIgnoreCase("java")){ 
				folderContents[i].copy(outputFolder.getProjectRelativePath(), IResource.DERIVED, monitor);
			} else if (file instanceof IFile && file.getFileExtension().equalsIgnoreCase("pde")){
				// The compiler and runner will need this to have a proper offset
				if (result == null)
					System.out.println("Danger!");
				addPreprocOffset((IFile) file, result.headerOffset);
			}
		}
		
		boolean foundMain = preprocessor.getFoundMain(); // is this still necessary?
		//return result.className
		
		monitor.worked(10);
		if(checkCancel(monitor)) { return; } 
		
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
	 * Utility method to mimic setPreprocOffset method of a SketchCode
	 * object using session properties of an eclipse resource
	 * 
	 * @param file the resource to modify
	 * @throws CoreException if the resource doesn't exist or cannot be accessed
	 */
	private void setPreprocOffset(IFile file, int offset) throws CoreException{
		file.setSessionProperty(new QualifiedName(BUILDER_ID, "Preproc Offset"), offset);
	}
	
	/**
	 * Utility method to mimic getPreprocOffset method of a SketchCode
	 * object using session properties of an eclipse resource
	 * 
	 * @param file the resource to modify
	 * @return the preprocessor offset or null if there is none
	 * @throws CoreException if the resource doesn't exist or cannot be accessed
	 * @throws ClassCastException if the session property cannot be converted to a string
	 */
	private int getPreprocOffset(IFile file) throws CoreException{
		Integer result = ((Integer) file.getSessionProperty(new QualifiedName(BUILDER_ID, "Preproc Offset")));
		return (result == null) ? result.intValue() : 0;
	}
	
	/**
	 * Utility method to mimic addPreprocOffset method of a SketchCode
	 * object combining the other two utility methods for getting and
	 * setting the preprocessor session property
	 * 
	 * @param file the resource to modify
	 * @param additionalOffset the amount of offset to add
	 * @throws CoreException if the resource doesn't exist or cannot be accessed
	 */
	private void addPreprocOffset(IFile file, int additionalOffset) throws CoreException{
		setPreprocOffset(file, getPreprocOffset(file)+additionalOffset);
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
