package org.processing.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.processing.editor.ProcessingEditorPlugin;

/**
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 * <p>
 * Two editor preferences are included as examples, but
 * currently do not do anything.
 * <p>
 * //TODO transfer Processing.app preferences here
 * this may involve subclassing PreferencePage,
 * but things could be setup such that these are read
 * from a file, and we could use a customized version of
 * a standard Processing preferences file to store this.
 */

public class ProcessingPreferencesPage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public ProcessingPreferencesPage() {
		super(GRID);
		setPreferenceStore(ProcessingEditorPlugin.getDefault().getPreferenceStore());
		setDescription("Processing Plugin Preferences (nothing here is used, see JavaDoc)");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		addField(new DirectoryFieldEditor(PreferenceConstants.PROCESSING_SKETCH, 
				"Sketchbook Directory:", getFieldEditorParent()));
		addField(
			new BooleanFieldEditor(
				PreferenceConstants.PROCESSING_AUTO_INDENT,
				"Auto indent",
				getFieldEditorParent()));
	}

	/**
	 * Initializes the preference page. Nothing to do here, but it has to be subclassed.	  
	 */
	public void init(IWorkbench workbench) {}
	
}