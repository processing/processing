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
package processing.plugin.ui;

import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import processing.plugin.ui.processingeditor.ProcessingPartitionScanner;
import processing.plugin.ui.processingeditor.language.ProcessingCodeScanner;
import processing.plugin.ui.processingeditor.util.ProcessingColorProvider;

/**
 * Controls Processing Plugin user interface elements.
 * <p>
 * It provides convenience methods and controls elements shared by editors and other UI elements
 * such as the document provider or the partitioning scanner.
 */
public class ProcessingPlugin extends AbstractUIPlugin {

	/** The ID of the Processing UI Plugin */
	public static final String PLUGIN_ID = "processing.plugin.ui"; //$NON-NLS-1$

	/** The ID of a planned but unimplemented Processing Perspective */ //TODO Implement P5 Perspective
	public static final String ID_PERSPECTIVE = PLUGIN_ID + ".ProcessingPerspective";
	
	/** The ID of the processing  */
	public static final String PROCESSING_PARTITIONING = "__processing_partitioning";
	
	/** The shared plugin instance */
	private static ProcessingPlugin plugin;
	
	// Supporting Objects
	private ProcessingPartitionScanner fPartitionScanner;
	private ProcessingColorProvider fColorProvider;
	private ProcessingCodeScanner fCodeScanner;
	
	/** Initialized the shared instance. */
	public ProcessingPlugin() {
		super();
		plugin = this;
	}

	/* Method declared in plug-in */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		// Other init stuff
		// Don't forget to shut it down in stop()!
	}

	/* Method declared in plug-in */
	public void stop(BundleContext context) throws Exception {
		try{
			plugin = null;
			if(fPartitionScanner != null)
				fPartitionScanner = null;
			if(fColorProvider != null){
				fColorProvider.dispose();
				fColorProvider = null;
			}
			if(fCodeScanner != null)
				fCodeScanner = null;
		} finally {
			super.stop(context);
		}
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ProcessingPlugin getDefault() {
		return plugin;
	}
	
	/**
	 * Return a scanner for creating Processing partitions.
	 * Processing uses Java's commenting scheme, so our partitioner is almost identical. Unlike
	 * the Java partitioner, however, the Processing scanner treats the JavaDoc comments as 
	 * simple multiline comments. 
	 * 
	 * @return a scanner for creating Processing partitions
	 */
	 public ProcessingPartitionScanner getProcessingPartitionScanner() {
		if (fPartitionScanner == null)
			fPartitionScanner= new ProcessingPartitionScanner();
		return fPartitionScanner;
	}
	
	/**
	 * Returns the shared code scanner.
	 * 
	 * @return the singleton Processing code scanner
	 */
	 public RuleBasedScanner getProcessingCodeScanner() {
	 	if (fCodeScanner == null)
			fCodeScanner= new ProcessingCodeScanner(getProcessingColorProvider());
		return fCodeScanner;
	}
	
	/**
	 * Returns the shared color provider.
	 * 
	 * @return the singleton Processing color provider
	 */
	 public ProcessingColorProvider getProcessingColorProvider() {
	 	if (fColorProvider == null)
			fColorProvider= new ProcessingColorProvider();
		return fColorProvider;
	}

}
