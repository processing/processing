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
// This is actually all original code, but it is part of an EPL project, so maybe I still need this? 
// If I wanted to worry about licensing I would have applied to law school. Back to code. [lonnen] June 18 2010
package org.processing.editor;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Container class for all the methods related to logging exceptions and other such
 * information that is useful to have written to a log file somewhere.
 * 
 * @author lonnen
 */
public class ProcessingLog {

	// Who needs a constructor?
	
	/**
	 * Convenience method for appending a string to the log file.
	 * Don't use this if there is an error.
	 * 
	 * @param message something to append to the log file 
	 */
	public static void logInfo(String message){
		log(IStatus.INFO, IStatus.OK, message, null);
	}
	
	/**
	 * Convenience method for appending an unexpected exception to the log file
	 * 
	 * @param exception some problem
	 */
	public static void logError(Throwable exception){
		logError("Unexpected Exception", exception);
	}
	
	/**
	 * Convenience method for appending an exception with a message
	 * 
	 * @param message a message, preferably something about the problem
	 * @param exception the problem
	 */
	public static void logError(String message, Throwable exception){
		log(IStatus.ERROR, IStatus.OK, message, exception);
	}
	
	/**
	 * Adapter method that creates the appropriate status to be logged
	 * 
	 * @param severity integer code indicating the type of message
	 * @param code plug-in-specific status code
	 * @param message a human readable message
	 */
	public static void log(int severity, int code, String message, Throwable exception){
		log(createStatus(severity, code, message, exception));
	}
	
	/**
	 * Creates a status object to log
	 * 
	 * @param severity integer code indicating the type of message
	 * @param code plug-in-specific status code
	 * @param message a human readable message
	 * @param a low-level exception, or null
	 * @return status object
	 */
	public static IStatus createStatus(int severity, int code, String message, Throwable exception){
		return new Status(severity, ProcessingEditorPlugin.ID, code, message, exception);
	}
	
	/**
	 * Write a status to the log
	 * 
	 * @param status
	 */
	public static void log(IStatus status){
		ProcessingEditorPlugin.getDefault().getLog().log(status);
	}
	
}
