package processing.plugin.core.launching;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.StandardClasspathProvider;

public class ProcessingClasspathProvider  extends StandardClasspathProvider{

	public IRuntimeClasspathEntry[] computeUnresolvedClasspath(ILaunchConfiguration configuration) {
		return null;
	}

}
