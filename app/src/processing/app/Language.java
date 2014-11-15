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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.*;


/**
 * Internationalization (i18n)
 */
public class Language {  
  /**
   * Store the language information in a file separate from the preferences,
   * because preferences need the language on load time.
   */
  static private final String LANG_FILE = "language.properties";

  /** Directory of available languages */
  static private final String LANG_FILES = "languages";
  
  /** Definition of package for fallback */
  //static private final String LANG_PACKAGE = "processing.app.languages";
  static private final String LANG_BASE_NAME = "PDE";

  static private Properties props;
  static private File propsFile;
  
  /** Language */
  private String language;

  /** Available languages */
  private HashMap<String, String> languages;

  /** Define the location of language files */
  static private File langFiles = Base.getSettingsFile(LANG_FILES);
  
  /** Set version of current PDE */
  static private final String version = Base.getVersionName();
  static private ResourceBundle bundle;
  
  /** Single instance of this Language class */
  static private volatile Language instance;

  
  private Language() {
    // Set default language
    language = "en";
    
    // Set available languages
    languages = new HashMap<String, String>();
    for (String code : listSupported()) {
      languages.put(code, Locale.forLanguageTag(code).getDisplayLanguage(Locale.forLanguageTag(code)));
    }
    
    if(loadProps()){

      boolean updateProps = false;
      boolean updateLangFiles = false;
      
      // Define and check version
      if(props.containsKey("version") && langFiles.exists()){
        if(!props.getProperty("version").equals(version)){
          updateLangFiles = true;
        }
      } else {
        updateLangFiles = true;
      }
      
      // Copy new language properties
      if(updateLangFiles){
        if(updateLangFiles()){
          props.setProperty("version", version);
          updateProps = true;
        }
      }
      
      // Define language
      if(props.containsKey("language")){
        language = props.getProperty("language");
      } else {
        if (!languages.containsKey(Locale.getDefault().getLanguage())) {
          language = Locale.getDefault().getLanguage();
        }
        props.setProperty("language", language);
        updateProps = true;
      }
      
      // Save changes
      if(updateProps){
        updateProps();
      }
    }
    
    try {
      URL[] paths = { langFiles.toURI().toURL() };
      bundle = ResourceBundle.getBundle(
        Language.LANG_BASE_NAME,
        new Locale(language),
        new URLClassLoader(paths),
        new UTF8Control()
      );
    } catch (MalformedURLException e) {
      // Fallback: Does we need a fallback?
//      bundle = ResourceBundle.getBundle(
//        Language.LANG_PACKAGE+"."+Language.LANG_BASE_NAME,
//        new Locale(this.language),
//        new UTF8Control()
//      );
    }
  }
  
  
  private static boolean updateLangFiles() {
    // Delete old languages
    if(langFiles.exists()){
      File[] lang = langFiles.listFiles();
      for (int i = 0; i < lang.length; i++) {
        lang[i].delete();
      }
      langFiles.delete();
    }
    // Copy new languages
    String javaPath = new File(Base.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile().getAbsolutePath();
    try {
      Base.copyDir(
        new File(javaPath+"/lib/languages"),  // from shared library folder
        langFiles                             // to editable settings folder
      );
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
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
    Arrays.sort(SUPPORTED);
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

  
  /** Load properties of language.properties */
  static private boolean loadProps(){
    if (props == null) {
      props = new Properties();
    }
    propsFile = Base.getSettingsFile(LANG_FILE);
    if(!propsFile.exists()){
      try {
        Base.saveFile("", propsFile);
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      }
    }
    InputStream is = null;
    try {
      is = new FileInputStream(propsFile);
    } catch (Exception e) {
      return false;
    }
    try {
      props.load(is);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    try {
      is.close();
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
  
  
  /** Save changes in language.properties */
  static private boolean updateProps(){
    if (props != null) { 
      try {
        OutputStream out = new FileOutputStream(propsFile);
        props.store(out, "language preferences");
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return false;
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      }
    }
    return true;
  }
  
  
  /**
   * Save the language directly to a settings file. This is 'save' and not 
   * 'set' because a language change requires a restart of Processing. 
   */
  static public void saveLanguage(String language) {
    if (loadProps()) {
      props.setProperty("language", language);
      if (updateProps()) {
        Base.getPlatform().saveLanguage(language);
      }
    }
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

  
  /** Get translation from bundles. */
  static public String text(String text) {
    init();
    try {
      return bundle.getString(text);
    } catch (MissingResourceException e) {
      return text;
    }
  }
  
  
  static public String interpolate(String text, Object... arguments) {
    init();
    return String.format(bundle.getString(text), arguments);
  }

  
  static public String pluralize(String text, int count) {
    init();
    String fmt = text + ".%s";
    if (bundle.containsKey(String.format(fmt, count))) {
      return interpolate(String.format(fmt, count), count);
    }
    return interpolate(String.format(fmt, "n"), count);
  }

  
  /** Get all available languages */
  static public Map<String, String> getLanguages() {
    return init().languages;
  }

  
  /** Get current language */
  static public String getLanguage() {
    return init().language;
  }


  /**
   * Custom 'Control' class for consistent encoding.
   * http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle
   */
  static private class UTF8Control extends ResourceBundle.Control {
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
