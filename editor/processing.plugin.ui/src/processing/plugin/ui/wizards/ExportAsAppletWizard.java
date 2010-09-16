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

import java.util.Iterator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import processing.plugin.core.builder.SketchProject;

/**
 * A wizard to export a Processing sketch project as an applet
 * <p>
 * Contains a single page.
 * @author lonnen
 *
 */
public class ExportAsAppletWizard extends Wizard implements IExportWizard {
	
	/** single page */
	private ExportAsAppletWizardPage page;

	/** The project to be exported */
	private IProject project;

	/** The current selection when the export is called. */
	private IStructuredSelection fSelection;

	public ExportAsAppletWizard() {}

	/** 
	 * Used to figure out what we're exporting. 
	 * <p>
	 * If more than one item is selected, the first one listed
	 * is the one that gets exported. Just to make sure, the wizard
	 * page prompts with the name of the sketch before continuing. 
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		workbench.getActiveWorkbenchWindow().getActivePage();
		Iterator iter = selection.iterator();
		while (iter.hasNext()){
			Object selectedElement = iter.next();
			if (selectedElement instanceof IProject) {
				IProject proj = (IProject) selectedElement;
				if(SketchProject.isSketchProject(proj)){

				}
			} else if (selectedElement instanceof IResource){
				IProject proj = ((IResource) selectedElement).getProject();
				if(SketchProject.isSketchProject(proj)){
					setProject(proj);
					break;
				}
			}
		}		

	}

	public void setProject(IProject project){
		this.project = project;
		if (page != null) page.setProject(project);
	}

	public void addPages(){
		page = new ExportAsAppletWizardPage("Export Sketch Wizard");
		page.setProject(project);
		addPage(page);
	}

	public boolean performFinish() {
		return SketchProject.forProject(page.getProject()).exportAsApplet();
	}

}
