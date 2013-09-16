package processing.app;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import processing.core.PApplet;


/**
 * Internationalization (i18n)
 * @author Darius Morawiec
 */
public class Language {
  
  private static Language instance = null;
  private String language;
  private HashMap<String,String> languages;
  private ResourceBundle bundle;
  
  private Language() {
    
    // Get system language
    this.language = Locale.getDefault().getLanguage();
    
    // Set available languages
    this.languages = new HashMap<String,String>();
    this.languages.put("en", "English");
    this.languages.put("de", "Deutsch");
    
    // Set default language
    if(!this.languages.containsKey(this.language)){
      this.language = "en";
    }
    
    // Get saved language
    try {
      File file = Base.getContentFile("lib/language.txt");
      if (file.exists()) {
        String language = PApplet.loadStrings(file)[0];
        language = language.trim().toLowerCase();
        if(!language.equals("")) {
          this.language = language;
        } else {
          Base.saveFile(this.language, file);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    // Get bundle with translations (processing.app.language.PDE)
    this.bundle = ResourceBundle.getBundle("processing.app.language.PDE", new Locale(this.language));
  }
  
  /**
   * Singleton constructor
   * @return
   */
  public static synchronized Language init() {
    if(instance == null) {
      instance = new Language();
    }
    return instance;
  }
  
  /**
   * Get translation from bundles.
   * @param text
   * @return
   */
  public static String text(String text) {
    return init().bundle.getString(text);
  }
  
  /**
   * Get all available languages
   * @return
   */
  public static HashMap<String, String> getLanguages() {
    return init().languages;
  }
  
  /**
   * Get current language
   * @return
   */
  public static String getLanguage() {
    return init().language;    
  }
  
  /**
   * Set new language
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
  
}