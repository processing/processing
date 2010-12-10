package processing.app;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.ImageIcon;

import processing.app.syntax.PdeKeywords;
import processing.app.syntax.SyntaxStyle;


public abstract class Mode {
  protected Base base;
  
//  protected String name;

  protected File folder;

  protected HashMap<String, String> keywordToReference;
  
  protected PdeKeywords tokenMarker;
  protected Settings theme;

  
  public Mode(Base base, File folder) {
    this.base = base;
    this.folder = folder;
  }
  

  /** 
   * Return the pretty/printable/menu name for this mode. This is separate from
   * the single word name of the folder that contains this mode. It could even
   * have spaces, though that might result in sheer madness or total mayhem.   
   */
  abstract public String getTitle();

//  public String getName() {
//    return name;
//  }

  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  abstract public EditorToolbar createToolbar(Editor editor);
  
  
  abstract public void internalCloseRunner(Editor editor);  

  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


/**
   * Get an image object from the theme folder.
   */
  public Image loadImage(String filename) {
    File file = new File(folder, filename);
    return new ImageIcon(file.getAbsolutePath()).getImage();
  }
  
  
  //public Settings getTheme() {
  //  return theme;
  //}


  public String getReference(String keyword) {
    return keywordToReference.get(keyword);
  }


  //public TokenMarker getTokenMarker() throws IOException {
  //  File keywordsFile = new File(folder, "keywords.txt");
  //  return new PdeKeywords(keywordsFile);
  //}


  //public String get(String attribute) {
  //  return theme.get(attribute);
  //}


  public boolean getBoolean(String attribute) {
    return theme.getBoolean(attribute);
  }


  public int getInteger(String attribute) {
    return theme.getInteger(attribute);
  }


  public Color getColor(String attribute) {
    return theme.getColor(attribute);
  }


  public Font getFont(String attribute) {
    return theme.getFont(attribute);
  }


  public SyntaxStyle getStyle(String attribute) {
    return theme.getStyle(attribute);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  
  
  public void handleNew() {
    base.handleNew();    
  }


  public void handleNewReplace() {
    base.handleNewReplace();
  }
}