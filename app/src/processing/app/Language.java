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
  static private Language instance = null;
  private String language;
  private HashMap<String, String> languages;
  private ResourceBundle bundle;
  static private final String FILE = "processing.app.languages.PDE";


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
    // ja, Japanese
    this.languages.put(Locale.JAPANESE.getLanguage(), Locale.JAPANESE.getDisplayLanguage(Locale.JAPANESE));
    // es, Spanish
    this.languages.put("es", Locale.forLanguageTag("es").getDisplayLanguage(Locale.forLanguageTag("es")));
    // nl, Dutch, Nederlands
    this.languages.put("nl", Locale.forLanguageTag("nl").getDisplayLanguage(Locale.forLanguageTag("nl")));

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


  /** Set new language */
  static public void setLanguage(String language) {
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

