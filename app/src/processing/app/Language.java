/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2014 The Processing Foundation

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import java.io.*;
import java.util.*;

import processing.core.PApplet;


/**
 * Internationalization (i18n)
 */
public class Language {
  // Store the language information in a file separate from the preferences,
  // because preferences need the language on load time.
  static protected final String PREF_FILE = "language.txt";
  static protected final File prefFile = Base.getSettingsFile(PREF_FILE);

  /** Single instance of this Language class */
  static private volatile Language instance;

  /** The system language */
  private String language;

  /** Available languages */
  private HashMap<String, String> languages;

  private LanguageBundle bundle;


  private Language() {
    String systemLanguage = Locale.getDefault().getLanguage();
    language = loadLanguage();
    boolean writePrefs = false;

    if (language == null) {
      language = systemLanguage;
      writePrefs = true;
    }

    // Set available languages
    languages = new HashMap<String, String>();
    for (String code : listSupported()) {
      languages.put(code, Locale.forLanguageTag(code).getDisplayLanguage(Locale.forLanguageTag(code)));
    }

    // Set default language
    if (!languages.containsKey(language)) {
      language = "en";
      writePrefs = true;
    }

    if (writePrefs) {
      saveLanguage(language);
    }

    // Get bundle with translations (processing.app.language.PDE)
    //bundle = ResourceBundle.getBundle(Language.FILE, new Locale(this.language), new UTF8Control());
    try {
      bundle = new LanguageBundle(language);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  static private String[] listSupported() {
    // List of languages in alphabetical order. (Add yours here.)
    // Also remember to add it to the corresponding build/build.xml rule.
    final String[] SUPPORTED = {
      "de", // German, Deutsch
      "en", // English
      "el", // Greek
      "es", // Spanish
      "fr", // French, Français
      "ja", // Japanese
      "ko", // Korean
      "nl", // Dutch, Nederlands
      "pt", // Portuguese
      "tr", // Turkish
      "zh"  // Chinese
    };
    return SUPPORTED;

    /*
    // come back to this when bundles are placed outside the JAR
    InputStream input = getClass().getResourceAsStream(LISTING);
    String[] lines = PApplet.loadStrings(input);
    ArrayList<String> list = new ArrayList<String>();
    for (String line : lines) {
      int index = line.indexOf('#');
      if (index != -1) {
        line = line.substring(0, index);
      }
      line = line.trim();
      list.add(line);
    }
    return list.toArray(new String[0]);
    */
  }


  /** Read the saved language */
  static private String loadLanguage() {
    try {
      if (prefFile.exists()) {
        String language = PApplet.loadStrings(prefFile)[0];
        language = language.trim().toLowerCase();
        if (language.trim().length() != 0) {
          return language;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }


  /**
   * Save the language directly to a settings file. This is 'save' and not
   * 'set' because a language change requires a restart of Processing.
   */
  static public void saveLanguage(String language) {
    try {
      Base.saveFile(language, prefFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
    Base.getPlatform().saveLanguage(language);
  }


  /** Singleton constructor */
  static public Language init() {
    if (instance == null) {
      synchronized (Language.class) {
        if (instance == null) {
          instance = new Language();
        }
      }
    }
    return instance;
  }


  static private String get(String key) {
    LanguageBundle bundle = init().bundle;

    try {
      String value = bundle.getString(key);
      if (value != null) {
        return value;
      }
    } catch (MissingResourceException e) { }

    return null;
  }


  /** Get translation from bundles. */
  static public String text(String key) {
    String value = get(key);
    if (value == null) {
      // MissingResourceException and null values
      return key;
    }
    return value;
  }


  static public String interpolate(String key, Object... arguments) {
    String value = get(key);
    if (value == null) {
      return key;
    }
    return String.format(value, arguments);
  }


  static public String pluralize(String key, int count) {
    // First check if the bundle contains an entry for this specific count
    String customKey = key + "." + count;
    String value = get(customKey);
    if (value != null) {
      return String.format(value, count);
    }
    // Use the general 'n' version for n items
    return interpolate(key + ".n", count);
  }


  /** Get all available languages */
  static public Map<String, String> getLanguages() {
    return init().languages;
  }


  /**
   * Get the current language.
   * @return two digit ISO code (lowercase)
   */
  static public String getLanguage() {
    return init().language;
  }


//  /** Set new language (called by Preferences) */
//  static public void setLanguage(String language) {
//    this.language = language;
//
//    try {
//      File file = Base.getContentFile("lib/language.txt");
//      Base.saveFile(language, file);
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }


  /**
   * Custom 'Control' class for consistent encoding.
   * http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle
   */
  /*
  static class UTF8Control extends ResourceBundle.Control {
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
          // Only line changed from the original source:
          bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
        } finally {
          stream.close();
        }
      }
      return bundle;
    }
  }
  */


  static class LanguageBundle {
    Map<String, String> table;

    LanguageBundle(String language) throws IOException {
      table = new HashMap<String, String>();

      String baseFilename = "languages/PDE.properties";
      String langFilename = "languages/PDE_" + language + ".properties";

      File baseFile = Base.getLibFile(baseFilename);
      File userBaseFile = new File(Base.getSketchbookFolder(), baseFilename);
      if (userBaseFile.exists()) {
        baseFile = userBaseFile;
      }

      File langFile = Base.getLibFile(langFilename);
      File userLangFile = new File(Base.getSketchbookFolder(), langFilename);
      if (userLangFile.exists()) {
        langFile = userLangFile;
      }

      read(baseFile);
      read(langFile);
    }

    void read(File additions) {
      String[] lines = PApplet.loadStrings(additions);
      //for (String line : lines) {
      for (int i = 0; i < lines.length; i++) {
        String line = lines[i];
        if ((line.length() == 0) ||
            (line.charAt(0) == '#')) continue;

        // this won't properly handle = signs inside in the text
        int equals = line.indexOf('=');
        if (equals != -1) {
          String key = line.substring(0, equals).trim();
          String value = line.substring(equals + 1).trim();

          /*
          // Support for backslashes to continue lines... Nah.
          while (line.endsWith("\\")) {
            // remove the backslash from the previous
            value = value.substring(0, value.length() - 1);
            // get the next line
            line = lines[++i].trim();
            // append the new line to the value (with a space)
            // This is imperfect since the prev may end <br>
            value += " " + line;
          }
          */

          // fix \n and \'
          value = value.replaceAll("\\\\n", "\n");
          value = value.replaceAll("\\\\'", "'");

          table.put(key, value);
        }
      }
    }

    String getString(String key) {
      return table.get(key);
    }

    boolean containsKey(String key) {
      return table.containsKey(key);
    }
  }
}
