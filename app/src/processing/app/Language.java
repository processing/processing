package processing.app;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import processing.core.PApplet;

/**
 * Internationalization (i18n)
 * 
 * @author Darius Morawiec
 */
public class Language {

	private static Language instance = null;
	private String language;
	private HashMap<String, String> languages;
	private ResourceBundle bundle;
	private static final String FILE = "processing.app.languages.PDE";

	private Language() {

		// Get system language
		this.language = Locale.getDefault().getLanguage();

		// Set available languages
		this.languages = new HashMap<String, String>();

		// Language code:
		// http://en.wikipedia.org/wiki/ISO_639-1
		// http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes

		// en, English, English
		this.languages.put(Locale.ENGLISH.getLanguage(), Locale.ENGLISH.getDisplayLanguage(Locale.ENGLISH));
		// de, German, Deutsch
		this.languages.put(Locale.GERMAN.getLanguage(), Locale.GERMAN.getDisplayLanguage(Locale.GERMAN));

		// Set default language
		if (!this.languages.containsKey(this.language)) {
			this.language = "en";
		}

		// Get saved language
		try {
			File file = Base.getContentFile("lib/language.txt");
			if (file.exists()) {
				String language = PApplet.loadStrings(file)[0];
				language = language.trim().toLowerCase();
				if (!language.equals("")) {
					this.language = language;
				} else {
					Base.saveFile(this.language, file);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Get bundle with translations (processing.app.language.PDE)
		this.bundle = ResourceBundle.getBundle(Language.FILE, new Locale(this.language), new UTF8Control());
	}

	/**
	 * Singleton constructor
	 * 
	 * @return
	 */
	public static synchronized Language init() {
		if (instance == null) {
			instance = new Language();
		}
		return instance;
	}

	/**
	 * Get translation from bundles.
	 * 
	 * @param text
	 * @return
	 */
	public static String text(String text) {
		return init().bundle.getString(text);
	}

	/**
	 * Get all available languages
	 * 
	 * @return
	 */
	public static HashMap<String, String> getLanguages() {
		return init().languages;
	}

	/**
	 * Get current language
	 * 
	 * @return
	 */
	public static String getLanguage() {
		return init().language;
	}

	/**
	 * Set new language
	 * 
	 * @param language
	 */
	public static void setLanguage(String language) {
		try {
			File file = Base.getContentFile("lib/language.txt");
			Base.saveFile(language, file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Custom Control class for consitent encoding.
	 * http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle
	 */
	public class UTF8Control extends ResourceBundle.Control {
		public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException,IOException {
			// The below is a copy of the default implementation.
			String bundleName = toBundleName(baseName, locale);
			String resourceName = toResourceName(bundleName, "properties");
			ResourceBundle bundle = null;
			InputStream stream = null;
			if (reload) {
				URL url = loader.getResource(resourceName);
				if (url != null) {
					URLConnection connection = url.openConnection();
					if (connection != null) {
						connection.setUseCaches(false);
						stream = connection.getInputStream();
					}
				}
			} else {
				stream = loader.getResourceAsStream(resourceName);
			}
			if (stream != null) {
				try {
					// Only this line is changed to make it to read properties
					// files as UTF-8.
					bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
				} finally {
					stream.close();
				}
			}
			return bundle;
		}
	}

}