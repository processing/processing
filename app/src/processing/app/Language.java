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
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import processing.core.PApplet;


/**
 * Internationalization (i18n)
 * @author Darius Morawiec
 */
public class Language {
  static private final String FILE = "processing.app.languages.PDE";
  //static private final String LISTING = "processing/app/languages/languages.txt";
  
  // Store the language information in a file separate from the preferences,
  // because preferences need the language on load time.
  static protected final String PREF_FILE = "language.txt";
  static protected final File prefFile = Base.getSettingsFile(PREF_FILE);
  
  /** Single instance of this Language class */
  static private Language instance = null;
  
  /** The system language */
  private String language;
  
  /** Available languages */
  private HashMap<String, String> languages;
  
  private ResourceBundle bundle;


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
    bundle = ResourceBundle.getBundle(Language.FILE, new Locale(this.language), new UTF8Control());
  }
  
  
  static private String[] listSupported() {
    // List of languages in alphabetical order. 
    final String[] SUPPORTED = {
      "de", // de, German, Deutsch
      "en", // en, English, English
      "el", // el, Greek
      "es", // es, Spanish
      "fr", // fr, French, Français, Langue française
      "ja", // ja, Japanese
      "nl", // nl, Dutch, Nederlands
      "pt", // pt, Portuguese
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
        if (!language.equals("")) {
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
  }
  
  
  /** Singleton constructor */
  static public synchronized Language init() {
    if (instance == null) {
      instance = new Language();
    }
    return instance;
  }


  /** Get translation from bundles. */
  static public String text(String text) {
    return init().bundle.getString(text);
  }


  /** Get all available languages */
  static public Map<String, String> getLanguages() {
    return init().languages;
  }


  /** Get current language */
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
  class UTF8Control extends ResourceBundle.Control {
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
}