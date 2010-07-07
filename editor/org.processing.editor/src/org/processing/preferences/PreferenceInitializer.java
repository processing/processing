package org.processing.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import org.processing.editor.ProcessingEditorPlugin;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/**
	 *  Sets the default preference values
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = ProcessingEditorPlugin.getDefault().getPreferenceStore();
		
		//store.setDefault(PreferenceContants.PROCESSING_SKETCH, value);
		store.setDefault(PreferenceConstants.PROCESSING_AUTO_INDENT, true);

		// PROCESSING_PREFERENCES are processing.app preferences
//		store.setDefault(PreferenceConstants.PROCESSING_APP_PREFERENCES_EDITOR_TABS_SIZE, "4");
//		store.setDefault(PreferenceConstants.PROCESSING_APP_PREFERENCES_PREPROC_SUBSTITUTE_FLOATS, "true");
//		store.setDefault(PreferenceConstants.PROCESSING_APP_PREFERENCES_PREPROC_WEB_COLORS, "true");
//		store.setDefault(PreferenceConstants.PROCESSING_APP_PREFERENCES_PREPROC_COLOR_DATATYPE, "true");
//		store.setDefault(PreferenceConstants.PROCESSING_APP_PREFERENCES_PREPROC_ENHANCED_CASTING, "true");
//		store.setDefault(PreferenceConstants.PROCESSING_APP_PREFERENCES_PREPROC_SUBSTITUTE_UNICODE, "true");
//		store.setDefault(PreferenceConstants.PROCESSING_APP_PREFERENCES_PREPROC_OUTPUT_PARSE_TREE, "false");
//		store.setDefault(PreferenceConstants.PROCESSING_APP_PREFERENCES_PREPROC_EXPORT_APPLICATION_FULLSCREEN, "false");
//		store.setDefault(PreferenceConstants.PROCESSING_APP_PREFERENCES_PREPROC_RUN_PRESENT_BGCOLOR, "#666666");
//		store.setDefault(PreferenceConstants.PROCESSING_APP_PREFERENCES_PREPROC_EXPORT_APPLICATION_STOP, "true");
//		store.setDefault(PreferenceConstants.PROCESSING_APP_PREFERENCES_PREPROC_RUN_PRESENT_STOP_COLOR, "#cccccc");
//		store.setDefault(PreferenceConstants.PROCESSING_APP_PREFERENCES_PREPROC_RUN_WINDOW_BGCOLOR, "#ECE9D8");
//		store.setDefault(PreferenceConstants.PROCESSING_APP_PREFERENCES_PREPROC_PREPROC_IMPORTS_LIST, "java.applet.*,java.awt.Dimension,java.awt.Frame,java.awt.event.MouseEvent,java.awt.event.KeyEvent,java.awt.event.FocusEvent,java.awt.Image,java.io.*,java.net.*,java.text.*,java.util.*,java.util.zip.*,java.util.regex.*");
	}

}
