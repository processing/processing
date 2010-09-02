package processing.plugin.core.builder;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IClasspathEntry;

import processing.app.Preferences;
import processing.app.debug.RunnerException;
import processing.app.preproc.PdePreprocessor;
import processing.app.preproc.PreprocessResult;

import processing.plugin.core.ProcessingCore;
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
	// The model would be really similar to what Sketch would look like if it was 
	// truly separated from the UI. Possible create it in the PDE, and extend it
	// here for marker management and such.

	//	/** Data folder, located in the sketch folder, may not exist */
	//	private IFolder dataFolder;

	/** Full paths to source folders for the JDT  */
	private ArrayList<IPath>srcFolderPathList;

	/** Full paths to jars required to compile the sketch */
	private ArrayList<IPath>libraryJarPathList;

	/** Code folder, located in the sketch folder, may not exist */
	private IFolder codeFolder;

	/** A temporary build folder, will be created if it doesn't exist */
	private File buildFolder;

	/** Clean any leftover state from previous builds. */
	protected void clean(SketchProject sketch, IProgressMonitor monitor) throws CoreException{
		deleteP5ProblemMarkers(sketch);
		srcFolderPathList = new ArrayList<IPath>();
		libraryJarPathList = new ArrayList<IPath>();
		// if this is the first run of the builder the build folder will not be stored yet,
		// but if there is an old build folder from a trial run it should still be nuked.
		// get the handle to it from the project's configuration
		Utilities.deleteFolderContents(sketch.getBuildFolder());



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
			ProcessingLog.logError("Sketch is inaccessible!", null);
			return null;
		}

		// Get handles to the folders
		codeFolder = sketchProject.getCodeFolder();
		buildFolder = sketchProject.getBuildFolder(); // null if it couldn't be created

		if (buildFolder == null){
			ProcessingLog.logError("Build folder could not be accessed.", null);
			return null;
		}

		monitor.beginTask("Sketch Build", 400); // not sure how much work to do here
		if(!sketch.isOpen()) { return null; } // has to be open to access it
		if(checkCancel(monitor)) { return null; }

		PdePreprocessor preproc = new PdePreprocessor(sketch.getName(), 4);
		String[] codeFolderPackages = null;

		// If the code folder exists:
		//    Find any .jar files in it and its subfolders
		//       Add their paths to the library jar list for addition to the class path later on 
		//       Get the packages of those jars so they can be added to the imports
		//    Add it to the class path source folders
		if (codeFolder.exists()){
			String codeFolderClassPath = Utilities.contentsToClassPath(codeFolder.getLocation().toFile());
			for( String s : codeFolderClassPath.split(File.separator)){
				libraryJarPathList.add(new Path(s));
			}
			codeFolderPackages = Utilities.packageListFromClassPath(codeFolderClassPath);
			srcFolderPathList.add(codeFolder.getFullPath()); // not sure about this one
		}

		// concatenate the individual .pde files into one large file using temporary 
		// 'session properties' attached to the IResource files, mark the start and end lines that they 
		// contribute to the bigCode file. This information will be used later for mapping errors backwards

		StringBuffer bigCode = new StringBuffer();
		int bigCount = 0; // line count

		for( IResource file : sketch.members()){
			if("pde".equalsIgnoreCase(file.getFileExtension())){ 
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
			final File java = new File(buildFolder, sketch.getName() + ".java"); 
			final PrintWriter stream = new PrintWriter(new FileWriter(java));
			try{
				result = preproc.write(stream, bigCode.toString(), codeFolderPackages);
				srcFolderPathList.add(new Path(buildFolder.getCanonicalPath()));
			} finally {
				stream.close();
			}
		} catch(antlr.RecognitionException re){

			IResource errorFile = null; // if this remains null, the error is reported back on the sketch itself with no line number
			int errorLine = re.getLine() - 1;

			for( IResource file : sketch.members()){
				if("pde".equalsIgnoreCase(file.getFileExtension())){ 
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

			String msg = re.getMessage();		

			//TODO better errors handling, matching errors often get put after the document end. see highlightLine() inside editor.
			// try to find a way to put all this error handling code in one spot. All of the message parsing and resource blaming,
			// so it doesn't appear multiple times in the source.
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

			String[] matches = Utilities.match(tsre.toString(), mess);
			IResource errorFile = null; 
			int errorLine = -1;

			if (matches != null){
				errorLine = Integer.parseInt(matches[1]) - 1;
				//		    	int errorColumn = Integer.parseInt(matches[2]); // unused in the builder

				for( IResource file : sketch.members()){
					if("pde".equalsIgnoreCase(file.getFileExtension())){
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
				if("pde".equalsIgnoreCase(file.getFileExtension())){
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

			//TODO see better error mapping todo above
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
		} catch (Exception e){
			ProcessingLog.logError(e); 
			return null;
		}

		monitor.worked(10);
		if(checkCancel(monitor)) { return null; }

		ArrayList<String> libs = new ArrayList<String>(); // a list of all the libraries that can be found

		libs.addAll( Utilities.getLibraryJars(ProcessingCore.getProcessingCore().getCoreLibsFolder()) );
		libs.addAll( Utilities.getLibraryJars(Utilities.getSketchBookLibsFolder(sketch)) );

		// setup the library table
		HashMap<String, IPath> importToLibraryTable = new HashMap<String, IPath>();

//		System.out.println("Libraries found (path sep: " + File.separator + " ) :");	
		for (String s : libs ){
//			System.out.println(s);
			String[] packages = Utilities.packageListFromClassPath(s);
			for (String pkg : packages){
				importToLibraryTable.put(pkg, new Path(s));
//				System.out.println(pkg);
			}
		}

//		System.out.println("There were a few extra imports: " + result.extraImports.size());
		for (int i=0; i < result.extraImports.size(); i++){
			String item = result.extraImports.get(i);
			// remove things up to the last dot
			int dot = item.lastIndexOf('.');
			String entry = (dot == -1) ? item : item.substring(0, dot);
//			System.out.println(entry);
			IPath libPath = importToLibraryTable.get(entry);
			if (libPath != null ){
				libraryJarPathList.add(libPath); // huzzah! we've found it, make sure its fed to the compiler
			} else { 
				// The user is trying to import something we won't be able to find.
				reportProblem(
						"Library import " + entry +" could not be found. Check the library folder in your sketchbook.",
						sketch.getFile( sketch.getName() + ".pde"), i+1, true 
				);
			}
		}

		// Adding the ol' Java classpath is handled by Eclipse. We don't worry about it.

		monitor.worked(10);
		if(checkCancel(monitor)) { return null; } 

		// I don't think this next part is necessary.
		// At this point we've written the derived java file and the code folder,
		// if it exists, is marked to be added to the classpath. There's not much sense
		// to copying things to the code folder and further messing with the offsets.

		//		// 3. loop over the code[] and save each .java file
		//
		//		for( IResource file : sketch.members()){
		//			if("java".equalsIgnoreCase(file.getFileExtension())){
		//				String filename = file.getName() + ".java";
		//				try{
		//					String program = Utilities.readFile((IFile) file);
		//					String[] pkg = Utilities.match(program, Utilities.PACKAGE_REGEX);
		//					// if no package, add one
		//					if(pkg == null){
		//						pkg = new String[] { packageName };
		//						// add the package name to the source
		//						program = "package " + packageName + ";" + program;
		//					}
		//					IFolder packageFolder = buildFolder.getFolder(pkg[0].replace('.', '/'));
		//					if (!packageFolder.exists())
		//						packageFolder.create(IResource.NONE, true, null);
		//
		//					IFile modFile = packageFolder.getFile(file.getName() + ".java");
		//
		//					ByteArrayInputStream inStream = new ByteArrayInputStream(program.getBytes());
		//					try{
		//						if (modFile.exists()) { 
		//							modFile.setContents(inStream, true, false, monitor);  
		//						} else { 
		//							modFile.create(inStream, true, monitor);
		//						}
		//						modFile.setDerived(true, monitor);
		//					} finally {
		//						inStream.close();
		//					}
		//				} catch (Exception e){
		//					ProcessingLog.logError("Problem moving " + filename + " to the build folder.", e);
		//				}
		//			} else if("pde".equalsIgnoreCase(file.getFileExtension())){
		//				// The compiler will need this to have a proper offset
		//				// not sure why every file gets the same offset, but ok I'll go with it... [lonnen] aug 20 2011 
		//				file.setSessionProperty(new QualifiedName(BUILDER_ID, "preproc start"), result.headerOffset);
		//			}
		//		}

		monitor.worked(10);
		if(checkCancel(monitor)) { return null; }
		
		// Even though the list types are specified, Java still tosses errors when I try 
		// to directly convert them or cast them. So instead I'm stuck doing this.
		
		IPath[] libPaths = new IPath[libraryJarPathList.size()];
		int i=0;
		for(IPath path : libraryJarPathList) libPaths[i++] = path;

		IPath[] srcPaths = new IPath[srcFolderPathList.size()];
		i=0;
		for(IPath path : srcFolderPathList) srcPaths[i++] = path;
		
		sketchProject.updateClasspathEntries( srcPaths, libPaths);
		
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
		if(resource != null && resource.exists()){
			resource.deleteMarkers(SketchBuilder.PREPROCMARKER, true, IResource.DEPTH_INFINITE);
			resource.deleteMarkers(SketchBuilder.COMPILEMARKER, true, IResource.DEPTH_INFINITE);
		}
	}

	protected static void deleteP5ProblemMarkers(SketchProject sketch)throws CoreException{
		if(sketch != null)
			deleteP5ProblemMarkers(sketch.getProject());
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
	private void reportProblem(String msg, IResource file, int lineNumber, boolean isError){
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
}
