package org.processing.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * A wizard to create a new Processing sketch project.
 * <p>
 * Contains a single page the requests the name of the sketch
 * and the sketch book folder to put the sketch in.
 * 
 * @author lonnen
 */
public class NewSketchWizard extends Wizard implements INewWizard {
	
	/** The single page in the wizard */
	private NewSketchWizardPage page;

	/** Constructor */
	public NewSketchWizard(){
		
		
	}
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// init code
	}
	
	/**
	 * Called when the 'Finish' button is pressed in the wizard.
	 */
	public boolean performFinish() {
		// shit it down
		return true;
	}
	
	/**
	 * Initializes the single page and adds it to the wizard.
	 */
	public void addPages(){
		setWindowTitle("New Sketch Wizard");
		page = new NewSketchWizardPage();

		addPage(page);
	}
	
}
