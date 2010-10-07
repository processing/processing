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
package processing.plugin.core.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

import processing.plugin.core.ProcessingCore;
import processing.plugin.core.ProcessingLog;

/** 
 * Handle an IResourceDelta reported against a Sketch Project.
 * <p>
 * Each instance of this class should only handle a single 
 * resource delta. Create a new one for each delta that needs
 * to be processed.
 * <p>
 * This class was inspired by the JyDT class of the same name
 * implemented by Red Robin software.
 * <p>
 * Right now only a full build is available, but the granularity
 * of this class prevents unnecessary builds, which saves a lot 
 * of time, especially on workspaces with more than a handful 
 * of sketches.
 * <p>
 * In the future this should be easy to integrate with a more
 * incremental build process. 
 */
public class IncrementalChangeProcessor {

	private boolean fullBuildRequired;

	private SketchProject sp;

	/** Give it a project as context */
	public IncrementalChangeProcessor(SketchProject sketchProject){
		sp = sketchProject;
		fullBuildRequired = false;
	}

	/** Indicate that a full build should be carried out */
	private void setFullBuildRequired(){
		fullBuildRequired = true;
	}

	/** Process an IResourceChangeEvent */
	public boolean resourceChanged(IResourceChangeEvent event){
		printEvent(event);
		IResourceDelta delta = event.getDelta();
		return this.resourceChanged(delta);
	}

	/** Process an IResourceDelta */
	public boolean resourceChanged(IResourceDelta delta){
		if (delta == null) return false;
		printResourceChanges(delta);

		IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
			public boolean visit(IResourceDelta delta) throws CoreException {
				switch (delta.getResource().getType()){
				case IResource.ROOT:
					// do nothing
					break;
				case IResource.PROJECT:
					IProject changedProject = (IProject) delta.getResource();
					if(changedProject != sp.getProject()) 
						return false;
					processProjectChange(delta);
					break;
				case IResource.FILE:
					processFileChange(delta);
					break;
				case IResource.FOLDER:
					processFolderChange(delta);
					break;
				default:
					break;
				}
				return true; // visit children
			}	
		};
		try{
			delta.accept(visitor);
		} catch (CoreException ex){
			ProcessingLog.logError(ex);
		}
		return fullBuildRequired;
	}

	/** Handle a change against the Project itself */
	private void processProjectChange(IResourceDelta delta){
		// Do nothing for now.
		switch (delta.getFlags()) {
		case IResourceDelta.DESCRIPTION:
			break;
		case IResourceDelta.OPEN:
			setFullBuildRequired();
			break;
		case IResourceDelta.TYPE:
			break;
		case IResourceDelta.SYNC:
			setFullBuildRequired();
			break;
		case IResourceDelta.MARKERS:
			break;
		case IResourceDelta.REPLACED:
			setFullBuildRequired();
			break;
		case IResourceDelta.MOVED_TO:
			setFullBuildRequired();
			break;
		case IResourceDelta.MOVED_FROM:
			break;
		default:
			break;
		}
	}

	/** Handle a resource change against a folder */
	private void processFolderChange(IResourceDelta delta){
		if (!sp.getProject().exists(delta.getProjectRelativePath()))
			return;
		switch(delta.getKind()){
		case IResourceDelta.ADDED:
			break;
		case IResourceDelta.REMOVED:
			if (delta.getFlags() == IResourceDelta.MOVED_TO)
				processInterestingFolderMove(delta);
			else
				processInterestingFolderRemoval(delta);
			break; 
		case IResourceDelta.CHANGED:
			processInterestingFolderChange(delta);
			break;
		default: 
			break;
		}
	}

	/** Handle the change of a folder in the project */
	private void processInterestingFolderChange(IResourceDelta delta){
		// we don't really care that the folder changed at the moment

		//		switch(delta.getFlags()){
		//		case IResourceDelta.CONTENT:
		//		case IResourceDelta.DESCRIPTION:
		//		case IResourceDelta.OPEN:
		//		case IResourceDelta.TYPE:
		//		case IResourceDelta.SYNC:
		//		case IResourceDelta.MARKERS:
		//		case IResourceDelta.REPLACED:
		//		case IResourceDelta.MOVED_TO:
		//		case IResourceDelta.MOVED_FROM:
		//		default:
		//			break;
		//		}
	}

	/** Handle the removal of a folder in the project  */
	private void processInterestingFolderRemoval(IResourceDelta delta){
		setFullBuildRequired();
	}

	/** Handle the move of a folder in the project */
	private void processInterestingFolderMove(IResourceDelta delta){
		setFullBuildRequired();
	}

	/** Handle the change of a file in the project */
	private void processFileChange(IResourceDelta delta){
		IFile resource = (IFile) delta.getResource();
		// we only care about .pde files
		if(!ProcessingCore.isProcessingFile(resource)) return;
		if (!sp.getProject().exists(resource.getProjectRelativePath())) return;
		
		//		System.out.println(delta.getProjectRelativePath() + " exists in " + sp.getProject().getName());
		setFullBuildRequired();

		//switch (delta.getKind()){
		//case IResourceDelta.ADDED:		
		//case IResourceDelta.REMOVED:
		//case IResourceDelta.CHANGED:
		//	int deltaFlags = delta.getFlags();
		//	switch(deltaFlags){
		//		case IResourceDelta.CONTENT:
		//		case IResourceDelta.ENCODING:
		//		case IResourceDelta.TYPE:
		//		case IResourceDelta.SYNC:
		//		case IResourceDelta.MARKERS:
		//		case IResourceDelta.REPLACED:
		//		case IResourceDelta.MOVED_TO:
		//		case IResourceDelta.MOVED_FROM:
		//		default:
		//			break;
		//		}
		//		break;
		//	default:
		//		break;
		//	}
	}

	/** Debug code */
	private static void printChange(IResourceDelta delta, StringBuffer buffer) {
		buffer.append("CHANGED ");
		buffer.append(delta.getFlags());
		switch (delta.getFlags()) {
		case IResourceDelta.CONTENT:
			buffer.append(" (CONTENT)");
			break;
		case IResourceDelta.ENCODING:
			buffer.append(" (ENCODING)");
			break;
		case IResourceDelta.DESCRIPTION:
			buffer.append(" (DESCRIPTION)");
			break;
		case IResourceDelta.OPEN:
			buffer.append(" (OPEN)");
			break;
		case IResourceDelta.TYPE:
			buffer.append(" (TYPE)");
			break;
		case IResourceDelta.SYNC:
			buffer.append(" (SYNC)");
			break;
		case IResourceDelta.MARKERS:
			buffer.append(" (MARKERS)");
			break;
		case IResourceDelta.REPLACED:
			buffer.append(" (REPLACED)");
			break;
		case IResourceDelta.MOVED_TO:
			buffer.append(" (MOVED_TO)");
			break;
		case IResourceDelta.MOVED_FROM:
			buffer.append(" (MOVED_FROM)");
			break;
		default:
			buffer.append(" (<unknown>)");
			break;
		}
	}

	/** Debug code */
	private static void printOneResourceChanged(IResourceDelta delta, StringBuffer buffer, int indent) {
		for (int i = 0; i < indent; i++) {
			buffer.append(' ');
		}
		switch (delta.getKind()) {
		case IResourceDelta.ADDED:
			buffer.append("ADDED");
			break;
		case IResourceDelta.REMOVED:
			buffer.append("REMOVED");
			break;
		case IResourceDelta.CHANGED:
			printChange(delta, buffer);
			break;
		default:
			buffer.append('[');
			buffer.append(delta.getKind());
			buffer.append(']');
			break;
		}
		buffer.append(' ');
		buffer.append(delta.getResource());
		buffer.append('\n');
	}

	/** Debug code */
	private static void printResourcesChanged(IResourceDelta delta, StringBuffer buffer, int indent) {
		printOneResourceChanged(delta, buffer, indent);
		IResourceDelta[] children = delta.getAffectedChildren();
		for (int i = 0; i < children.length; i++) {
			printResourcesChanged(children[i], buffer, indent + 1);
		}
	}

	/** Debug code */
	public static void printEvent(IResourceChangeEvent event) {
		StringBuffer buffer = new StringBuffer(80);
		buffer.append("Resource change event received ");
		switch (event.getType()) {
		case IResourceChangeEvent.POST_BUILD:
			buffer.append("[POST_BUILD]");
			break;
		case IResourceChangeEvent.POST_CHANGE:
			buffer.append("[POST_CHANGE]");
			break;
		case IResourceChangeEvent.PRE_BUILD:
			buffer.append("[PRE_BUILD]");
			break;
		case IResourceChangeEvent.PRE_CLOSE:
			buffer.append("[PRE_CLOSE]");
			break;
		case IResourceChangeEvent.PRE_DELETE:
			buffer.append("[PRE_DELETE]");
			break;
		default:
			break;
		}
		buffer.append(".\n");
		System.out.println(buffer);
	}

	/** Debug code */
	public static void printResourceChanges(IResourceDelta delta) {
		StringBuffer buffer = new StringBuffer(80);
		if (delta != null)
			printResourcesChanged(delta, buffer, 0);
		System.out.println(buffer);
	}


}
