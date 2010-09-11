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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class ExportAsAppletWizardPage extends WizardPage {

	IProject project;

	/** should only be instantiated by its corresponding wizard */
	protected ExportAsAppletWizardPage(String pageName) {
		super(pageName);
		setTitle("Export Sketch Wizard");
		setDescription("Export your sketch as an applet.");
	}

	public void setProject(IProject project){
		this.project = project;
	}

	/** nothing fancy here, just a confirmation. the core will handle most of it. */
	public void createControl(Composite parent) {
		setPageComplete(false);

		Composite container = new Composite(parent, SWT.NULL);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		container.setLayout(gridLayout);
		setControl(container);

		final Label label = new Label(container, SWT.NONE);
		final Label label2 = new Label(container, SWT.NONE);
		final GridData gridData = new GridData();
		gridData.horizontalSpan = 3;	
		label.setLayoutData(gridData);
		label2.setLayoutData(gridData);
		if (project != null ){
			label.setText(
				"You're about to export " + project.getName() + " as an applet.");
			label2.setText(
				"Click finish to proceed or cancel to select a different sketch.");
			setPageComplete(true);
		} else {
			label.setText(
				"The wizard cannot figure out what you are trying to export.");
			label2.setText(
				"Click cancel to go back and select a different sketch or .pde file.");
		}
	}
	
	public IProject getProject(){
		return project;
	}

}
