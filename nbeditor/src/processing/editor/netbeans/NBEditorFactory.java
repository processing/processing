package processing.editor.netbeans;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.KeyStroke;

import org.netbeans.editor.BaseSettingsInitializer;
import org.netbeans.editor.Settings;
import org.netbeans.editor.ext.ExtKit;
import org.netbeans.editor.ext.ExtSettingsInitializer;
import org.netbeans.editor.ext.ExtUtilities;
import org.openide.util.Utilities;

/**
 * NBEditorFactory is
 * @author $Author: antonio $ Antonio Vieiro (antonio@antonioshome.net)
 * @version $Revision: 1.2 $
 */
public class NBEditorFactory
{
	private NBEditorFactory()
	{
	}

	/**
	 * Flag indicating if default settings have been initialized.
	 */
	private static boolean initialized = false;

	/**
	 * Initializes default settings.
	 */
	private static synchronized void initEditorSettings()
	{
		if (!initialized)
		{
			Settings.addInitializer(new BaseSettingsInitializer(), Settings.CORE_LEVEL);
			Settings.addInitializer(new ExtSettingsInitializer(), Settings.CORE_LEVEL);
			Settings.reset();
			initialized = true;
		}
	}

	/**
	 * Registers a given content type (from the ExtKit) and installs 
	 *  appropriate settings for it.
	 * @param anEditorKit an ExtKit to use.
	 * @param aSettingsInitializer an object that initializes editor settings
	 *  for that ExtKit type.
	 */
	public static void addSyntax(final ExtKit anEditorKit,
			final Settings.AbstractInitializer aSettingsInitializer)
	{
		// Initialize default settings in case of need
		initEditorSettings();

		// Register content-type
		JEditorPane.registerEditorKitForContentType(anEditorKit.getContentType(),
				anEditorKit.getClass().getName());

		// Add the initializer to the hierarchy of initializers
		Settings.addInitializer(aSettingsInitializer);
		Settings.reset();
	}

	/**
	 * Updates key bindings for a JEditorPane
	 * @param anEditorPane the JEditorPane on which the key bindings are to
	 *  be installed.
	 */
	private static void updateKeyBindings(final JEditorPane anEditorPane)
	{
		// Update key bindings for the editor pane
		Properties keybindings = new Properties();
		InputMap inputMap = anEditorPane.getInputMap();
		try
		{
			InputStream inputStream = NBEditorFactory.class
					.getResourceAsStream("keybindings.properties");
			keybindings.load(inputStream);
			inputStream.close();

			Enumeration keynames = keybindings.keys();
			while (keynames.hasMoreElements())
			{
				String keyName = (String) keynames.nextElement();
				KeyStroke ks = Utilities.stringToKey(keyName);
				String actionName = keybindings.getProperty(keyName);
				inputMap.put(ks, actionName);
			}
		}
		catch (Exception e)
		{
			// If there's any problem loading keybindings then log it ...
			e.printStackTrace(System.err);
			// ... and use a minimum of working bindings
			// TODO: Update this list of default keys
			inputMap.put(KeyStroke.getKeyStroke("DELETE"), ExtKit.deleteNextCharAction);
			inputMap.put(KeyStroke.getKeyStroke("BACK_SPACE"),
					ExtKit.deletePrevCharAction);
			inputMap.put(KeyStroke.getKeyStroke("ENTER"), ExtKit.insertBreakAction);
			inputMap.put(KeyStroke.getKeyStroke("UP"), ExtKit.upAction);
			inputMap.put(KeyStroke.getKeyStroke("DOWN"), ExtKit.downAction);
			inputMap.put(KeyStroke.getKeyStroke("LEFT"), ExtKit.backwardAction);
			inputMap.put(KeyStroke.getKeyStroke("RIGHT"), ExtKit.forwardAction);
			inputMap.put(KeyStroke.getKeyStroke("ctrl Z"), ExtKit.undoAction);
			inputMap.put(KeyStroke.getKeyStroke("ctrl Y"), ExtKit.redoAction);
		}
	}

	/**
	 * Obtains a JComponent responsible for visualization of the contents
	 *  of a JEditorPane, and prepares the JEditorPane to accept the content-type
	 *  of the given EditorKit.
	 * @param anEditorKit an ExtKit with an appropriate content-type.
	 * @param anEditorPane a JEditorPane whose UI will be replaced with the
	 *  JComponent. This JEditorPane can be used to modify the text, but not
	 *  for visualization.
	 * @return a JComponent responsible for visualizing text, rendering a status
	 *  bar and a line number bar.
	 */
	public static JComponent newTextRenderer(final ExtKit anEditorKit,
			final JEditorPane anEditorPane)
	{
		initEditorSettings();
		updateKeyBindings(anEditorPane);
		anEditorPane.setContentType(anEditorKit.getContentType());
		return ExtUtilities.getExtEditorUI(anEditorPane).getExtComponent();
	}

}
