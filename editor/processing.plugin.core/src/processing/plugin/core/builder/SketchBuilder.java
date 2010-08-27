package processing.plugin.core.builder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.osgi.framework.Bundle;

import processing.app.Preferences;
import processing.app.Sketch;
import processing.app.debug.RunnerException;
import processing.app.preproc.PdePreprocessor;
import processing.app.preproc.PreprocessResult;
import processing.core.PApplet;

import processing.plugin.core.ProcessingCore;
import processing.plugin.core.ProcessingCorePreferences;
import processing.plugin.core.ProcessingLog;

/**
 * Builder for Processing Sketches
 * <p>
 * Preprocesses .pde sketches into Java. Errors returned are reflected back on the source files.
 * From Eclipse's perspective, Sketch projects are actually specially configured Java projects.
 * The SketchNature manages this configuration stuff, so woe be to those who would carelessly
 * manipulate this builder directly.
 * </p>
 * <p>
 * The builder is compatible with the PDE, and it expects sketches to be laid out with the
 * same folder structure. It may store metadata and temporary build files in the sketch 
 * file system but these will not change how the PDE interacts with it. Users should be 
 * able to use the PDE interchangeably with this builder.
 * </p>
 * <p>
 * Though this implements the Incremental Project Builder, all builds are really full
 * builds because the preprocessor is not incremental.
 * </p>
 * @author lonnen
 *
 */
public class SketchBuilder extends IncrementalProjectBuilder{	

	/**
	 * The identifier for the Processing builder
	 * (value <code>"processing.plugin.core.processingbuilder"</code>).
	 */
	public static final String BUILDER_ID = ProcessingCore.PLUGIN_ID + ".sketchBuilder";

	/** 
	 * Problem marker for processing preprocessor issues 
	 * (value <code>"processing.plugin.core.preprocError"</code>).
	 */
	public static final String PREPROCMARKER = ProcessingCore.PLUGIN_ID + ".preprocError";

	/**
	 * Problem marker for processing compile issues
	 * value <code>"processing.plugin.core.compileError"</code>
	 */
	public static final String COMPILEMARKER = ProcessingCore.PLUGIN_ID + ".compileError";

	/** All of these need to be set for the Processing.app classes. */
	static{
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

	///** For testing */
	//public boolean DEBUG = true;

	// TODO the builder is maintaining too much state. move this all to a model
	// Right now it is building an ad-hoc model on the fly using the implied
	// structure of a Sketch project. Minimally a model should manage these state
	// objects, manage markers, and be controlled by the SketchProject object.

	/** Data folder, located in the sketch folder, may not exist */
	private IFolder dataFolder;

	/** Code folder, located in the sketch folder, may not exist */
	private IFolder codeFolder;

	/** A temporary build folder, will be created if it doesn't exist */
	private IFolder buildFolder;

	/** The output applet folder for after the compile */
	private IFolder appletFolder;

	/** The core libraries folder in the plug-in resources. */
	private File coreLibs;

	/** The SketchBook libraries folder, may not exist */
	private File sketchBookLibs;

	/** the library path for the compiler */
	private String libraryPath;

	/** the class path for the compiler */
	private String classPath;

	/** a list of library folders */
	private ArrayList<File> importedLibraries; // list of library folders

	/** a table resolving a package to the folder containing its .jar */
	private HashMap<String,File> importToLibraryTable;

	/** Supplied as a build argument, usually empty */
	private String packageName = "";

	/** Clean any leftover state from previous builds. */
	protected void clean(SketchProject sketch, IProgressMonitor monitor) throws CoreException{
		deleteP5ProblemMarkers(sketch);
		if (buildFolder != null && buildFolder.exists()){
			for( IResource r : buildFolder.members()){
				r.delete(IResource.FORCE, monitor);
			}
		}
		// any other cleaning stuff goes here
		// Eventually, a model should control the markers, and once a model is
		// written it should be controlled by the SketchProject. Cleaning the model 
		// should be in a method called beCleaned() in the SketchProject.
		// This method will then look something like:
		// SketchProject sketch = SketchProject.forProject(this.getProject());
		// sketch.beCleaned();
	}

	/**
	 * Build the sketch project.
	 * <p>
	 * This usually means grabbing all the Processing files and compiling them
	 * to Java source files and moving them into a designated folder where the
	 * JDT will grab them and build them.
	 * </p>
	 * <p>
	 * For now all builds are full builds because the preprocessor does not
	 * handle incremental builds. 
	 * </p>
	 * 
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException{
		SketchProject sketch = SketchProject.forProject(this.getProject());
		// Eventually we may distinguish between build types.
		//		switch (kind){
		//		case FULL_BUILD:
		//			return this.fullBuild(sketch, monitor);
		//		case AUTO_BUILD:
		//			return this.autoBuild(sketch, monitor);
		//		case INCREMENTAL_BUILD:
		//			return this.incrementalBuild(sketch, monitor);
		//		default:
		//			return null
		//		}
		return this.fullBuild(sketch, monitor);
	}

	/**
	 * Full build from scratch.
	 * <p>
	 * Try to clean out and old markers from derived files. They may not be present,
	 * but if they are wipe 'em out and get along with the business of building.
	 * This can be a long running process, so we use a monitor.
	 */	
	protected IProject[] fullBuild( SketchProject sketchProject, IProgressMonitor monitor) throws CoreException {
		this.clean(sketchProject, monitor);
		IProject sketch = sketchProject.getProject();

		if ( sketch == null || !sketch.isAccessible() ){
			System.out.println("Sketch is null!");
			return null;
		}

		// Setup the folders
		codeFolder = sketch.getFolder("code");
		dataFolder = sketch.getFolder("data");
		buildFolder = sketch.getFolder("bin"); // TODO relocate to MyPlugin.getPlugin().getStateLocation().getFolder("bin")
		appletFolder = sketch.getFolder("applet");

		monitor.beginTask("Sketch Build", 400); // not sure how much work to do here
		if(!sketch.isOpen()) { return null; } // has to be open to access it
		if(checkCancel(monitor)) { return null; }

		// 1 . PREPARE

		if (!buildFolder.exists())
			buildFolder.create(IResource.NONE, true, null);

		if (!appletFolder.exists())
			appletFolder.create(IResource.NONE, true, null);

		monitor.worked(100); // 100 for every step of the process?
		if(checkCancel(monitor)){ return null; }

		// 2 . PREPROCESS
		PdePreprocessor preproc = new PdePreprocessor(sketch.getName(), 4);

		String[] codeFolderPackages = null;
		classPath = appletFolder.getLocation().toOSString();

		// check the contents of the code folder to see if there are files that need to be added to the imports
		if (codeFolder.exists()){
			libraryPath = codeFolder.getLocation().toOSString();
			// get a list of .jar files in the code folder and its subfolders
			String codeFolderClassPath = Utilities.contentsToClassPath(codeFolder.getLocation().toFile());
			// append the jar files to the class path
			classPath += File.pathSeparator + codeFolderClassPath;
			// get the list of packages in those jars
			codeFolderPackages = Utilities.packageListFromClassPath(codeFolderClassPath);
		} else { libraryPath = ""; }

		// concat all .pde files to the 'main' pde
		// store line number for the starting and ending points of each bit

		StringBuffer bigCode = new StringBuffer();
		int bigCount = 0; // line count

		for( IResource file : sketch.members()){
			if(file.getFileExtension() != null && file.getFileExtension().equalsIgnoreCase("pde")){ 
				file.setSessionProperty(new QualifiedName(BUILDER_ID, "preproc start"), bigCount);
				String content = Utilities.readFile((IFile) file);
				bigCode.append(content);
				bigCode.append("\n");
				bigCount += Utilities.getLineCount(content);
				file.setSessionProperty(new QualifiedName(BUILDER_ID, "preproc end"), bigCount);
			}
		}

		monitor.worked(10);
		if(checkCancel(monitor)) { return null; } 

		PreprocessResult result = null;
		try{ 
			IFile outputFile = buildFolder.getFile(sketch.getName() + ".java"); 
			StringWriter stream = new StringWriter();
			result = preproc.write(stream, bigCode.toString(), codeFolderPackages);

			// Eclipse idiom for generating the java file and marking it as a generated file
			ByteArrayInputStream inStream = new ByteArrayInputStream(stream.toString().getBytes());
			try{
				if (outputFile.exists()) { 
					outputFile.setContents(inStream, true, false, monitor);  
				} else { 
					outputFile.create(inStream, true, monitor);
				}
				outputFile.setDerived(true, monitor);
			} finally {
				stream.close();
				inStream.close();
			}
		} catch(antlr.RecognitionException re){

			IResource errorFile = null; // if this remains null, the error is reported back on the sketch itself with no line
			int errorLine = re.getLine() - 1;

			for( IResource file : sketch.members()){
				if(file.getFileExtension() != null && file.getFileExtension().equalsIgnoreCase("pde")){ 
					int low = (Integer) file.getSessionProperty(new QualifiedName(BUILDER_ID, "preproc start"));
					int high = (Integer) file.getSessionProperty(new QualifiedName(BUILDER_ID, "preproc end"));
					if( low <= errorLine && high > errorLine){
						errorFile = file;
						errorLine -= low;
						break;
					}
				}			
			}

			// mark the whole project if no file will step forward.
			if (errorFile == null){
				errorFile = sketch;
				errorLine = -1;
			} 

			//DEBUG
			//System.out.println("error line - error file - offset");
			//System.out.println(errorLine + " - " + errorFile + " - " + getPreprocOffset((IFile) folderContents[errorFile]));

			String msg = re.getMessage();		

			//TODO better remapping of errors, matching errors often get put after the document end. see highlightLine() inside editor.
			if (msg.equals("expecting RCURLY, found 'null'"))
				msg = "Found one too many { characters without a } to match it.";
			if (msg.indexOf("expecting RBRACK") != -1)
				msg = "Syntax error, maybe a missing right ] character?";
			if (msg.indexOf("expecting SEMI") != -1)
				msg = "Syntax error, maybe a missing semicolon?";
			if (msg.indexOf("expecting RPAREN") != -1)
				msg = "Syntax error, maybe a missing right parenthesis?";
			if (msg.indexOf("preproc.web_colors") != -1)
				msg = "A web color (such as #ffcc00) must be six digits.";

			reportProblem(msg, errorFile, errorLine, true);
			return null; // exit the build
		} catch (antlr.TokenStreamRecognitionException tsre) { 
			// System.out.println("and then she tells me " + tsre.toString());
			String mess = "^line (\\d+):(\\d+):\\s"; // a regexp to grab the line and column from the exception

			String[] matches = PApplet.match(tsre.toString(), mess); // TODO remove this processing.app dependency
			IResource errorFile = null; 
			int errorLine = -1;

			if (matches != null){
				errorLine = Integer.parseInt(matches[1]) - 1;
				//		    	int errorColumn = Integer.parseInt(matches[2]); // unused in the builder

				for( IResource file : sketch.members()){
					if(file.getFileExtension() != null && file.getFileExtension().equalsIgnoreCase("pde")){ 
						int low = (Integer) file.getSessionProperty(new QualifiedName(BUILDER_ID, "preproc start"));
						int high = (Integer) file.getSessionProperty(new QualifiedName(BUILDER_ID, "preproc end"));
						if( low <= errorLine && high > errorLine){
							errorFile = file;
							errorLine -= low;
							break;
						}
					}			
				}


			} 

			// If no file was found or the regex failed
			if (errorFile == null){
				errorFile = sketch;
				errorLine = -1;
			} 

			reportProblem(tsre.getMessage(), errorFile, errorLine, true);		
			return null;
		} catch (RunnerException re){
			/* 
			 * This error is not addressed in the PDE. I've only seen it correspond to
			 * an unclosed, double quote mark ("). The runner reports 1 line behind where
			 * it occurs, so we add 1 to its line.
			 */
			IResource errorFile = null; // if this remains null, the error is reported back on the sketch itself with no line
			int errorLine = re.getCodeLine() + 1;

			for( IResource file : sketch.members()){
				if(file.getFileExtension() != null && file.getFileExtension().equalsIgnoreCase("pde")){ 
					int low = (Integer) file.getSessionProperty(new QualifiedName(BUILDER_ID, "preproc start"));
					int high = (Integer) file.getSessionProperty(new QualifiedName(BUILDER_ID, "preproc end"));
					if( low <= errorLine && high > errorLine){
						errorFile = file;
						errorLine -= low;
						break;
					}
				}			
			}

			// mark the whole project if no file will step forward.
			if (errorFile == null){
				errorFile = sketch;
				errorLine = -1;
			} 

			//DEBUG
			//System.out.println("error line - error file - offset");
			//System.out.println(errorLine + " - " + errorFile + " - " + getPreprocOffset((IFile) folderContents[errorFile]));

			String msg = re.getMessage();		

			//TODO better remapping of errors, matching errors often get put after the document end. see highlightLine() inside editor.
			if (msg.equals("expecting RCURLY, found 'null'"))
				msg = "Found one too many { characters without a } to match it.";
			if (msg.indexOf("expecting RBRACK") != -1)
				msg = "Syntax error, maybe a missing right ] character?";
			if (msg.indexOf("expecting SEMI") != -1)
				msg = "Syntax error, maybe a missing semicolon?";
			if (msg.indexOf("expecting RPAREN") != -1)
				msg = "Syntax error, maybe a missing right parenthesis?";
			if (msg.indexOf("preproc.web_colors") != -1)
				msg = "A web color (such as #ffcc00) must be six digits.";

			reportProblem(msg, errorFile, errorLine, true);
			return null; // exit the build
		} catch (CoreException e){
			ProcessingLog.logError(e); // logging the error is a better  
			return null;
		} catch (Exception e){
			ProcessingLog.logError(e); 
			return null;
		}

		monitor.worked(10);
		if(checkCancel(monitor)) { return null; } 

		// Get the imports from the code that was preproc'd
		importedLibraries = new ArrayList<File>();

		coreLibs = getCoreLibsFolder().getAbsoluteFile();
		sketchBookLibs = getSketchBookLibsFolder(sketch).getAbsoluteFile();

		// Clean the library table and rebuild it
		importToLibraryTable = new HashMap<String,File>();

		// addLibraries internally checks for null folders
		try{
			addLibraries(coreLibs);
			addLibraries(sketchBookLibs);
		} catch (IOException e){
			ProcessingLog.logError("Libraries could not be loaded.", e);
		}

		for (String item : result.extraImports){
			// remove things up to the last dot
			int dot = item.lastIndexOf('.');
			String entry = (dot == -1) ? item : item.substring(0, dot);
			File libFolder = importToLibraryTable.get(entry);
			if (libFolder != null ){
				importedLibraries.add(libFolder);
				classPath += Utilities.contentsToClassPath(libFolder);
				libraryPath += File.pathSeparator + libFolder.getAbsolutePath();
			}
		}

		// Finally add the regular Java CLASSPATH
		String javaClassPath = System.getProperty("java.class.path");
		// Remove quotes if any ... an annoying ( and frequent ) Windows problem
		if (javaClassPath.startsWith("\"") && javaClassPath.endsWith("\""))
			javaClassPath = javaClassPath.substring(1,javaClassPath.length()-1);
		classPath += File.pathSeparator + javaClassPath;	

		monitor.worked(10);
		if(checkCancel(monitor)) { return null; } 

		// 3. loop over the code[] and save each .java file

		for( IResource file : sketch.members()){
			if(file.getFileExtension() != null && file.getFileExtension().equalsIgnoreCase("java")){
				String filename = file.getName() + ".java";
				try{
					String program = Utilities.readFile((IFile) file);
					String[] pkg = PApplet.match(program, Utilities.PACKAGE_REGEX); //TODO remove this processing.app dependency
					// if no package, add one
					if(pkg == null){
						pkg = new String[] { packageName };
						// add the package name to the source
						program = "package " + packageName + ";" + program;
					}
					IFolder packageFolder = buildFolder.getFolder(pkg[0].replace('.', '/'));
					if (!packageFolder.exists())
						packageFolder.create(IResource.NONE, true, null);

					IFile modFile = packageFolder.getFile(file.getName() + ".java");

					ByteArrayInputStream inStream = new ByteArrayInputStream(program.getBytes());
					try{
						if (modFile.exists()) { 
							modFile.setContents(inStream, true, false, monitor);  
						} else { 
							modFile.create(inStream, true, monitor);
						}
						modFile.setDerived(true, monitor);
					} finally {
						inStream.close();
					}
				} catch (Exception e){
					ProcessingLog.logError("Problem moving " + filename + " to the build folder.", e);
				}
			} else if (file.getFileExtension() != null && file.getFileExtension().equalsIgnoreCase("pde")){
				// The compiler will need this to have a proper offset
				// not sure why every file gets the same offset, but ok I'll go with it... [lonnen] aug 20 2011 
				file.setSessionProperty(new QualifiedName(BUILDER_ID, "preproc start"), result.headerOffset);
			}
		}

		monitor.worked(10);
		if(checkCancel(monitor)) { return null; } 

		//COMPILE
		
		// setup the VM
		IPath containerPath = new Path(JavaRuntime.JRE_CONTAINER);
		IVMInstall vm = JavaRuntime.getDefaultVMInstall();
		IPath vmPath = containerPath.append(vm.getVMInstallType().getId()).append(vm.getName());

		
		System.out.println("classPath entries:");
		for( String s : classPath.split(File.pathSeparator)){
			if (!s.isEmpty())
				System.out.println(s);
		}

		System.out.println("IClasspathEntry[] items:");
		// Build the classpath entries
		IClasspathEntry[] newClasspath = {
		      JavaCore.newSourceEntry(buildFolder.getFullPath()),
		      //JavaCore.newLibraryEntry(),
		      JavaCore.newContainerEntry(vmPath)
		      		};
		for (IClasspathEntry s : newClasspath){
			System.out.println(s.toString());
		}

		return null;
	}

	/**
	 * Try to delete all of the existing P5 problem markers
	 * <p>
	 * This should only remove the Processing specific markers.
	 * 
	 * @param project the project to be stripped of problem markers
	 * @return true if all markers were deleted, false if some remain
	 * @throws CoreException
	 */
	protected static void deleteP5ProblemMarkers(IResource resource) throws CoreException{
		if(resource != null && resource.exists())
			resource.deleteMarkers(SketchBuilder.PREPROCMARKER, true, IResource.DEPTH_INFINITE);
		resource.deleteMarkers(SketchBuilder.COMPILEMARKER, true, IResource.DEPTH_INFINITE);
	}

	protected static void deleteP5ProblemMarkers(SketchProject sketch)throws CoreException{
		if(sketch != null){
			IProject proj = sketch.getProject();
			if (proj.exists())
				deleteP5ProblemMarkers(proj);
		}
	}

	/**
	 * Generates a problem marker from the preprocessor output
	 * <p>
	 * Tags the whole line and adds an issue to the Problems box. If the problem could not be tied
	 * to a specific file it will be marked against the project and the line will not be marked.
	 * 
	 * @param msg error message
	 * @param file where the problem occurred
	 * @param line_number what line did the problem occur on
	 * @param isError is this an error
	 */
	private void reportProblem( String msg, IResource file, int lineNumber, boolean isError){
		try{
			IMarker marker = file.createMarker(SketchBuilder.PREPROCMARKER);
			marker.setAttribute(IMarker.MESSAGE, msg);
			marker.setAttribute(IMarker.SEVERITY, isError ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING);
			if( lineNumber != -1)
				marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		} catch(CoreException e){
			ProcessingLog.logError(e);
			return;
		}	
	}

	/**
	 * Check for interruptions
	 * <p>
	 * The build process is rather long so if the user cancels things toss an exception.
	 * Builds also tie up the workspace, so check to see if something is demanding to
	 * interrupt it and let it. Usually these interruptions would force a rebuild of the
	 * system anyhow, so save some CPU cycles and time.
	 * </p>
	 * 
	 * @param monitor
	 * @return true if the build is hogging the resource thread
	 * @throws OperationCanceledException is the user cancels
	 */
	private boolean checkCancel(IProgressMonitor monitor){
		if (monitor.isCanceled()){ throw new OperationCanceledException(); }
		if (isInterrupted()){ return true; }
		return false;
	}

	/**
	 * Finds any Processing Libraries in the folder and loads them into the HashMap
	 * importToLibraryTable so they can be imported later. Based on the Base.addLibraries,
	 * but adapted to avoid GUI elements
	 * 
	 * @param folder the folder containing libraries
	 * @return true if libraries were imported, false otherwise
	 */
	public boolean addLibraries(File folder) throws IOException{
		if (folder == null)	return false;
		if (!folder.isDirectory()) return false;

		File list[] = folder.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				// skip .DS_Store files, .svn folders, etc
				if (name.charAt(0) == '.') return false;
				if (name.equals("CVS")) return false;
				return (new File(dir, name).isDirectory());
			}
		});

		// if a bad folder or something like that, this might come back null
		if (list == null) return false;

		boolean ifound = false;

		for (File potentialFile : list){
			if(ProcessingCore.isLibrary(potentialFile, true)){
				File libraryFolder = new File(potentialFile, "library");
				// get the path of all .jar files in this code folder
				String libraryClassPath = Utilities.contentsToClassPath(libraryFolder);
				// associate each import with a library folder
				String packages[] = Utilities.packageListFromClassPath(libraryClassPath);
				for (String pkg:packages){
					importToLibraryTable.put(pkg, libraryFolder);
				}
				ifound = true;
			} else {
				if (addLibraries(potentialFile))  // recurse!
					ifound = true;
			}
		}
		
		return ifound;
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
	 * Find the folder containing the users libraries, which should be in the sketchbook.
	 * Looks in the user's preferences first and if that is null it checks for the appropriate
	 * folder relative to the sketch location.
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
