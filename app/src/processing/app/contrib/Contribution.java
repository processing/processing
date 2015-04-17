/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013 The Processing Foundation
  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along 
  with this program; if not, write to the Free Software Foundation, Inc.
  59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package processing.app.contrib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import processing.core.PApplet;
import processing.app.Language;

abstract public class Contribution {
  static final String SPECIAL_CATEGORY_NAME = "Starred";
  static final List validCategories = 
    Arrays.asList("3D", "Animation", "Data", "Geometry", "GUI", "Hardware", 
                  "I/O", "Math", "Simulation", "Sound", SPECIAL_CATEGORY_NAME, "Typography", 
                  "Utilities", "Video & Vision", "Other");

  //protected String category;      // "Sound"
  protected List<String> categories;  // "Sound", "Typography"
  protected String name;          // "pdf" or "PDF Export"
  protected String authorList;    // Ben Fry
  protected String url;           // http://processing.org
  protected String sentence;      // Write graphics to PDF files.
  protected String paragraph;     // <paragraph length description for site>
  protected int version;          // 102
  protected String prettyVersion; // "1.0.2"
  protected long lastUpdated;   //  1402805757
  protected int minRevision;    //  0
  protected int maxRevision;    //  227
  protected List<String> specifiedImports; // pdf.export.*,pdf.convert.common.*
  
  
  // "Sound"
//  public String getCategory() {
//    return category;
//  }

  
  // "Sound", "Utilities"... see valid list in ContributionListing
  protected List<String> getCategories() {
    return categories;
  }
  
  
  protected String getCategoryStr() {
    StringBuilder sb = new StringBuilder();
    for (String category : categories) {
      sb.append(category);
      sb.append(',');
    }
    sb.deleteCharAt(sb.length()-1);  // delete last comma
    return sb.toString();
  }
  
  
  protected boolean hasCategory(String category) {
    if (category != null) {
      for (String c : categories) {
        if (category.equalsIgnoreCase(c)) {
          return true;
        }
      }
    }
    return false;
  }  
  
  
  // pdf.export.*,pdf.convert.common.*
  protected List<String> getImports() {
    return specifiedImports;
  }
  
  
  protected String getImportStr() {
    if (specifiedImports == null || specifiedImports.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (String importName : specifiedImports) {
      sb.append(importName);
      sb.append(',');
    }
    sb.deleteCharAt(sb.length() - 1); // delete last comma
    return sb.toString();
  }

  protected boolean hasImport(String importName) {
    if (specifiedImports != null && importName != null) {
      for (String c : specifiedImports) {
        if (importName.equalsIgnoreCase(c)) {
          return true;
        }
      }
    }
    return false;
  }


  // "pdf" or "PDF Export"
  public String getName() {
    return name;
  }


  // "[Ben Fry](http://benfry.com/)"
  public String getAuthorList() {
    return authorList;
  }


  // "http://processing.org"
  public String getUrl() {
    return url;
  }


  // "Write graphics to PDF files."
  public String getSentence() {
    return sentence;
  }


  // <paragraph length description for site>
  public String getParagraph() {
    return paragraph;
  }


  // 102
  public int getVersion() {
    return version;
  }


  // "1.0.2"
  public String getPrettyVersion() {
    return prettyVersion;
  }
  
  // 1402805757
  public long getLastUpdated() {
    return lastUpdated;
  }

  // 0
  public int getMinRevision() {
    return minRevision;
  }

  // 227
  public int getMaxRevision() {
    return maxRevision;
  }


  public boolean isCompatible(int versionNum) {
    return ((maxRevision == 0 || versionNum < maxRevision) && versionNum > minRevision);
  }


  abstract public ContributionType getType();
  
  
  public String getTypeName() {
    return getType().toString();
  }
  
  
  abstract public boolean isInstalled();
  
  
//  /** 
//   * Returns true if the type of contribution requires the PDE to restart
//   * when being added or removed. 
//   */
//  public boolean requiresRestart() {
//    return getType() == ContributionType.TOOL || getType() == ContributionType.MODE;
//  }
  

  boolean isRestartFlagged() {
    return false;
  }
  
  
  /** Overridden by LocalContribution. */
  boolean isDeletionFlagged() {
    return false;
  }

  
  boolean isUpdateFlagged() {
    return false;
  }


  /**
   * Returns true if the contribution is a starred/recommended contribution, or
   * is by the Processing Foundation.
   * 
   * @return
   */
  boolean isSpecial() {
    try {
      return (authorList.indexOf("The Processing Foundation") != -1 || 
              categories.contains(SPECIAL_CATEGORY_NAME));
    } catch (NullPointerException npe) {
      return false;
    }
  }


  /** 
   * @return a single element list with "Unknown" as the category.
   */
  static List<String> defaultCategory() {
    List<String> outgoing = new ArrayList<String>();
    outgoing.add("Unknown");
    return outgoing;
  }
  
  
  /**
   * @return the list of categories that this contribution is part of
   *         (e.g. "Typography / Geometry"). "Unknown" if the category null.
   */
  static List<String> parseCategories(String categoryStr) {
    List<String> outgoing = new ArrayList<String>();
    
    if (categoryStr != null) {
      String[] listing = PApplet.trim(PApplet.split(categoryStr, ','));
      for (String category : listing) {
        if (validCategories.contains(category)) {
          category = translateCategory(category);
          outgoing.add(category);
        }
      }
    }
    if (outgoing.size() == 0) {
      return defaultCategory();
    }
    return outgoing;
  }
  
  
  /**
   * @return the list of imports that this contribution (library) contains.
   */
  static List<String> parseImports(String importStr) {
    List<String> outgoing = new ArrayList<String>();

    if (importStr != null) {
      String[] importList = PApplet.trim(PApplet.split(importStr, ','));
      for (String importName : importList) {
        outgoing.add(importName);
      }
    }
    return (outgoing.size() > 0) ? outgoing : null;
  }

  static private String translateCategory(String cat) {
    String translated = "";
  
    switch (cat) {
    case "3D":
      translated = Language.text("contrib.category.3d");
      break;
    case "Animation":
      translated = Language.text("contrib.category.animation");
      break;
    case "Data":
      translated = Language.text("contrib.category.data");
      break;
    case "Geometry":
      translated = Language.text("contrib.category.geometry");
      break;
    case "GUI":
      translated = Language.text("contrib.category.gui");
      break;
    case "Hardware":
      translated = Language.text("contrib.category.hardware");
      break;
    case "I/O":
      translated = Language.text("contrib.category.io");
      break;
    case "Math":
      translated = Language.text("contrib.category.math");
      break;
    case "Simulation":
      translated = Language.text("contrib.category.simulation");
      break;
    case "Sound":
      translated = Language.text("contrib.category.sound");
      break;
    case "Typography":
      translated = Language.text("contrib.category.typography");
      break;
    case "Utilities":
      translated = Language.text("contrib.category.utilities");
      break;
    case "Video & Vision":
      translated = Language.text("contrib.category.video_vision");
      break;
    case "Other":
      translated = Language.text("contrib.category.other");
      break;
    }
    return translated;
  }
  
}
