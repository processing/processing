package org.processing.wizards;

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

import processing.app.Sketch;


public class NewSketchWizardPage extends WizardPage {

	/** field containg the name of the sketch to be created */
	private Text sketchNameField; 
	/** field containing path of the sketch book folder that will contain the sketch */
	private Text sketchbookPathField; // TODO get the default from the plugin preferences
	/** the actual path of the sketch book folder */
	private IPath initialSketchbookPath;
	
	public NewSketchWizardPage() {
		super("selectFiles");
		setTitle("New Sketch Wizard");
		setDescription("Create a new Processing Sketch");
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		container.setLayout(gridLayout);
		setControl(container);
		
		final Label label = new Label(container, SWT.NONE);
		final GridData gridData = new GridData();
		gridData.horizontalSpan = 3;
		label.setLayoutData(gridData);
		label.setText("Select a name for the new sketch.");
		
		final Label label_1 = new Label(container, SWT.NONE);
		final GridData gridData_1 = new GridData(GridData.HORIZONTAL_ALIGN_END);
		label_1.setLayoutData(gridData_1);
		label_1.setText("Sketch Name:");
		
		sketchNameField = new Text(container, SWT.BORDER);
		sketchNameField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e){
				updatePageComplete();
			}
		});
		sketchNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		final Label label_2 = new Label(container, SWT.NONE);
		final GridData gridData_2 = new GridData();
		gridData_2.horizontalSpan = 3;
		label_2.setLayoutData(gridData_2);
		
		final Label label_3 = new Label(container, SWT.NONE);
		final GridData gridData_3 = new GridData();
		gridData_3.horizontalSpan = 3;
		label_3.setLayoutData(gridData_3);
		label_3.setLayoutData(gridData_3);
		label_3.setText("Select the sketchbook folder that will contain the sketch:");
		
		final Label label_4 = new Label(container, SWT.NONE);
		final GridData gridData_4 = new GridData();
		gridData_4.horizontalIndent = 20;
		label_4.setLayoutData(gridData_4);
		label_4.setText("Sketchbook Folder:");

		sketchbookPathField = new Text(container, SWT.BORDER);
		sketchbookPathField.addModifyListener(
				new ModifyListener() {
					public void modifyText(ModifyEvent e){
						updatePageComplete();
					}
				});
		sketchbookPathField.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		
		final Button button_1 = new Button(container, SWT.NONE);
		button_1.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){browseForDestinationFolder();
			}
		});
		button_1.setText("Browse...");
		
		initContents();
	}

	/** initialize the contents of the sketch book path */
	private void initContents() {
		if (initialSketchbookPath == null){
			setPageComplete(false);
			return;
		}
		// see if the sketchbook path has been set in the prefs
//		IPreferenceStore prefs = ProcessingEditorPlugin.getDefault().getPreferenceStore();
//		
//		IPath rootLoc = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		
	}

	/**
	 * Browse button functionality to find a destination folder
	 * <p>
	 * Prettifies the file path if it happens to be in the workspace
	 */
	protected void browseForDestinationFolder() {
		IPath path = browse(getSketchbookLoc());
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
	 * Verifies that the sketchbook path exists and does not already contain a sketch with that name.
 	 */
	private void updatePageComplete() {
		setPageComplete(false);
		
		// check the sketchbook path first
		IPath sketchbookLoc = getSketchbookLoc();
		if (sketchbookLoc == null || !sketchbookLoc.toFile().exists()){
			setMessage(null);
			setErrorMessage("Please specify a sketchbook folder.");
			return;
		}
		
		// ensure the sketch isn't already present in the sketchbook
		String sketchName = getSketchName();
		for( File child : sketchbookLoc.toFile().listFiles()){
			if (child.isDirectory() && child.getName().equals(sketchName)){
				setMessage(null);
				setErrorMessage("A sketch with that name already exists. Please choose another.");
				return;
			}	
		}
		
		// if nothing was caught, enable the finish button 
		setPageComplete(true);
		setMessage(null);
		setErrorMessage(null);
	}

	/**
	 * Sanitizes and returns the sketchNameField contents
	 * <p>
	 * Uses the Processing sanitize method to ensure consistent naming
	 * rules across editors.
	 * 
	 * @return the contents of the sketchNameField as a string or null
	 */
	protected String getSketchName() {
		String text = sketchNameField.getText().trim();
		if (text.length() == 0)
			return null;
		text = Sketch.sanitizeName(text);
		return text;
	}

	/**
	 * Tries to resolve the contents of the sketch book path field to an IPath, and returns it.
	 * If the field contains a relative path it will be resolved relative to the Eclipse workspace folder.
	 * 
	 * @return an absolute IPath handle to the contents of the sketchbookPathField or null
	 */
	protected IPath getSketchbookLoc() {
		String text = sketchbookPathField.getText().trim();
		if (text.length() == 0)
			return null;
		IPath path = new Path(text);
		if (!path.isAbsolute()) // relative paths are relative to the Eclipse workspace
			path = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(path);
		return path;
	}
	
}