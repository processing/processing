package processing.plugin.core.builder;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.QualifiedName;
import org.osgi.framework.Bundle;

import processing.app.Preferences;
import processing.app.Sketch;
import processing.app.preproc.PdePreprocessor;
import processing.app.preproc.PreprocessResult;
import processing.core.PApplet;

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

	/** Sketch folder that contains the sketch */
	private IProject sketch; 

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
	
	/**
	 * Build the sketch.
	 * <p>
	 * 1. Prepare the Sketch.  
	 * 2. Preprocess the files to Java
	 * 3. Shuffle the generated Java and supporting libs into the proper folders
	 * </p>
	 * Unlike the PDE build chain, the Sketch Builder stops short of compiling the generated files.
	 * In a properly configured project the Java builder will run after the Sketch builder finishes 
	 * and automagically handle the heavy compiling work.
	 * 
	 * 
	 * @param kind the build type
	 * @param args build arguments
	 * @param monitor let the user know things are happening
	 * @throws CoreException for I/O problems, the Eclipse UI should handle these automatically
	 */	
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {

		this.sketch = getProject();
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

		deleteP5ProblemMarkers(sketch);

		// save time on autobuilds, but respect clean and full builds
		if (kind == IncrementalProjectBuilder.CLEAN_BUILD || kind == IncrementalProjectBuilder.FULL_BUILD){
			removeDerived(monitor); // delete all the old state
		}

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
				file.setSessionProperty(new QualifiedName(ProcessingCore.BUILDER_ID, "preproc start"), bigCount);
				String content = Utilities.readFile((IFile) file);
				bigCode.append(content);
				bigCode.append("\n");
				bigCount += Utilities.getLineCount(content);
				file.setSessionProperty(new QualifiedName(ProcessingCore.BUILDER_ID, "preproc end"), bigCount);
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
					int low = (Integer) file.getSessionProperty(new QualifiedName(ProcessingCore.BUILDER_ID, "preproc start"));
					int high = (Integer) file.getSessionProperty(new QualifiedName(ProcessingCore.BUILDER_ID, "preproc end"));
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
						int low = (Integer) file.getSessionProperty(new QualifiedName(ProcessingCore.BUILDER_ID, "preproc start"));
						int high = (Integer) file.getSessionProperty(new QualifiedName(ProcessingCore.BUILDER_ID, "preproc end"));
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

		coreLibs = getCoreLibsFolder();
		sketchBookLibs = getSketchBookLibsFolder();

		// Clean the library table and rebuild it
		importToLibraryTable = new HashMap<String,File>();
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
				file.setSessionProperty(new QualifiedName(ProcessingCore.BUILDER_ID, "preproc start"), result.headerOffset);
			}
		}

		boolean foundMain = preproc.getFoundMain(); // is this still necessary?

		monitor.worked(10);
		if(checkCancel(monitor)) { return null; } 

		//COMPILE


		return null;
	}

	/**
	 * Removes all the derived files and markers generated by previous builds. 
	 * If there are issues expect a CoreException with details. Deletion can be 
	 * a long running operation, so give it access to the monitor.
	 * 
	 * @param monitor
	 * @throws CoreException
	 */
	private void removeDerived(IProgressMonitor monitor) throws CoreException {
		if (buildFolder.exists()){
			for( IResource r : buildFolder.members()){
				r.delete(IResource.FORCE, monitor);
			}
		}
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
			resource.deleteMarkers(ProcessingCore.MARKER_ID, true, IResource.DEPTH_INFINITE);
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
			IMarker marker = file.createMarker(ProcessingCore.MARKER_ID);
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
	public boolean  addLibraries(File folder) throws IOException{
		if (folder == null)	return false;
		if (!folder.isDirectory()) return false;

		String list[] = folder.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				// skip .DS_Store files, .svn folders, etc
				if (name.charAt(0) == '.') return false;
				if (name.equals("CVS")) return false;
				return (new File(dir, name).isDirectory());
			}
		});

		// if a bad folder or something like that, this might come back null
		if (list == null) return false;

		// alphabetize list, since it's not always alpha order
		Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

		boolean ifound = false;

		for (String potentialName : list) {
			File subfolder = new File(folder, potentialName);
			File libraryFolder = new File(subfolder, "library");
			File libraryJar = new File(libraryFolder, potentialName + ".jar");
			// look for the .jar inside /<library name>/library/<library name>.jar 
			if (libraryJar.exists()) {
				String sanityCheck = Sketch.sanitizeName(potentialName); // TODO remove this processing.app class dependency
				if (!sanityCheck.equals(potentialName)) {
					String mess =
						"The library \"" + potentialName + "\" cannot be used.\n" +
						"Library names must contain only basic letters and numbers.\n" +
						"(ASCII only and no spaces, and it cannot start with a number)";
					ProcessingLog.logInfo("Ignoring bad library name \n" + mess);
					continue;
				}

				// get the path for all .jar files in this code folder
				String libraryClassPath = Utilities.contentsToClassPath(libraryFolder);

				// the librariesClassPath is not used in the main build process
				//librariesClassPath += File.pathSeparatorChar + libraryClassPath;

				// need to associate each import with a library folder
				String packages[] =	Utilities.packageListFromClassPath(libraryClassPath);
				for (String pkg : packages) {
					importToLibraryTable.put(pkg, libraryFolder);
				}

				ifound = true;

			} else {  
				// not a library, but is still a folder, so recurse
				boolean found = addLibraries(subfolder);
				if (found) ifound = true; // it was found in the subfolder
			}
		}
		return ifound;
	}

	/**
	 * Finds the folder containing the Processing core libraries, which are bundled with the
	 * plugin. This folder doesn't exist in the workspace, so we return it as a plain File. 
	 * If a CoreException is thrown it will be logged and this will return null.
	 *  
	 * @return File containing the core libraries folder or null if it cannot be found
	 */
	public File getCoreLibsFolder() {
		Bundle bundle = ProcessingCore.getProcessingCore().getBundle();
		URL fileLocation;
		try {
			fileLocation = FileLocator.toFileURL(bundle.getEntry("/Resources" + File.pathSeparatorChar + "libraries"));
			File folder = new File(fileLocation.getPath());
			folder.isDirectory(); // throw an error if the folder does not exist
			return folder;
		} catch (Exception e) {
			ProcessingLog.logError(e);
			return null;
		}
	}

	/**
	 * Find the folder containing the Processing sketchbook libraries, which should be contained 
	 * in the same directory as the sketch itself. This only returns a handle to the folder. If 
	 * the folder does not exist it will be logged and returned as null
	 * 
	 * @return File containing the Sketch book library folder, or null if it doesn't exist
	 */
	public File getSketchBookLibsFolder() {
		try{
			File folder = sketch.getLocation().removeLastSegments(1).append("libraries").toFile();
			folder.isDirectory(); // throw an error if the folder does not exist
			return folder;
		} catch (Exception e){
			ProcessingLog.logError(e);
			return null;
		}
	}
}
