/**
 * Copyright (c) 2010 Chris Lonnen. All rights reserved.
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * Contributors:
 *     Chris Lonnen - initial API and implementation
 */
package processing.plugin.core.launching;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.JavaRuntime;

import processing.plugin.core.ProcessingLog;

/**
 * The default launch configuration delegate for Processing Sketches. 
 * <p>
 * Checks to make sure the last build was successful and that the sketch is not being run in debug mode.
 * Then it computes some sensible defaults, builds a simple html wrapper and launches the sketch as an
 * applet. This is not the same process as an export, and will not populate the applet folder.
 */
public class ProcessingSketchLaunchConfigurationDelegate extends JavaLaunchDelegate implements IDebugEventSetListener {

	/**
	 * Maps ILaunch objects to File objects that represent the .html file initiating the
	 * applet launch. This is used to delete the .html file when the launch terminates.
	 */
	private static Map fgLaunchToFileMap = new HashMap();

	/** Used to map temp file to launch object. */
	private ILaunch fLaunch;	

	/* (non-Javadoc) Makes sure to cleanup the leftovers if things break. */
	public synchronized void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		try{
			fLaunch = launch;
			super.launch(configuration, mode, launch, monitor);
		} catch (CoreException e){
			cleanup(launch);
			throw e;
		}
		fLaunch = null;
	}

	/** 
	 * Called first in the launch sequence. Checks to make sure this launcher is being executed
	 * in run mode, and returns true to indicate that things can proceed. If it is being executed
	 * in debug mode or some other unsupported mode, return false to abort the launch.
	 */
	public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		if(mode.equals(ILaunchManager.RUN_MODE)){
			if(configuration.getAttribute("wasLastBuildSuccessful", false)) 
				return true;
			ProcessingLog.logInfo("Aborting launch -- Sketch has unresolved build problems.");
		}
		return false;
	}

	// public String getJavaPolicyFile() ??

	/** Clean up the temp files and listeners after a launch */
	public void cleanup(ILaunch launch){
		File temp = (File) fgLaunchToFileMap.get(launch);
		if (temp != null){
			try {
				fgLaunchToFileMap.remove(launch);
				temp.delete();
			} finally {
				// unregister any debug listeners? there shouldn't be any
				// because we don't support debugging
			}
		}

	}

	public File buildHTMLFile(ILaunchConfiguration configuration, File dir) {
		FileWriter writer = null;
		File tempFile = null;
		try {
			String name = getAppletMainTypeName(configuration);
			tempFile = new File(dir, name + System.currentTimeMillis() + ".html"); //$NON-NLS-1$ 
			writer = new FileWriter(tempFile);
			writer.write("<html>\n"); 
			writer.write("<body>\n"); 
			writer.write("<applet code=");
			writer.write(name);
			writer.write(".class "); 
			String appletName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_NAME, "");
			if (appletName.length() != 0) {
				writer.write("NAME =\"" + appletName + "\" ");
			}
			writer.write("width=\"");
			writer.write(Integer.toString(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_WIDTH, 200))); 
			writer.write("\" height=\"");
			writer.write(Integer.toString(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_HEIGHT, 200))); 
			writer.write("\" >\n");
			Map parameters = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_PARAMETERS, new HashMap());
			if (parameters.size() != 0) {
				Iterator iterator= parameters.entrySet().iterator();
				while(iterator.hasNext()) {
		 			Map.Entry next = (Map.Entry) iterator.next();
					writer.write("<param name=");
					writer.write(getQuotedString((String)next.getKey()));
					writer.write(" value=");
					writer.write(getQuotedString((String)next.getValue()));
					writer.write(">\n");
				}
			}
			writer.write("</applet>\n");
			writer.write("</body>\n"); 
			writer.write("</html>\n");
		} catch(IOException e) {
		} catch(CoreException e) {
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch(IOException e) {
				}
			}
		}
		
		return tempFile;
	}
	
	public String getQuotedString(String string) {
		if (string.indexOf('"') == -1) {
			return '"' + string + '"';
		} 
		return '\'' + string + '\'';
	}


	/* @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getProgramArguments(org.eclipse.debug.core.ILaunchConfiguration) */
	public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException{
		File workingDir = verifyWorkingDirectory(configuration);
		File htmlFile = buildHTMLFile(configuration, workingDir);
		if(htmlFile == null){
			abort("Could not build HTML for applet launch.", null, IJavaLaunchConfigurationConstants.ERR_COULD_NOT_BUILD_HTML);
		}
		// add a mapping of the launch to the html file
		fgLaunchToFileMap.put(fLaunch, htmlFile);
		return htmlFile.getName();
	}

	//	Uncomment if we end up using javaPolicyFile
	//	public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
	//		StringBuffer arguments = new StringBuffer(super.getVMArguments(configuration));
	//		File workingDir = verifyWorkingDirectory(configuration);
	//		String javaPolicyFile = getJavaPolicyFile(workingDir);
	//		arguments.append(javaPolicyFile);
	//		return arguments.toString();
	//	}

	public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException{
		return configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_APPLETVIEWER_CLASS, IJavaLaunchConfigurationConstants.DEFAULT_APPLETVIEWER_CLASS);
	}

	/** Returns the applet's main type name. */
	public String getAppletMainTypeName(ILaunchConfiguration configuration) throws CoreException{
		return super.getMainTypeName(configuration);
	}

	/* @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getDefaultWorkingDirectory(org.eclipse.debug.core.ILaunchConfiguration) */
	public File getDefaultWorkingDirectory(ILaunchConfiguration configuration) throws CoreException{
		// default working dir for applets is the project's output directory
		String outputDir = JavaRuntime.getProjectOutputDirectory(configuration);
		if (outputDir == null) {
			// if no project attribute, default to eclipse directory
			return new File(System.getProperty("user.dir")); 
		}
		IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(outputDir);
		if (resource == null || !resource.exists()) {
			//default to eclipse directory
			return new File(System.getProperty("user.dir"));
		}
		return resource.getLocation().toFile(); 
	}

}
