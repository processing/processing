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

import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import processing.plugin.core.ProcessingCorePreferences;

public class ImportSketchWizardPage extends WizardPage {

	/** field containing the path to the existing sketch folder */
	private Text sketchPathField;

	public ImportSketchWizardPage() {
		super("Import Sketch");
		setTitle("Import Sketch Wizard");
		setDescription("Import an existing Processing sketch to the workspace.");
	}

	/* awt stuff. @see the javadoc for WizardPage */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		container.setLayout(gridLayout);
		setControl(container);

		final Label sketchPathLabel = new Label(container, SWT.NONE);
		final GridData sketchPathLabelPos = new GridData();
		sketchPathLabelPos.horizontalSpan = 3;
		sketchPathLabel.setLayoutData(sketchPathLabelPos);
		sketchPathLabel.setText("Select the root folder of the sketch to import");

		final Label sketchPathBoxLabel = new Label(container, SWT.NONE);
		final GridData sketchPathBoxLabelPos = new GridData(GridData.HORIZONTAL_ALIGN_END);
//		sketchPathBoxLabelPos.horizontalIndent = 20;
		sketchPathBoxLabel.setLayoutData(sketchPathBoxLabelPos);
		sketchPathBoxLabel.setText("Sketch Folder:");

		sketchPathField = new Text(container, SWT.BORDER);
		sketchPathField.addModifyListener(
			new ModifyListener() {
				public void modifyText(ModifyEvent e){
					updatePageComplete();
				}
			});
		sketchPathField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		final Button browseButton = new Button(container, SWT.NONE);
		browseButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				browseForDestinationFolder();
			}
		});
		browseButton.setText("Browse...");
		initContents();
	}

	/**
	 * Initialize the contents of the sketch book path.
	 * <p>
	 * If the user has specified a sketchbook, the sketchbook
	 * populates the field initially for convenience, even
	 * though the sketchbook is not itself a valid import.
	 */
	private void initContents() {
		IPath sketchbook = ProcessingCorePreferences.current().getSketchbookPath();
		if(sketchbook != null)
			sketchPathField.setText(sketchbook.toString());
		updatePageComplete();
	}

	/**
	 * Browse button functionality to find a destination folder.
	 * <p>
	 * If the path happens to be workspace relative, this will
	 * prettify the path.
	 */
	protected void browseForDestinationFolder() {
		IPath path = browse(getSketchPath());
		if (path == null) return;
		IPath rootLoc = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		if (rootLoc.isPrefixOf(path))
			path = path.setDevice(null).removeFirstSegments(rootLoc.segmentCount());
		sketchPathField.setText(path.toString());
	}

	/**
	 * Sets up a dialog box allowing the user to browse the file system and 
	 * select a directory.
	 * 
	 * @param path the path to be investigated
	 * @return the chosen path from the dialog box
	 */
	private IPath browse(IPath path){
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		if (path != null){
			if(path.segmentCount() > 1)
				dialog.setFilterPath(path.toOSString());
		}
		String result = dialog.open();
		if (result == null)
			return null;
		return new Path(result);		
	}

	/**
	 * Does some validation of the selected path before enabling the 
	 * finish button.
	 */
	private void updatePageComplete() {
		setPageComplete(false);

		// check path first
		// If it contains the sketchbook path, treat it as empty
		IPath sketchLoc = getSketchPath();
		IPath sketchbook = ProcessingCorePreferences.current().getSketchbookPath();
		if (sketchLoc == null 
				|| !sketchLoc.toFile().exists()
				|| (sketchbook != null 
						&& sketchLoc.makeAbsolute().equals(sketchbook.makeAbsolute()))){
			setErrorMessage(null);
			setMessage("Please specify a folder containing a sketch.");
			return;
		}


		// ensure the path contains the expected folderName.pde file
		String sketchName = sketchLoc.lastSegment();
		//System.out.println(sketchName + ".pde");
		File mainFile = new File(sketchLoc.toFile(), sketchName+".pde");
		if(!mainFile.exists()){
			setMessage(null);
			setErrorMessage("The sketch folder does not appear to be valid. " +
					"Could not find " + sketchName + ".pde");
			return;
		}

		// if nothing was caught, enable the finish button 
		setPageComplete(true);
		setMessage("Press Finish to import the sketch");
		setErrorMessage(null);
	}

	/**
	 * Tries to resolve the contents of the sketch path field to an IPath, 
	 * and returns it. If the field contains a relative path it will be 
	 * resolved relative to the Eclipse workspace folder.
	 * 
	 * @return an absolute IPath or null
	 */
	protected IPath getSketchPath() {
		String text = sketchPathField.getText().trim();
		if (text.length() == 0)	return null;
		IPath path = new Path(text);
		if (!path.isAbsolute()) // relative paths are relative to the Eclipse workspace
			path = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(path);
		return path;
	}

}
