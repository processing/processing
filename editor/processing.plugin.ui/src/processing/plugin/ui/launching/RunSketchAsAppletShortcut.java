/*******************************************************************************
 * This program and the accompanying materials are made available under the 
 * terms of the Common Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.opensource.org/licenses/cpl1.0.php
 * 
 * Contributors:
 *     IBM Corporation - initial API
 *     Chris Lonnen - implementation for Processing
 *******************************************************************************/
package processing.plugin.ui.launching;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.ResourceUtil;

import processing.plugin.core.builder.SketchProject;
import processing.plugin.ui.ProcessingLog;

public class RunSketchAsAppletShortcut implements ILaunchShortcut {

	protected ILaunchConfiguration createConfiguration(IProject project){
		if (project == null) return null;
		SketchProject sketch = SketchProject.forProject(project);
		ILaunchConfiguration config = null;
		try{
			ILaunchConfigurationType configType = getConfigurationType();
			ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, project.getName());
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, sketch.getMainType());
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_WIDTH, sketch.getWidth());
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_HEIGHT, sketch.getHeight());
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_NAME, "Processing Sketch");
			wc.setMappedResources(new IResource[] { sketch.getJavaProject().getUnderlyingResource() });
//			config =wc.doSave();
			config = wc; // this prevents a run config from being saved and sticking around.
		} catch (CoreException ce) {
			ProcessingLog.logError(ce);
		}
		return config;
	}

	protected ILaunchConfigurationType getConfigurationType(){
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		return lm.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLET);
	}

	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection)selection).getFirstElement();

			if (element instanceof IResource){
				IProject proj = ((IResource)element).getProject();
				try{
					if (proj.hasNature("processing.plugin.core.sketchNature")){
						launch(createConfiguration(proj), mode);
					} else {
						ProcessingLog.logInfo("Sketch could not be launched. The selected project does not have the required Sketch nature.");
					}
				} catch (CoreException e){
					ProcessingLog.logError("Launch aborted.", e);
				}
			} else {
				ProcessingLog.logInfo("Sketch could not be launched. Launcher was provided with a non-resource selection.");
			}
		}
	}

	// have to implement. log a warning.
	// there isn't a great way to launch this without a model
	public void launch(IEditorPart editor, String mode) {
		IFile file = ResourceUtil.getFile(editor.getEditorInput());
		if(file != null){
			IProject proj = file.getProject();
			try{
				if (proj.hasNature("processing.plugin.core.sketchNature")){
					launch(createConfiguration(proj), mode);
				} else {
					ProcessingLog.logInfo("Sketch could not be launched. The editor contains a file that is not part of a project with a Sketch nature.");
				}
			} catch (CoreException e){
				ProcessingLog.logError("Launch aborted.", e);
			}
		} else {
			ProcessingLog.logInfo("Launch aborted. Editor contents are not part of a Sketch Project in the workspace");
		}
	}

	private void launch(ILaunchConfiguration config, String mode) {
		if (config != null)
			DebugUITools.launch(config, mode);
	}

	
}
