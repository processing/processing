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
package processing.plugin.ui.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import processing.plugin.core.ProcessingLog;
import processing.plugin.core.builder.SketchProject;
import processing.plugin.core.exporter.Exporter;


/**
 * An export wizard for processing projects.
 * <p>
 * The single page presents users with a list of open sketch Projects in 
 * the workspace and the user can check one or all of them to export.
 */
public class ExportAsAppletWizard extends Wizard implements IExportWizard {
	
	/** Single page */
	private ExportAsAppletSelectProjectsWizardPage page;

	public ExportAsAppletWizard() { }

	/** Nothing to init. The page takes care of this itself. */
	public void init(IWorkbench workbench, IStructuredSelection selection) { }

	/* Loads the single page. */
	public void addPages(){
		page = new ExportAsAppletSelectProjectsWizardPage("Export Sketch Wizard");
		addPage(page);
	}

	/* Occurs when user hits the finish button. */
	public boolean performFinish() {
		for (SketchProject sp : page.getSelectedProjects()){
			if (!Exporter.exportApplet(sp)){
				ProcessingLog.logInfo( "Unable to export " + sp.getProject().getName() 
						+ ". Check the error log for more info.");
			}
			
			// java.io might have changed things, even if it failed
			// so force the workspace to refresh or everything will disappear
			
			try {
				sp.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException e) {
				ProcessingLog.logError("The workspace could not refresh after the export wizard ran. " 
					+ "You may need to manually refresh the workspace to continue.", e);
			}
		}
		return true;
	}
}
