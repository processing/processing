package processing.plugin.core.launching;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * The launch configuration delegate for Processing Sketches. It is really a customized Java
 * launch configuration delegate, as Processing Sketches are compiled down and ultimately run
 * as Java applications (shhh! dont tell anyone)
 */
public class ProcessingSketchLaunchConfigurationDelegate extends JavaLaunchDelegate {

	public void launch(ILaunchConfiguration configuration, String mode,	ILaunch launch, IProgressMonitor monitor) throws CoreException {
		String workingDirectory = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, "");
		launch.setAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, workingDirectory);
		super.launch(configuration, mode, launch, monitor);
	}

	/**
	 * If this is being started in run mode return true, otherwise return false to indicate it shouldn't be run
	 */
	public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		if(mode.equals(ILaunchManager.RUN_MODE)){
			return true;
		}
		return false;
	}
	
	/** Returns the default VMInstall object. */
	public IVMInstall getVMInstall(ILaunchConfiguration configuration) throws CoreException {
		return JavaRuntime.getDefaultVMInstall();
	}
	
	public Map getVMSpecificAttributesMap(ILaunchConfiguration configuration) throws CoreException{
		return null;
	}
	
	public String[] getClassPath(ILaunchConfiguration configuration) throws CoreException{
		IRuntimeClasspathEntry[] entries = new ProcessingClasspathProvider().computeUnresolvedClasspath(configuration);
		//TODO pickup here http://www.eclipse.org/articles/Article-Launch-Framework/launch.html
		return null;
	}
	
	
}
