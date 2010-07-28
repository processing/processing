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
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.processing.editor.javadoc.JavaDocScanner;
import org.processing.editor.language.ProcessingCodeScanner;
import org.processing.editor.util.ProcessingColorProvider;

/**
 * Processing editor plug-in class.
 * Uses a singleton pattern to controls access to a few objects that need to be shared
 * across the plugin. Access these options with ProcessingEditorPlugin.getDefault().method() 
 * @since 3.0
 */
public class ProcessingEditorPlugin extends AbstractUIPlugin {
	
	public static final String PLUGIN_ID = "org.processing.editor";
	//public static final String JAVA_PARTITIONING= "__java_example_partitioning";   //$NON-NLS-1$
	public static final String PROCESSING_PARTITIONING= "__processing_partitioning";   //$NON-NLS-1$
	
	// The shared instance
	private static ProcessingEditorPlugin fgInstance;
	
	// Supporting objects that are managed by the singleton
	private ProcessingPartitionScanner fPartitionScanner;
	private ProcessingColorProvider fColorProvider;
	private ProcessingCodeScanner fCodeScanner;
	private JavaDocScanner fDocScanner;

	/**
	 * Creates a new plug-in instance.
	 */
	public ProcessingEditorPlugin() {
		//any init code should go in start()
	}
	
	/**
	 * Called when the plugin is loaded.
	 * 
	 * All initialization stuff goes here. Make sure to de-initialize it in stop()
	 * Also, try to keep these methods lean. If it takes too long the platform will
	 * cancel loading the plug-in. 
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fgInstance = this;
	}
	
	/**
	 * Called when the plugin is unloaded.
	 * 
	 * Make sure to remove anything here that was initialized to prevent memory
	 * leaks. Keep this lean, or the platform will cancel the operation.
	 */
	public void stop(BundleContext context) throws Exception {
		fgInstance = null;
		super.stop(context);
	}
	

	/**
	 * Returns the default plug-in instance.
	 * 
	 * @return the default plug-in instance
	 */
	public static ProcessingEditorPlugin getDefault() { return fgInstance; }
	
	/**
	 * Return a scanner for creating Processing partitions.
	 * Processing uses Java's commenting scheme, so our partitioner is almost identical. Unlike
	 * the Java partitioner, however, this Processing one currently treats the JavaDoc style
	 * comments as simple multiline comments. 
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
	
	/**
	 * Returns the singleton Processingdoc scanner. 
	 * 
	 * @return the singleton Processingdoc scanner
	 */
	 public RuleBasedScanner getProcessingDocScanner() {
	 	if (fDocScanner == null)
			fDocScanner= new JavaDocScanner(fColorProvider);
		return fDocScanner;
	}
	
	 /**
	  * Returns a buffered input stream for a file in the plug-in directory.
	  * 
	  * @param filename the file to be loaded
	  * @return BufferedInputStream to read the file with
	  */
 	public BufferedInputStream getFileInputStream(String filename) {
		   Bundle bundle = getDefault().getBundle();
		   URL fileLocation;
		   try {
		       fileLocation = FileLocator.toFileURL(bundle.getEntry(filename));
		       BufferedInputStream file = new BufferedInputStream(fileLocation.openStream());
			   return file;
		   } catch (IOException e) {
		       e.printStackTrace();
		   }
		   return null; // this should be more explicit than a null pointer from a caught exception, right? [lonnen] June 15, 2010
 	}


}
