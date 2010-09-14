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
package processing.plugin.ui.preferences;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import processing.plugin.core.ProcessingCorePreferences;

/**
 * Pref pane class for the Core preferences. 
 * <p>
 * Has a single field corresponding to the sketchbook path. Paths are verified, then if they
 * are invalid a warning message is displayed. Whenever an empty path or a valid path is set
 * it is saved. The OK button is only clickable after a successful save or a legit path.
 * <p>
 * Because the Processing Core plug-in is not an Abstract UI plugin, it does not carry a 
 * preference store.
 */
public class CorePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Text sketchbookPathField;
	
	public CorePreferencePage() {
		super();
		setTitle("Processing Core");
		noDefaultAndApplyButton();
	}


	public void init(IWorkbench workbench) {}

	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(3, false));

		Label l = new Label(container, SWT.LEFT);
		l.setText("Sketchbook Folder:");
		GridData gd = new GridData();
		gd.horizontalIndent = 20;
		l.setLayoutData(gd);
		
		
		sketchbookPathField = new Text(container, SWT.BORDER);
		sketchbookPathField.addModifyListener(
				new ModifyListener() {
					public void modifyText(ModifyEvent e){
						updatePageComplete();
					}
				});
		sketchbookPathField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Button button_1 = new Button(container, SWT.NONE);
		button_1.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){browseForDestinationFolder();}
		});
		button_1.setText("Browse...");
		
		initContents();
		
		return container;
	}
	
	public void initContents(){
		String sketchbook = ProcessingCorePreferences.current().getSketchbookPathAsString();
		if(sketchbook != null){
			System.out.println("INIT: " + sketchbook + ";");
			sketchbookPathField.setText(sketchbook.toString());
		} else {
			sketchbookPathField.setText("");
		}
	}
	
	/**
	 * Browse button functionality to find a destination folder
	 * <p>
	 * Prettifies the file path if it happens to be in the workspace
	 */
	protected void browseForDestinationFolder() {
		IPath path = browse(resolveLocation());
		if (path == null)
			return;
		IPath rootLoc = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		if (rootLoc.isPrefixOf(path))
			path = path.setDevice(null).removeFirstSegments(rootLoc.segmentCount());
		sketchbookPathField.setText(path.toString());
	}
	
	/**
	 * Sets up a dialog box allowing you to select a directory to use for the sketchbook
	 * 
	 * @param path the path to be investigated
	 * @return the path chosen in the dialog box
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
	 * Verifies that the sketchbook path exists or is empty
 	 */
	private void updatePageComplete() {				
		// check the sketchbook path first
		IPath sketchbookLoc = resolveLocation();
		if (sketchbookLoc == null){
			setErrorMessage(null);
			setMessage("Please specify a sketchbook folder.");
		} else if (!sketchbookLoc.toFile().exists()){
			// Do not accept invalid paths
			setValid(false);
			setMessage(null);
			setErrorMessage("Must specify a valid folder or leave blank.");
			return;
		}
		// if nothing was caught, enable the finish button 
		setValid(true);
		setMessage(null);
		setErrorMessage(null);
		ProcessingCorePreferences.current().setSketchbookPathWithString(sketchbookPathField.getText());
	}

	/**
	 * Tries to resolve the contents of the sketch book path field to an IPath, and returns it.
	 * If the field contains a relative path it will be resolved relative to the Eclipse workspace folder.
	 *
	 * 
	 * @return an absolute IPath handle to the contents of the sketchbookPathField, or null
	 */
	protected IPath resolveLocation() {
		if (sketchbookPathField==null) return null;
		String text = sketchbookPathField.getText().trim();
		if (text.length() == 0)	return null;
		IPath path = new Path(text);
		if (!path.isAbsolute()) path = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(path);
		return path;
	}
	
	
}