/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.processing.editor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.text.rules.RuleBasedScanner;
//import org.processing.editor.javadoc.JavaDocScanner;
import org.processing.editor.language.ProcessingCodeScanner;
import org.processing.editor.util.ProcessingColorProvider;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Processing editor plug-in class.
 * Manages the startup and shutdown of the plugin. Also uses a singleton pattern to controls 
 * access to a few objects that need to be shared across the plugin. Access these objects with 
 * ProcessingEditorPlugin.getDefault().method() 
 */
public class ProcessingEditorPlugin extends AbstractUIPlugin {
	
	public static final String ID = "org.processing.ProcessingEditor";
	public static final String PROCESSING_PARTITIONING= "__processing_partitioning";
	
	// The shared instance
	private static ProcessingEditorPlugin fgInstance;
	
	// Supporting objects that are managed by the singleton
	private ProcessingPartitionScanner fPartitionScanner;
	private ProcessingColorProvider fColorProvider;
	private ProcessingCodeScanner fCodeScanner;
	//private JavaDocScanner fDocScanner;

	/**
	 * Creates a new plug-in instance.
	 * 
	 * Called when by the Eclipse runtime when the plugin is activated.
	 * 
	 * @see org.eclipse.core.runtime.Plugin
	 */
	public ProcessingEditorPlugin() {}
	
	/**
	 * Invoked by the platform the first time any code from this plug-in is 
	 * executed. Due to the platforms time-sensitive constraints, this
	 * only initializes a shared instance of the plugin itself.
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fgInstance = this;
	}
	
	/**
	 * Invoked by the platform when this plugin is shutting down. Performs a
	 * simple termination of this plugin's singleton instance.
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		fgInstance = null;
		super.stop(context);
	}
	

	/**
	 * Returns the default plug-in instance. This method should be used to
	 * access any of the other singleton objects in the plugin.
	 * 
	 * @return the default plug-in instance
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin
	 */
	public static ProcessingEditorPlugin getDefault() { return fgInstance; }
	
	/**
	 * Returns a scanner for creating Processing partitions.
	 * Processing uses Java's commenting scheme, so our partitioner is almost identical. Unlike
	 * the Java partitioner, however, this Processing scanner treats the JavaDoc style comments 
	 * as simple multiline comments. 
	 * 
	 * @return a scanner for creating Processing partitions
	 */
	 public ProcessingPartitionScanner getProcessingPartitionScanner() {
		if (fPartitionScanner == null)
			fPartitionScanner= new ProcessingPartitionScanner();
		return fPartitionScanner;
	}
	
	/**
	 * Returns the singleton Processing code scanner.
	 * 
	 * @return the singleton Processing code scanner
	 * @see org.processing.editor.language.ProcessingCodeScanner
	 */
	 public RuleBasedScanner getProcessingCodeScanner() {
	 	if (fCodeScanner == null)
			fCodeScanner= new ProcessingCodeScanner(getProcessingColorProvider());
		return fCodeScanner;
	}
	
	/**
	 * Returns the singleton Processing color provider.
	 * 
	 * @return the singleton Processing color provider
	 */
	 public ProcessingColorProvider getProcessingColorProvider() {
	 	if (fColorProvider == null)
			fColorProvider= new ProcessingColorProvider();
		return fColorProvider;
	}

	 //For the time being, we're disabling JavaDoc
//	/**
//	 * Returns the singleton Processingdoc scanner.
//	 * 
//	 * @return the singleton Processingdoc scanner
//	 */
//	 public RuleBasedScanner getProcessingDocScanner() {
//	 	if (fDocScanner == null)
//			fDocScanner= new JavaDocScanner(fColorProvider);
//		return fDocScanner;
//	}
	
	 /**
	  * Returns a buffered input stream for a file in the plug-in directory.
	  * 
	  * Used by the CodeScanner to access the keyword list in the plug-in directory.
	  * 
	  * @param filename the file to be loaded
	  * @return BufferedInputStream to read the file with
	  */
	 public BufferedInputStream getFileInputStream(String filename) {
		 //TODO consider replacing this with find (IPath path) and openStream (IPath file)
		 Bundle bundle = getDefault().getBundle(); // the plugin's root directory, regardless of install directory
		 try {
			 URL fileLocation = FileLocator.toFileURL(bundle.getEntry(filename));
			 BufferedInputStream file = new BufferedInputStream(fileLocation.openStream());
			 return file;
		 } catch (IOException e) {
			 e.printStackTrace();
		 }
		 return null; // this should be more explicit than a null pointer from a caught exception, right? [lonnen] June 15, 2010
	 }


}
