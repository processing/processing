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

import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.processing.editor.javadoc.JavaDocScanner;
import org.processing.editor.language.ProcessingCodeScanner;
import org.processing.editor.util.ProcessingColorProvider;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Processing editor plug-in class.
 * Uses a singleton pattern to controls access to a few objects that need to be shared
 * across the plugin. Access these options with ProcessingEditorPlugin.getDefault().method() 
 * @since 3.0
 */
public class ProcessingEditorPlugin extends AbstractUIPlugin {
	
	public static final String PLUGIN_ID = "org.processing.ProcessingEditor";
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
	 * 
	 */
	public ProcessingEditorPlugin() {
//		[lonnen]
//		Java editor example has "fgInstance= this;" 
//		while the editor template uses the start method to handle
//		that and leaves this empty. Since I've been chasing down
//		this null pointer error for going on 12 hours, I'm going
//		to try it the template's way and see if it works.
//		 
//		That did the trick! On to debugging other problems. [lonnen] June 10 2010
	}
	
	/* added from the editor template, not present in the java editor code
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fgInstance = this;
		//System.out.println("fgInstance initialized!");
	}
	
	/* added from the editor template, not present in the java editor code
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
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
}
